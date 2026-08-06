# schemacrawler-kylin

![Java](https://img.shields.io/badge/Java-21-orange) ![License](https://img.shields.io/badge/License-Apache%202.0-blue)

SchemaCrawler plug-in for [Apache Kylin](https://kylin.apache.org/). It registers the `kylin` database system with SchemaCrawler so that `jdbc:kylin:*` connections can be inspected through the standard SchemaCrawler tool chain.

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Features & Status](#2-features--status)
- [3. Requirements & Compatibility](#3-requirements--compatibility)
- [4. Architecture & Modules](#4-architecture--modules)
- [5. Installation](#5-installation)
- [6. Quick Start](#6-quick-start)
- [7. Configuration](#7-configuration)
- [8. Core Usage / API](#8-core-usage--api)
- [9. Testing & Build](#9-testing--build)
- [10. Versioning & Branches](#10-versioning--branches)
- [11. Contributing & License](#11-contributing--license)

## 1. Project Overview

**What it is**

`schemacrawler-kylin` is a SchemaCrawler database plug-in for Apache Kylin. It plugs into SchemaCrawler's `DatabaseConnectorRegistry` through the JDK `ServiceLoader` mechanism, exposing the Kylin server type (`kylin`) so that SchemaCrawler CLI and library users can connect to Kylin via `jdbc:kylin:*` URLs and inspect its schemas.

**What it is not**

- It is not a Kylin JDBC driver — the actual driver is provided by `org.apache.kylin:kylin-jdbc` (a dependency of this module).
- It is not a full database-metadata emulator. Kylin's JDBC driver exposes a limited `DatabaseMetaData` implementation, so the quality of metadata (databases, tables, columns) depends on what the Kylin JDBC layer itself supports.

**Typical scenarios**

| Scenario | Description |
| :--- | :--- |
| Schema inspection of Kylin OLAP cubes | Run SchemaCrawler with `--server=kylin` to list databases, tables and columns of a Kylin instance. |
| Embedding in SchemaCrawler-based tooling | Any tool that resolves database connectors from `DatabaseConnectorRegistry` automatically picks up the Kylin plug-in once this jar is on the classpath. |
| Database documentation pipelines | Combine with SchemaCrawler commands (e.g. `list`, `schema`) to generate schema reports for Kylin. |

## 2. Features & Status

| Capability | Status | Notes |
| :--- | :--- | :--- |
| Registers server type `kylin` (Apache Kylin) | Available | `DatabaseServerType("kylin", "Apache Kylin")` |
| SPI auto-discovery via `META-INF/services` | Available | `schemacrawler.tools.databaseconnector.DatabaseConnector` service entry |
| URL matching for `jdbc:kylin:*` | Available | Implemented by `supportsUrlPredicate()` |
| Loads `org.apache.kylin.jdbc.Driver` | Available | `Class.forName` on connector construction |
| CLI help integration (`Connections.kylin.txt`) | Available | `--host`, `--port`, `--database`, `--user`, `--password` options |
| Information-schema views resource | Limited | `kylin.information_schema` resource ships with the plug-in; its content is currently minimal (empty baseline). |

> Status is reported as of `3.0.x.x.20260630-SNAPSHOT` on the `feature/3.0.x` branch.

## 3. Requirements & Compatibility

| Item | Version |
| :--- | :--- |
| JDK | 21+ |
| Maven | 3.0+ (Maven Wrapper 3.5.0 bundled) |
| SchemaCrawler | 16.7.2 |
| Kylin JDBC driver | 3.0.0 (`org.apache.kylin:kylin-jdbc`) |

**Version lines**

| Branch | JDK baseline | Version pattern |
| :--- | :--- | :--- |
| `feature/1.0.x` | JDK 8 | `1.0.x.*` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` |

## 4. Architecture & Modules

```text
 SchemaCrawler CLI / library
        |
        v
 DatabaseConnectorRegistry (ServiceLoader)
        |
        v
 KylinDatabaseConnector  <--(SPI: META-INF/services)-->  plug-in jar
        |
        |  url = jdbc:kylin://host:port/database
        v
 org.apache.kylin.jdbc.Driver  (kylin-jdbc dependency)
        |
        v
 Apache Kylin (OLAP engine)
```

This is a **single-module** project (packaging `jar`):

| Module / artifact | Role |
| :--- | :--- |
| `schemacrawler-kylin` | The SchemaCrawler plug-in: connector registration, URL matching, driver loading, CLI help, information-schema resource. |

## 5. Installation

The artifact is not yet published to Maven Central. Resolve it from the project's configured artifact repository (Aliyun Packages) or install it locally from source; the snapshot version currently used on the `feature/3.0.x` branch is `3.0.x.x.20260630-SNAPSHOT`.

**Maven**

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>schemacrawler-kylin</artifactId>
    <version>3.0.x.x.20260630-SNAPSHOT</version>
</dependency>
```

**Gradle**

```groovy
implementation 'io.github.easy4j:schemacrawler-kylin:3.0.x.x.20260630-SNAPSHOT'
```

## 6. Quick Start

**Via SchemaCrawler CLI** (the plug-in jar plus `kylin-jdbc` must be on the SchemaCrawler classpath):

```bash
schemacrawler --server=kylin \
  --host=localhost --port=7070 \
  --database=default --user=<user> --password=<password> \
  --info-level=standard --command=list
```

**Expected result:** SchemaCrawler recognizes `--server=kylin`, loads the Kylin JDBC driver and lists the databases/tables visible through the Kylin JDBC metadata layer (exact output depends on the Kylin version and on what the Kylin JDBC driver exposes).

**Via the library API** (plug-in discovery is automatic once the jar is on the classpath):

```java
import schemacrawler.tools.databaseconnector.DatabaseConnectorRegistry;

DatabaseConnectorRegistry registry =
        DatabaseConnectorRegistry.getDatabaseConnectorRegistry();

boolean hasKylin = registry.hasDatabaseSystemIdentifier("kylin"); // true
```

## 7. Configuration

The plug-in ships `schemacrawler-kylin.config.properties` with the following defaults:

| Property | Default | Meaning |
| :--- | :--- | :--- |
| `host` | `localhost` | Kylin host name |
| `port` | `10000` | Port used in the connection URL template |
| `database` | `default` | Database (project) name |

> **Assumption:** the shipped default `port=10000` appears to have been carried over from a Hive-oriented template. Apache Kylin instances commonly listen on port `7070`; verify the port against your deployment and override it explicitly when connecting.

The connection options exposed through SchemaCrawler help are `--host`, `--port`, `--database`, `--user`, `--password`.

## 8. Core Usage / API

The public API surface is intentionally small — one connector class:

| Class | Package | Role |
| :--- | :--- | :--- |
| `KylinDatabaseConnector` | `schemacrawler.server.kylin` | `DatabaseConnector` implementation; registers `kylin`, matches `jdbc:kylin:*` URLs, loads the Kylin JDBC driver. |

Constructing the connector directly:

```java
import schemacrawler.server.kylin.KylinDatabaseConnector;

// Throws IOException if the bundled config/information-schema
// resources cannot be loaded; throws RuntimeException if the
// Kylin JDBC driver class is not on the classpath.
KylinDatabaseConnector connector = new KylinDatabaseConnector();
```

## 9. Testing & Build

```bash
# Full build with tests and JaCoCo coverage report/check
./mvnw clean verify

# Run tests only
./mvnw test

# Install into the local repository
./mvnw install
```

Test & gate facts (as configured in the pom):

- JUnit 4 (`junit:junit:4.13.2`, test scope).
- One integration-style test class, `schemacrawler.integration.test.TestBundledDistributions`, which verifies that the bundled distribution registers the Kylin database system identifier through `DatabaseConnectorRegistry`.
- JaCoCo is bound to `prepare-agent` / `report` / `check`; the `check` rule requires a **90% line coverage ratio** (configured with `haltOnFailure=false`, i.e. reported rather than hard-failing).

## 10. Versioning & Branches

| Branch | JDK baseline | Version pattern | Status |
| :--- | :--- | :--- | :--- |
| `feature/1.0.x` | JDK 8 | `1.0.x.*` | Active; current snapshot `1.0.x.20260630-SNAPSHOT` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` | Maintained |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` | Maintained |

Maintenance strategy: the 1.0.x line keeps JDK 8 compatibility for legacy deployments; the 2.0.x and 3.0.x lines are the modern JDK baselines. Release artifacts are published to the project's configured artifact repository (Aliyun Packages) and GitHub Releases; the project has not yet published to Maven Central.

## 11. Contributing & License

Contributions are welcome — please open an issue or a pull request on the [GitHub repository](https://github.com/easy-4-java/schemacrawler-kylin). Code style follows the existing conventions of the repository (4-space indentation, commented Maven plugin/dependency blocks).

This project is licensed under the **Apache License 2.0**. See [LICENSE](LICENSE) for details.
