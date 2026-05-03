# DSL Reference

A pipeline file is a YAML document with three top-level keys.

```yaml
name: my-pipeline          # required, 5–30 characters
variables: []              # optional list of named values
actions: []                # ordered list of sources, transformers, sinks
```

---

## Variables

Variables are resolved once before the pipeline runs. Use `{{variableName}}` anywhere an option value or expression supports substitution.

### `string`

A static string value.

```yaml
variables:
  - name: targetTable
    type: string
    value: "my_schema.my_table"
```

| Field   | Required | Description |
|---------|----------|-------------|
| `name`  | yes | Variable name, referenced as `{{name}}` |
| `type`  | yes | `string` |
| `value` | yes | The literal string value |

### `expression`

A Spark SQL expression evaluated at runtime against an empty DataFrame row.

```yaml
variables:
  - name: reportDate
    type: expression
    expression: "current_date()"
    defaultValue: ""       # optional, default ""
```

| Field          | Required | Description |
|----------------|----------|-------------|
| `name`         | yes | Variable name |
| `type`         | yes | `expression` |
| `expression`   | yes | Any Spark SQL expression (`current_date()`, `date_sub(current_date(), 7)`, etc.) |
| `defaultValue` | no  | Fallback if the expression evaluates to null. Default: `""` |

---

## Variable Substitution Scope

Variable substitution uses `{{variableName}}` (double curly braces). It is **not** applied everywhere — only in specific fields of specific action types.

| Location | Supported | Notes |
|----------|-----------|-------|
| `general.filter` | yes | Full SQL expression string |
| `general.select` list entries | yes | Each expression in the list |
| `csv` sink `path` | yes | Output directory path |
| `jdbc` source `queryFile` content | yes | Entire SQL file contents |
| `jdbc` source `options` map | **no** | Options are passed to Spark as-is |
| `jdbc` sink `options` map | **no** | Options are passed to Spark as-is |
| `generic` source `options` map | **no** | Options are passed to Spark as-is |
| `join.on` expression | **no** | Not processed by `Formatter` |
| `group.aggregations` list | **no** | Not processed by `Formatter` |
| `pivot.aggregations` list | **no** | Not processed by `Formatter` |

**Practical rule:** substitution works in `general` transformer expressions and `csv` sink paths. For JDBC sources, put parameterised SQL in a `.sql` file and reference it with `queryFile`.

```yaml
# Correct: variable in a queryFile (substitution applied)
- name: load-data
  kind: source
  type: jdbc
  options:
    url: jdbc:postgresql://host:5432/db
    user: myuser
    password: mypassword
    driver: org.postgresql.Driver
  queryFile: sql/my-query.sql    # {{startDate}} inside the file will be resolved

# Correct: variable in a general transformer select
- name: annotate
  kind: transformer
  type: general
  input: load-data
  select:
    - id
    - value
    - "cast('{{reportDate}}' as date) as report_date"

# Wrong: variables in jdbc options are NOT resolved
- name: load-data-wrong
  kind: source
  type: jdbc
  options:
    dbtable: "{{targetTable}}"    # will be passed literally as "{{targetTable}}"
```

---

## Actions

Every action shares these top-level fields:

| Field  | Required | Description |
|--------|----------|-------------|
| `name` | yes | Unique identifier — used to reference this action's output DataFrame in later actions |
| `kind` | yes | `source`, `transformer`, or `sink` |
| `type` | yes | Subtype within the kind (see below) |

---

## Sources (`kind: source`)

### `type: jdbc`

Reads a DataFrame from any JDBC-compatible database.

```yaml
- name: load-orders
  kind: source
  type: jdbc
  options:
    url: jdbc:postgresql://host:5432/db
    user: myuser
    password: mypassword
    driver: org.postgresql.Driver
    dbtable: orders
  queryFile: sql/custom-query.sql    # optional
```

| Field       | Required | Description |
|-------------|----------|-------------|
| `options`   | yes | Map of JDBC options passed directly to Spark's JDBC reader. Common keys: `url`, `user`, `password`, `driver`, `dbtable`. Any Spark JDBC option is accepted. |
| `queryFile` | no  | Path to a `.sql` file. Its content is read, variable substitution (`{{name}}`) is applied, and the result is passed as the `query` option, overriding `dbtable`. |

### `type: generic`

Reads a DataFrame from any Spark-supported file format.

```yaml
- name: load-csv
  kind: source
  type: generic
  format: csv
  options:
    path: /data/input/file.csv
    header: "true"
    inferSchema: "true"
```

| Field     | Required | Description |
|-----------|----------|-------------|
| `format`  | yes | Spark format string: `csv`, `parquet`, `json`, `orc`, `avro`, etc. |
| `options` | yes | Map of format-specific read options passed to Spark's `DataFrameReader`. |

---

## Transformers (`kind: transformer`)

### `type: general`

Applies filter, column selection, ordering, and deduplication.

```yaml
- name: filter-active
  kind: transformer
  type: general
  input: load-orders
  filter: "status = 'ACTIVE'"
  select:
    - id
    - "upper(name) as name"
    - "{{reportDate}} as report_date"
  order:
    - created_at
  distinct: false
```

| Field      | Required | Description |
|------------|----------|-------------|
| `input`    | yes | Name of a previous action whose DataFrame to transform |
| `filter`   | no  | SQL WHERE expression (e.g. `amount > 100`) |
| `select`   | no  | List of SQL expressions passed to `selectExpr`. Variable substitution (`{{name}}`) is applied. |
| `order`    | no  | List of column names for `orderBy` |
| `distinct` | no  | Remove duplicate rows. Default: `false` |

### `type: join`

Joins two DataFrames.

```yaml
- name: enriched
  kind: transformer
  type: join
  left:
    input: load-orders
    alias: o
  right:
    input: load-customers
    alias: c
  join: left
  on: "o.customer_id = c.id"
  select:
    - o.id
    - "c.name as customer_name"
    - o.amount
```

| Field           | Required | Description |
|-----------------|----------|-------------|
| `left.input`    | yes | Name of the left-side action |
| `left.alias`    | yes | SQL alias for the left DataFrame |
| `right.input`   | yes | Name of the right-side action |
| `right.alias`   | yes | SQL alias for the right DataFrame |
| `join`          | yes | Spark join type: `inner`, `left`, `right`, `outer`, `left_semi`, `left_anti`, `cross` |
| `on`            | yes | SQL join condition expression (may reference both aliases) |
| `select`        | yes | List of SQL expressions for the output columns |

### `type: group`

Groups rows and applies aggregations.

```yaml
- name: daily-totals
  kind: transformer
  type: group
  input: load-orders
  groupBy:
    - customer_id
    - order_date
  aggregations:
    - "sum(amount) as total_amount"
    - "count(1) as order_count"
```

| Field          | Required | Description |
|----------------|----------|-------------|
| `input`        | yes | Name of a previous action |
| `groupBy`      | yes | List of column names to group by |
| `aggregations` | yes | List of SQL aggregate expressions. Must have at least one entry. |

### `type: pivot`

Groups rows, pivots a column into multiple output columns, and aggregates.

```yaml
- name: metrics-by-day
  kind: transformer
  type: pivot
  input: load-measurements
  groupBy:
    - day
    - region
  pivotColumn: metric_name
  aggregations:
    - "sum(value) as total"
```

| Field          | Required | Description |
|----------------|----------|-------------|
| `input`        | yes | Name of a previous action |
| `groupBy`      | yes | Columns to group by (become the row keys) |
| `pivotColumn`  | yes | Column whose distinct values become the new output column names |
| `aggregations` | yes | List of SQL aggregate expressions applied to each pivoted value |

### `type: unpivot`

Melts wide columns back into rows using Spark's `stack` function.

```yaml
- name: unpivoted
  kind: transformer
  type: unpivot
  input: wide-data
  columns:
    - id
    - day
  nameColumn: metric_name
  valueColumn: value
```

| Field         | Required | Description |
|---------------|----------|-------------|
| `input`       | yes | Name of a previous action |
| `columns`     | yes | Columns to **keep** as-is (not unpivoted). All other columns in the DataFrame are unpivoted. |
| `nameColumn`  | yes | Name of the new column that will hold the original column names |
| `valueColumn` | yes | Name of the new column that will hold the original column values |

### `type: drop`

Drops named columns from a DataFrame.

```yaml
- name: clean-data
  kind: transformer
  type: drop
  input: load-orders
  columns:
    - internal_id
    - audit_timestamp
```

| Field     | Required | Description |
|-----------|----------|-------------|
| `input`   | yes | Name of a previous action |
| `columns` | yes | List of column names to remove |

### `type: persist`

Caches a DataFrame to avoid recomputation when it is used by multiple downstream actions.

```yaml
- name: cached-data
  kind: transformer
  type: persist
  input: expensive-join
  level: MEMORY_ONLY
```

| Field   | Required | Description |
|---------|----------|-------------|
| `input` | yes | Name of a previous action |
| `level` | no  | Storage level hint. Currently the framework always uses `MEMORY_ONLY` regardless of this value. |

---

## Sinks (`kind: sink`)

### `type: show`

Prints the DataFrame to stdout. Useful for debugging.

```yaml
- name: preview
  kind: sink
  type: show
  input: filter-active
  printSchema: false
```

| Field         | Required | Description |
|---------------|----------|-------------|
| `input`       | yes | Name of a previous action |
| `printSchema` | no  | Print the DataFrame schema before showing rows. Default: `false` |

### `type: csv`

Writes the DataFrame to CSV files.

```yaml
- name: write-output
  kind: sink
  type: csv
  input: filter-active
  path: "/data/output/{{reportDate}}/results"
  mode: Overwrite
  options:
    header: "true"
    delimiter: ","
```

| Field     | Required | Description |
|-----------|----------|-------------|
| `input`   | yes | Name of a previous action |
| `path`    | yes | Output directory path. Variable substitution (`{{name}}`) is applied. |
| `mode`    | no  | Spark `SaveMode`: `Append`, `Overwrite`, `Ignore`, `ErrorIfExists`. Default: `Ignore` |
| `options` | no  | Map of CSV write options passed to Spark's `DataFrameWriter` (e.g. `header`, `delimiter`) |

### `type: jdbc`

Writes the DataFrame back to a JDBC database table.

```yaml
- name: write-db
  kind: sink
  type: jdbc
  input: filter-active
  mode: Append
  options:
    url: jdbc:postgresql://host:5432/db
    user: myuser
    password: mypassword
    driver: org.postgresql.Driver
    dbtable: output_table
```

| Field     | Required | Description |
|-----------|----------|-------------|
| `input`   | yes | Name of a previous action |
| `mode`    | no  | Spark `SaveMode`: `Append`, `Overwrite`, `Ignore`, `ErrorIfExists`. Default: `Append` |
| `options` | yes | Map of JDBC options passed to Spark's `DataFrameWriter`. Same keys as JDBC source. |
