# Troubleshooting

## Pipeline name validation error

**Error:**
```
com.sparktf.exception.ValidationException: [name must be 5-30 char]
```

**Cause:** The `name` field at the top of the pipeline YAML must be between 5 and 30 characters.

**Fix:** Use a name that meets the constraint.

```yaml
# Too short (3 chars)
name: etl

# Valid
name: daily-etl-pipeline
```

---

## JDBC driver class not found

**Error:**
```
java.lang.ClassNotFoundException: org.postgresql.Driver
```

**Cause:** The JDBC driver JAR is not on the Spark classpath. The framework JAR does not bundle database drivers.

**Fix:** Pass the driver JAR with `--jars` at submission time.

```bash
spark-submit \
  --jars /path/to/postgresql-42.7.3.jar \
  --class com.sparktf.Application \
  target/spark-transform-framework-1.0.0.jar \
  -f my-pipeline.yaml
```

See [deployment.md](deployment.md) for a full list of common driver coordinates.

---

## Variable not resolved — literal `{{name}}` appears in output

**Cause:** Variable substitution is only applied in specific fields. `{{}}` placeholders in JDBC `options` maps are passed to Spark as literal strings and are never resolved.

**Fix:** Move parameterised values into a field where substitution is supported. See the [substitution scope table](dsl-reference.md#variable-substitution-scope) for the full list.

For JDBC sources, use `queryFile` instead of putting the variable in `options.dbtable`:

```yaml
# Does not work — options are not formatted
- name: load
  kind: source
  type: jdbc
  options:
    dbtable: "{{myTable}}"    # passed literally to Spark

# Works — queryFile content is formatted
- name: load
  kind: source
  type: jdbc
  options:
    url: jdbc:postgresql://host:5432/db
    user: myuser
    password: mypassword
    driver: org.postgresql.Driver
  queryFile: sql/load.sql     # {{myTable}} inside this file is resolved
```

---

## YAML file not found at runtime

**Error:**
```
java.lang.RuntimeException: java.io.FileNotFoundException
```

**Cause:** The `-f` path is resolved from the working directory of the Spark driver, not the machine that submitted the job.

**Fix:**
- In `client` deploy mode: use an absolute local path or a path relative to the submit directory.
- In `cluster` deploy mode (YARN/K8s): the driver runs on the cluster — store the YAML on HDFS, ADLS, S3, or a mounted volume and pass the full path.

---

## Action references a name that does not exist

**Error:**
```
java.lang.NullPointerException
  at com.sparktf.vo.actions.transformer.GeneralTransformer.transform
```

**Cause:** The `input` field references an action name that does not exist or appears later in the list.

**Fix:** Actions are executed in YAML order. Ensure the referenced action name appears above the current action in the `actions` list and is spelled exactly the same.

```yaml
actions:
  - name: load-data       # ← defined first
    kind: source
    ...

  - name: filter-data
    kind: transformer
    input: load-data      # ← must match exactly, including hyphens/case
```

---

## `persist` transformer has no effect on memory usage

**Cause:** `PersistTransformer` always uses `StorageLevel.MEMORY_ONLY()` regardless of the `level` field value. There is no way to configure a different storage level through the YAML at this time.

**Workaround:** This is a known limitation. If `MEMORY_ONLY` is insufficient (e.g. executor memory is too small), consider restructuring the pipeline to reduce how many times the same DataFrame is recomputed, or increase executor memory in the `spark-submit` command.

---

## `show` sink truncates column values

**Cause:** Spark's `Dataset.show()` truncates long string values by default. This is Spark's default behaviour — the framework calls `show(false)` which disables row truncation but column values wider than the display width are still trimmed by the terminal.

**Fix:** Use `printSchema: true` to verify column types, and consider writing to a `csv` sink for full data inspection.

---

## Out of memory on large datasets

**Symptoms:** Executor OOM errors, GC overhead, or stage failures.

**Common causes and fixes:**

| Cause | Fix |
|-------|-----|
| Large broadcast joins | Ensure the smaller side of a `join` is genuinely small, or switch to a sort-merge join by increasing `spark.sql.autoBroadcastJoinThreshold=-1` |
| Unpersisted DataFrame reused multiple times | Add a `type: persist` step before the shared DataFrame |
| Collecting all rows in `show` sink | `show` is for debugging only — avoid using it on production-sized data |
| Executor memory too low | Increase `--executor-memory` in the `spark-submit` command |
