# Architecture

## Overview

The framework has two phases for every pipeline run: **validate** then **transform**. Both phases walk the same ordered list of actions, so validation errors surface before any Spark job is submitted.

```
spark-submit → Application.main()
                    │
                    ▼
             TransformationFlow.run()
                    │
                    ├─ getRoot()       parse YAML → Root object
                    ├─ validate()      validate Root + each action + each variable
                    └─ transform()     execute each action in order
```

---

## Key Classes

### `Application`

Entry point. Parses the `-f <file>` CLI argument via Apache Commons CLI, builds a `SparkSession`, and delegates to `TransformationFlow`.

### `TransformationFlow`

Orchestrates the pipeline lifecycle:

1. **`getRoot(yamlFile)`** — Deserializes the YAML file into a `Root` object using Jackson + `YAMLFactory`. Three `MixIn` classes (`SourceDeserializerMixIn`, `TransformerDeserializerMixIn`, `SinkDeserializerMixIn`) tell Jackson to use custom deserializers for each action kind.

2. **`validate(root, data)`** — Runs Hibernate Validator constraints on `Root` (e.g. `@NotNull`, `@Size` on `name`), then calls `root.validate(data)` which chains `validate()` on every variable and every action.

3. **`transform(root, data)`** — Calls `root.transform(data)` which calls `transform()` on each action in the list order.

### `TransformationData`

A simple runtime context object passed through every step. Holds:
- `SparkSession` — shared session for the pipeline run
- `Map<String, Dataset<Row>> datasets` — named DataFrames produced by each action; downstream actions look up their input by the upstream action's `name`
- `Map<String, String> variables` — resolved variable values, populated during the validate phase

### `TransformationStep` (interface)

The single contract that every action implements:

```java
void validate(TransformationData data) throws ValidationException;
void transform(TransformationData data);
```

`Action` is the abstract base class that implements this interface. `Source`, `Transformer`, and `Sink` all extend `Action`. Each concrete class (e.g. `GeneralTransformer`, `JdbcSource`) implements both methods directly.

### `Formatter`

Handles variable substitution in option strings. Uses `{{variableName}}` as the delimiter (Apache Commons Text `StringSubstitutor`). Three methods:

| Method | Used by |
|--------|---------|
| `formatString(str, vars)` | `CsvSink` path, `JdbcSource` query file content |
| `formatList(list, vars)` | `GeneralTransformer` select expressions |
| `formatMap(map, vars)` | Available for extension; not yet used in built-in actions |

---

## Polymorphic Deserialization

Jackson determines which concrete class to instantiate for each action using a two-layer MixIn pattern:

```
YAML action node
      │
      ▼ TransformerDeserializerMixIn (@JsonDeserialize → TransformerDeserializer)
      │
      ▼ TransformerDeserializer  (creates a fresh ObjectMapper + TransformerMixIn)
      │
      ▼ TransformerMixIn  (@JsonTypeInfo on "type" field + @JsonSubTypes registry)
      │
      ▼ concrete class (e.g. GeneralTransformer, JoinTransformer)
```

The `@JsonSubTypes` registry in each `MixIn` is the single place to register a new type:

| MixIn | Registers types for |
|-------|---------------------|
| `TransformerMixIn` | `general`, `join`, `group`, `pivot`, `unpivot`, `drop`, `persist` |
| `SourceMixIn` | `jdbc`, `generic` |
| `SinkMixIn` | `show`, `csv`, `jdbc` |

---

## Pipeline Execution Model

Actions are executed **sequentially** in the order they appear in the YAML. Each action reads its named input from `TransformationData.datasets` and writes its output back under its own `name`. This means:

- An action can only reference inputs that appear **earlier** in the list.
- A single DataFrame can be referenced by multiple downstream actions.
- Use `type: persist` before a DataFrame that feeds multiple expensive downstream transformers.

```
actions:
  - name: A   (source)       → datasets["A"]
  - name: B   (transformer)  reads datasets["A"], writes datasets["B"]
  - name: C   (transformer)  reads datasets["A"], writes datasets["C"]  ← reuses A
  - name: D   (sink)         reads datasets["C"]
```

---

## Known Limitations

| Area | Limitation |
|------|-----------|
| **`persist` storage level** | `PersistTransformer` declares a `level` field but always calls `inputData.persist(StorageLevel.MEMORY_ONLY())` — the field value is ignored. All persisted DataFrames use `MEMORY_ONLY` regardless of what is set in the YAML. |
| **Variable substitution in JDBC options** | `JdbcSource` and `JdbcSink` pass their `options` map directly to Spark without calling `Formatter.formatMap`. `{{variableName}}` placeholders in `options` values are passed to Spark as literal strings. Use `queryFile` for parameterised SQL. |
| **`JdbcSource.table` field** | `JdbcSource` declares a `table` field, but it is never read in `transform()`. The table to query must be set via `options.dbtable` or `queryFile`. |
| **`Root.configuration` field** | The root pipeline object accepts a `configuration: Map<String, String>` key, but it is not wired into `TransformationData` or any action. It is reserved and currently has no effect. |
| **No parallel action execution** | `Root.transform()` iterates actions with `forEach` — execution is strictly sequential. There is no built-in way to run independent branches in parallel. |
| **`SparkSession` master hardcoded in `Application`** | `Application.main()` calls `SparkSession.builder().master("local[*]")`. When deploying to a cluster the `--master` flag in `spark-submit` overrides this, but the code always sets the default to local. |
