# Contributing

## Branch strategy

| Branch | Purpose |
|--------|---------|
| `main` | Production-ready code — tagged releases only, no direct push |
| `develop` | Integration branch — all features merge here first |
| `feature/<short-description>` | New work, branched from `develop` |
| `hotfix/<short-description>` | Critical fixes, branched from `main` |
| `release/<version>` | Release prep, branched from `develop` |

## Workflow

```bash
git checkout develop
git pull
git checkout -b feature/my-new-transformer
# ... make changes ...
git push origin feature/my-new-transformer
# open a PR targeting develop
```

## Adding a new transformer

1. Create a class extending `Transformer` in `src/main/java/com/sparktf/vo/actions/transformer/`.
   Implement `validate(TransformationData)` and `transform(TransformationData)` directly in that class.
2. Add a constant to `TransformerTypes`.
3. Register it in `TransformerMixIn` — add a `@JsonSubTypes.Type` entry mapping your constant to your class.
4. Add a test in `src/test/java/com/sparktf/`.
5. Add an entry to `examples/` showing the new YAML syntax.

## Adding a new source

1. Create a class extending `Source` in `src/main/java/com/sparktf/vo/actions/source/`.
   Implement `validate(TransformationData)` and `transform(TransformationData)`.
2. Add a constant to `SourceTypes`.
3. Register it in `SourceMixIn` — add a `@JsonSubTypes.Type` entry.
4. Add a test.

## Adding a new sink

1. Create a class extending `Sink` in `src/main/java/com/sparktf/vo/actions/sink/`.
   Implement `validate(TransformationData)` and `transform(TransformationData)`.
2. Add a constant to `SinkTypes`.
3. Register it in `SinkMixIn` — add a `@JsonSubTypes.Type` entry.
4. Add a test.

## Running tests locally

```bash
mvn verify
```

## Release process

1. On `develop`, update `CHANGELOG.md` — move `[Unreleased]` entries under the new version heading.
2. Bump `pom.xml` version from `x.y.z-SNAPSHOT` to `x.y.z`.
3. Open a PR from `develop` → `main`.
4. After merge, tag `main`:
   ```bash
   git tag v1.1.0
   git push origin v1.1.0
   ```
5. The `release.yml` workflow automatically builds the JAR and creates a GitHub Release.
6. Back on `develop`, bump `pom.xml` to the next `SNAPSHOT` version.

## Commit message style

```
<type>: <short summary>

Types: feat | fix | refactor | test | docs | ci | chore
```

Examples:
- `feat: add parquet sink type`
- `fix: null pointer in JoinTransformer when right DataFrame is empty`
- `docs: document unpivot options in README`
