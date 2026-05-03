# Spark Transformation Framework

A declarative, YAML-driven Spark data transformation framework. Define your entire pipeline — sources, transformations, and sinks — in a single YAML file, then run it with `spark-submit`.

## Quick Start

### Build

```bash
mvn package -DskipTests
```

This produces a fat JAR at `target/spark-transform-framework-1.0.0-SNAPSHOT.jar`.

### Run

```bash
spark-submit \
  --class com.sparktf.Application \
  target/spark-transform-framework-1.0.0-SNAPSHOT.jar \
  -f examples/simple-jdbc-transform.yaml
```

## YAML Pipeline DSL

A pipeline file has three top-level sections:

| Section     | Purpose                                              |
|-------------|------------------------------------------------------|
| `name`      | Logical name for the pipeline (used in logging)     |
| `variables` | Named values injected into action options            |
| `actions`   | Ordered list of sources, transformers, and sinks     |

### Action kinds

| Kind          | Description                                                    |
|---------------|----------------------------------------------------------------|
| `source`      | Loads a DataFrame (JDBC, CSV, generic file)                   |
| `transformer` | Transforms a named input DataFrame                             |
| `sink`        | Writes or displays a DataFrame                                 |

### Source types

| Type      | Description                         |
|-----------|-------------------------------------|
| `jdbc`    | Reads from any JDBC-compatible DB   |
| `generic` | Reads from a file path (CSV, Parquet, etc.) |

### Transformer types

| Type        | Description                                      |
|-------------|--------------------------------------------------|
| `general`   | filter, select, rename, order                    |
| `join`      | left / inner / right join between two inputs     |
| `group`     | group-by with aggregations                       |
| `pivot`     | pivot a column into multiple value columns       |
| `unpivot`   | unpivot wide columns back into rows              |
| `drop`      | drop named columns                               |
| `persist`   | cache a DataFrame to avoid re-computation        |

### Sink types

| Type   | Description                              |
|--------|------------------------------------------|
| `show` | Print to stdout (useful for debugging)   |
| `csv`  | Write to CSV files                       |
| `jdbc` | Write back to a JDBC database table      |

## Examples

See the [`examples/`](examples/) directory for annotated YAML pipelines:

| File | Demonstrates |
|------|-------------|
| [`simple-jdbc-transform.yaml`](examples/simple-jdbc-transform.yaml) | JDBC source, `general` transformer (filter, select, order), `string` and `expression` variables, `show` sink |
| [`join-and-pivot.yaml`](examples/join-and-pivot.yaml) | Two JDBC sources, `join`, `pivot`, `csv` sink |
| [`generic-source-to-csv.yaml`](examples/generic-source-to-csv.yaml) | `generic` source (Parquet/CSV/JSON), `drop` transformer, `distinct`, `csv` sink |
| [`group-and-aggregate.yaml`](examples/group-and-aggregate.yaml) | `generic` source, `group` transformer with multiple aggregations, `printSchema` |
| [`unpivot-wide-table.yaml`](examples/unpivot-wide-table.yaml) | `pivot` then `unpivot` — wide-to-long and back, multiple `show` sinks |
| [`persist-and-multi-sink.yaml`](examples/persist-and-multi-sink.yaml) | `persist` transformer, two downstream branches, `csv` sink + `jdbc` sink |
| [`jdbc-query-file.yaml`](examples/jdbc-query-file.yaml) | JDBC source with `queryFile` — the correct way to use `{{variables}}` in JDBC queries |

## Project Structure

```
src/
├── main/java/com/sparktf/
│   ├── Application.java          # Entry point & CLI parsing
│   ├── core/
│   │   ├── TransformationFlow.java   # Pipeline orchestrator
│   │   ├── TransformationStep.java   # Per-action execution logic
│   │   ├── TransformationData.java   # Runtime state (named DataFrames)
│   │   └── Formatter.java            # Variable substitution in option strings
│   ├── exception/
│   │   └── ValidationException.java
│   └── vo/                       # Value objects (Jackson-deserialized from YAML)
│       ├── Root.java
│       ├── actions/
│       │   ├── source/           # Source implementations + serde
│       │   ├── transformer/      # Transformer implementations + serde
│       │   └── sink/             # Sink implementations + serde
│       └── variables/            # Variable types (string, expression)
└── test/
    ├── java/com/sparktf/  # JUnit 5 tests
    └── resources/transformers/   # Sample YAML + SQL for tests
```

## Adding a New Transformer

1. Create a class extending `Transformer` in `vo/actions/transformer/`.
2. Add a constant to `TransformerTypes`.
3. Register it in `TransformerDeserializer`.
4. Implement the execution logic in `TransformationStep`.

## Requirements

- Java 17
- Maven 3.8+
- Apache Spark 3.5.x (provided by the cluster; not bundled in the JAR)

## License

Apache License 2.0
