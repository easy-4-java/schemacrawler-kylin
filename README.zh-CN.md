# schemacrawler-kylin

[English](./README.md) | [简体中文](./README.zh-CN.md)

![Java](https://img.shields.io/badge/Java-8-orange) ![License](https://img.shields.io/badge/License-Apache%202.0-blue)

[Apache Kylin](https://kylin.apache.org/) 的 SchemaCrawler 数据库插件。它通过 JDK `ServiceLoader` 机制将 `kylin` 数据库类型注册到 SchemaCrawler 的 `DatabaseConnectorRegistry`，使 SchemaCrawler 命令行与库用户可以通过 `jdbc:kylin:*` URL 连接 Kylin 并检查其 Schema。

## 目录

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

**是什么**

`schemacrawler-kylin` 是面向 Apache Kylin 的 SchemaCrawler 数据库插件。它通过 JDK `ServiceLoader` 机制接入 SchemaCrawler 的 `DatabaseConnectorRegistry`，对外暴露 Kylin 服务器类型（`kylin`），使 SchemaCrawler CLI 与库用户可以借助 `jdbc:kylin:*` URL 连接 Kylin 并检查其 Schema。

**不是什么**

- 它不是 Kylin JDBC 驱动——真正的驱动由本模块的依赖 `org.apache.kylin:kylin-jdbc` 提供。
- 它不是完整的数据库元数据模拟器。Kylin JDBC 驱动暴露的 `DatabaseMetaData` 实现能力有限，元数据（库、表、列）的完整程度取决于 Kylin JDBC 层本身的支持情况。

**典型场景**

| 场景 | 说明 |
| :--- | :--- |
| 检查 Kylin OLAP 多维数据集 Schema | 使用 `--server=kylin` 运行 SchemaCrawler，列出 Kylin 实例的数据库、表与列。 |
| 嵌入基于 SchemaCrawler 的工具链 | 任何通过 `DatabaseConnectorRegistry` 解析数据库连接器的工具，只要 classpath 中包含本 jar，即可自动发现 Kylin 插件。 |
| 数据库文档生成流水线 | 配合 SchemaCrawler 的 `list`、`schema` 等命令为 Kylin 生成 Schema 报告。 |

## 2. Features & Status

| 能力 | 状态 | 说明 |
| :--- | :--- | :--- |
| 注册服务器类型 `kylin`（Apache Kylin） | 可用 | `DatabaseServerType("kylin", "Apache Kylin")` |
| 通过 `META-INF/services` 实现 SPI 自动发现 | 可用 | `schemacrawler.tools.databaseconnector.DatabaseConnector` 服务条目 |
| 匹配 `jdbc:kylin:*` URL | 可用 | 由 `supportsUrlPredicate()` 实现 |
| 加载 `org.apache.kylin.jdbc.Driver` | 可用 | 连接器构造时执行 `Class.forName` |
| CLI 帮助集成（`Connections.kylin.txt`） | 可用 | `--host`、`--port`、`--database`、`--user`、`--password` 选项 |
| information-schema 视图资源 | 有限 | 插件随包提供 `kylin.information_schema` 资源，目前内容为最小化基线（空文件）。 |

> 状态以 `feature/1.0.x` 分支上的 `1.0.x.20260630-SNAPSHOT` 为准。

## 3. Requirements & Compatibility

| 项目 | 版本 |
| :--- | :--- |
| JDK | 8+ |
| Maven | 3.0+（内置 Maven Wrapper 3.5.0） |
| SchemaCrawler | 16.7.2 |
| Kylin JDBC 驱动 | 3.0.0（`org.apache.kylin:kylin-jdbc`） |

**版本线**

| 分支 | JDK 基线 | 版本模式 |
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
 KylinDatabaseConnector  <--(SPI: META-INF/services)-->  插件 jar
        |
        |  url = jdbc:kylin://host:port/database
        v
 org.apache.kylin.jdbc.Driver  (kylin-jdbc 依赖)
        |
        v
 Apache Kylin (OLAP 引擎)
```

本项目为**单模块**工程（packaging 为 `jar`）：

| 模块 / 构件 | 职责 |
| :--- | :--- |
| `schemacrawler-kylin` | SchemaCrawler 插件本体：连接器注册、URL 匹配、驱动加载、CLI 帮助、information-schema 资源。 |

## 5. Installation

该构件尚未发布到 Maven Central。请从项目配置的制品仓库（阿里云制品仓库）获取，或从源码本地安装；`feature/1.0.x` 分支当前使用的快照版本为 `1.0.x.20260630-SNAPSHOT`。

**Maven**

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>schemacrawler-kylin</artifactId>
    <version>1.0.x.20260630-SNAPSHOT</version>
</dependency>
```

**Gradle**

```groovy
implementation 'io.github.easy4j:schemacrawler-kylin:1.0.x.20260630-SNAPSHOT'
```

## 6. Quick Start

**通过 SchemaCrawler CLI**（需将插件 jar 与 `kylin-jdbc` 放入 SchemaCrawler 的 classpath）：

```bash
schemacrawler --server=kylin \
  --host=localhost --port=7070 \
  --database=default --user=<user> --password=<password> \
  --info-level=standard --command=list
```

**预期结果：** SchemaCrawler 识别 `--server=kylin`，加载 Kylin JDBC 驱动，并列出 Kylin JDBC 元数据层可见的数据库与表（具体输出取决于 Kylin 版本及其 JDBC 驱动的支持情况）。

**通过库 API**（jar 在 classpath 中即可自动发现插件）：

```java
import schemacrawler.tools.databaseconnector.DatabaseConnectorRegistry;

DatabaseConnectorRegistry registry =
        DatabaseConnectorRegistry.getDatabaseConnectorRegistry();

boolean hasKylin = registry.hasDatabaseSystemIdentifier("kylin"); // true
```

## 7. Configuration

插件随包提供 `schemacrawler-kylin.config.properties`，默认值如下：

| 属性 | 默认值 | 含义 |
| :--- | :--- | :--- |
| `host` | `localhost` | Kylin 主机名 |
| `port` | `10000` | 连接 URL 模板中使用的端口 |
| `database` | `default` | 数据库（项目）名称 |

> **假设：** 随包默认值 `port=10000` 疑似从 Hive 模板继承而来。Apache Kylin 实例通常监听 7070 端口；请根据你的部署环境核对端口，并在连接时显式覆盖。

通过 SchemaCrawler 帮助暴露的连接选项为 `--host`、`--port`、`--database`、`--user`、`--password`。

## 8. Core Usage / API

公开 API 刻意保持精简——只有一个连接器类：

| 类 | 包 | 职责 |
| :--- | :--- | :--- |
| `KylinDatabaseConnector` | `schemacrawler.server.kylin` | `DatabaseConnector` 实现；注册 `kylin` 类型、匹配 `jdbc:kylin:*` URL、加载 Kylin JDBC 驱动。 |

直接构造连接器：

```java
import schemacrawler.server.kylin.KylinDatabaseConnector;

// 若无法加载随包配置/information-schema 资源则抛出 IOException；
// 若 classpath 中不存在 Kylin JDBC 驱动类则抛出 RuntimeException。
KylinDatabaseConnector connector = new KylinDatabaseConnector();
```

## 9. Testing & Build

```bash
# 完整构建（含测试与 JaCoCo 覆盖率报告/检查）
./mvnw clean verify

# 仅运行测试
./mvnw test

# 安装到本地仓库
./mvnw install
```

测试与门禁事实（以 pom 配置为准）：

- JUnit 4（`junit:junit:4.13.2`，test 作用域）。
- 一个集成风格测试类 `schemacrawler.integration.test.TestBundledDistributions`，通过 `DatabaseConnectorRegistry` 验证随包发行版注册了 Kylin 数据库系统标识。
- JaCoCo 绑定 `prepare-agent` / `report` / `check`；`check` 规则要求**行覆盖率不低于 90%**（配置了 `haltOnFailure=false`，即仅报告而不强制失败）。

## 10. Versioning & Branches

| 分支 | JDK 基线 | 版本模式 | 状态 |
| :--- | :--- | :--- | :--- |
| `feature/1.0.x` | JDK 8 | `1.0.x.*` | 活跃；当前快照 `1.0.x.20260630-SNAPSHOT` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` | 维护中 |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` | 维护中 |

维护策略：1.0.x 版本线保持 JDK 8 兼容，服务于存量部署；2.0.x 与 3.0.x 版本线为现代 JDK 基线。发布制品发布到项目配置的制品仓库（阿里云制品仓库）与 GitHub Releases；项目尚未发布到 Maven Central。

## 11. Contributing & License

欢迎参与贡献——请在 [GitHub 仓库](https://github.com/easy-4-java/schemacrawler-kylin) 提交 Issue 或 Pull Request。代码风格遵循仓库既有约定（4 空格缩进、Maven 插件/依赖块带注释）。

本项目基于 **Apache License 2.0** 开源。详见 [LICENSE](LICENSE)。
