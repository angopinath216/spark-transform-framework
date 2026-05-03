# Deployment Guide

## Prerequisites

- Java 17
- Maven 3.8+
- Apache Spark 3.5.x on the target cluster
- JDBC driver JAR(s) for any database you read from or write to

## Build

```bash
mvn package -DskipTests
```

Produces a fat JAR at `target/spark-transform-framework-<version>.jar`. All framework dependencies are shaded in. Spark itself is **not** bundled (`provided` scope) — it must be available on the cluster.

---

## Running Locally

Useful for development and testing a pipeline before deploying to a cluster.

```bash
spark-submit \
  --class com.sparktf.Application \
  --jars /path/to/postgresql-42.7.3.jar \
  target/spark-transform-framework-1.0.0.jar \
  -f my-pipeline.yaml
```

`Application.main()` defaults the Spark master to `local[*]` when no `--master` flag is provided, so all cores on your machine are used.

---

## JDBC Drivers

Spark does not bundle database drivers. You must supply the JDBC driver JAR at submission time. The framework JAR does not include drivers either.

```bash
spark-submit \
  --class com.sparktf.Application \
  --jars /path/to/driver.jar \
  target/spark-transform-framework-1.0.0.jar \
  -f my-pipeline.yaml
```

Common drivers and their Maven coordinates:

| Database | Driver class | Maven artifact |
|----------|-------------|----------------|
| PostgreSQL | `org.postgresql.Driver` | `org.postgresql:postgresql:42.7.3` |
| MySQL | `com.mysql.jdbc.Driver` | `mysql:mysql-connector-java:8.0.33` |
| SQL Server | `com.microsoft.sqlserver.jdbc.SQLServerDriver` | `com.microsoft.sqlserver:mssql-jdbc:12.6.1.jre11` |
| Oracle | `oracle.jdbc.OracleDriver` | `com.oracle.database.jdbc:ojdbc11:23.4.0.24.05` |

If using multiple databases in one pipeline, pass all drivers as a comma-separated list:

```bash
--jars /path/to/postgresql.jar,/path/to/mysql.jar
```

---

## YARN

```bash
spark-submit \
  --master yarn \
  --deploy-mode cluster \
  --num-executors 4 \
  --executor-cores 2 \
  --executor-memory 4g \
  --driver-memory 2g \
  --class com.sparktf.Application \
  --jars /path/to/postgresql-42.7.3.jar \
  hdfs:///apps/spark-transform-framework-1.0.0.jar \
  -f hdfs:///pipelines/my-pipeline.yaml
```

Notes:
- In `cluster` deploy mode, both the JAR and the pipeline YAML must be reachable by the YARN cluster — use HDFS or a shared filesystem path.
- In `client` deploy mode, the YAML can be a local path on the submitting node.
- The `--master local[*]` set in `Application.main()` is overridden by the `--master yarn` flag.

---

## Kubernetes

```bash
spark-submit \
  --master k8s://https://<k8s-api-server>:6443 \
  --deploy-mode cluster \
  --name spark-transform \
  --class com.sparktf.Application \
  --conf spark.executor.instances=3 \
  --conf spark.kubernetes.container.image=apache/spark:3.5.5 \
  --conf spark.kubernetes.driver.volumes.persistentVolumeClaim.pipeline-pvc.mount.path=/pipelines \
  --conf spark.kubernetes.driver.volumes.persistentVolumeClaim.pipeline-pvc.options.claimName=pipeline-pvc \
  --jars local:///opt/spark/jars/postgresql-42.7.3.jar \
  local:///opt/spark/jars/spark-transform-framework-1.0.0.jar \
  -f /pipelines/my-pipeline.yaml
```

Notes:
- The framework JAR and JDBC drivers must be baked into the Spark container image or mounted via a volume.
- The pipeline YAML must be accessible from the driver pod at runtime (volume mount, ConfigMap, or object storage).
- Use `local://` prefix for JARs already present in the container image.

---

## Databricks

Upload the fat JAR to DBFS or a Unity Catalog volume, then run via a Databricks Job.

**Cluster configuration:**
1. In the Databricks cluster settings, add the JDBC driver JAR under **Libraries** → **DBFS/ADLS**.
2. Add the framework JAR as a cluster library or attach it to the job task.

**Job task (Spark JAR task):**
- Main class: `com.sparktf.Application`
- Parameters: `["-f", "dbfs:/pipelines/my-pipeline.yaml"]`

**Pipeline YAML location:**
Store the YAML on DBFS, ADLS, or S3 and pass the full path as the `-f` argument.

---

## Passing Secrets

Avoid hardcoding database passwords in YAML files. Instead:

**Option 1 — Environment variables via `spark-submit`:**

```bash
spark-submit \
  --conf spark.executorEnv.DB_PASSWORD=secret \
  --conf spark.yarn.appMasterEnv.DB_PASSWORD=secret \
  ...
```

Then use a shell wrapper that writes the YAML with the resolved password before submitting.

**Option 2 — External secrets manager:**

Pre-process the YAML with a tool like `envsubst` or Vault agent before passing it to `-f`.

**Option 3 — JDBC options via a properties file:**

Spark's JDBC reader supports a `connectionProperties` file. Pass sensitive options there instead of in the YAML.
