# Changelog

All notable changes to this project will be documented in this file.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-05-03

### Added
- YAML pipeline DSL with `source`, `transformer`, and `sink` action kinds
- Sources: `jdbc`, `generic` (CSV, Parquet, etc.)
- Transformers: `general` (filter/select/rename/order), `join`, `group`, `pivot`, `unpivot`, `drop`, `persist`
- Sinks: `show`, `csv`, `jdbc`
- Variable substitution in pipeline options (`string` and `expression` types)
- Fat JAR packaging via `maven-shade-plugin` for `spark-submit`
- CI workflow (build + test on push to `main` and `develop`)
- Annotated examples: `simple-jdbc-transform.yaml`, `join-and-pivot.yaml`
