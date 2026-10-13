# TODAY Infrastructure

[简体中文](README.md) | English

![Logo](./logo.svg)

A modular Java application framework with dependency injection, auto-configuration, Web MVC, Netty and data access support.

![Java 17+](https://img.shields.io/badge/JDK-17+-success.svg)
[![Apache License 2.0](https://img.shields.io/github/license/TAKETODAY/today-infrastructure?color=blue)](LICENSE)
[![CI](https://github.com/TAKETODAY/today-infrastructure/actions/workflows/ci.yaml/badge.svg)](https://github.com/TAKETODAY/today-infrastructure/actions/workflows/ci.yaml)

[Quick start](#quick-start) · [Project setup](#project-setup) · [Real-world application](#real-world-application-today-blog) · [Documentation](https://docs.today-tech.cn/today-infrastructure/) · [Samples](infra-samples) · [Contributing](#contributing) · [Discussions](https://github.com/TAKETODAY/today-infrastructure/discussions)

## Overview

TODAY Infrastructure began in 2017 as infrastructure for the author's personal blog and has evolved into a multi-module Java application framework. It adapts parts of Spring Framework and Spring Boot alongside its own implementations, and powers the author's website, [taketoday.cn](https://taketoday.cn).

The project is individually maintained, with a focus on framework design, source-code research and practical application development.

## Who is it for?

- Java developers exploring dependency injection, AOP, bean lifecycles and Web MVC.
- Developers interested in framework architecture, bytecode, dynamic proxies and module boundaries.
- Developers researching how Netty integrates with an application framework.
- Individuals and small projects interested in experimenting with infrastructure they can inspect and modify.
- Contributors interested in documentation, tests, examples and design discussions.

If your priority is a broad third-party ecosystem, commercial support or established enterprise integrations, Spring Boot remains the more established choice.

## Features

| Area | Capabilities |
|------|--------------|
| Application runtime | `@InfraApplication`, `Application`, `ApplicationBuilder`, auto-configuration, environment and lifecycle support |
| IoC container | Bean definitions, dependency injection, component scanning, configuration classes and events |
| AOP | Advisors, method interception and automatic proxy creation |
| Web | MVC request mapping, parameter binding, exception handling, response writing and WebSocket |
| HTTP | Netty server integration, HTTP abstractions, clients and declarative HTTP services |
| Data access | JDBC, transactions, persistence and database testing support |
| Optional integrations | Jackson, Gson, JSON-B, Protobuf, templates and mail |
| Tooling | BOMs, starters, Gradle plugin, configuration processors, testing and application packaging |

Choose application-oriented `infra-starter-*` modules or use lower-level `infra-*` modules directly. See [settings.gradle](settings.gradle) for the module inventory.

## Relationship with Spring

TODAY Infrastructure studies, adapts and evolves designs and implementations from Spring Framework and Spring Boot within its own engineering structure.

- Parts of the implementation are ported and adapted from those projects.
- The project has its own Java package namespace, artifact coordinates, application runtime and build tooling.
- It is not a binary-compatible replacement for Spring Framework or Spring Boot. Compatibility with Spring ecosystem components should not be assumed.
- Third-party source files retain their original copyright and license headers, with additional modification notices where applicable.

See [NOTICE](NOTICE) for attribution and third-party license information.

### Contributing back upstream

While maintaining TODAY Infrastructure and studying and adapting upstream code, I have also found issues and submitted some of the fixes upstream. The following Spring Framework changes have been integrated into the upstream repository:

- [Implement DefaultErrorResponseBuilder.headers(Consumer), #33156](https://github.com/spring-projects/spring-framework/pull/33156).
- [Optimize InlineMap and InlineList in SpEL, #30251](https://github.com/spring-projects/spring-framework/pull/30251).
- [Optimize array creation in SpEL ConstructorReference, #30189](https://github.com/spring-projects/spring-framework/pull/30189).
- [Copy methodAnnotations in the MethodParameter copy constructor, #36342](https://github.com/spring-projects/spring-framework/pull/36342).

I have also contributed merged changes to [MyBatis, #2340](https://github.com/mybatis/mybatis-3/pull/2340) and [Halo, #9888](https://github.com/halo-dev/halo/pull/9888). Some contributions grew out of maintaining this project; others came from studying source code and working on other software.

These contributions are part of the same learning process: understand an implementation, investigate problems, and contribute applicable changes back to the original project.

## Real-world application: TODAY BLOG

[TODAY BLOG](https://github.com/TAKETODAY/today-blog) is an open-source blog application built by the author using TODAY Infrastructure.

| Resource | Details |
|----------|---------|
| Website | [taketoday.cn](https://taketoday.cn) |
| Source | [TAKETODAY/today-blog](https://github.com/TAKETODAY/today-blog) |
| Backend | Java + TODAY Infrastructure; see [blog-api](https://github.com/TAKETODAY/today-blog/tree/main/blog-api) |
| Frontend | React for desktop, Vue for mobile, React + Ant Design for administration |

Explore the application for examples of dependencies, configuration, HTTP endpoints and frontend/backend integration. Consult its [README](https://github.com/TAKETODAY/today-blog#readme) and source configuration before running it locally; external dependencies and application settings must be prepared.

## Quick start

### Run the sample application

The [infra-samples](infra-samples) project includes build configuration and a Gradle Wrapper. Prepare Git and a JDK compatible with the sample's Gradle version. The framework targets Java 17+, but the Gradle runtime may require a newer JDK; check [the wrapper configuration](infra-samples/gradle/wrapper/gradle-wrapper.properties) when selecting your environment.

Runtime requirements:

- **Application runtime JDK**: the framework targets Java 17+. The default Netty sample has been verified on JDK 17.0.16 and JDK 25.0.3 (`GET /index` returns HTTP 200 with the body `Hello`). Older JDKs were not tested, and this does not establish a minimum for every module or optional feature.
- **Gradle runtime JDK**: the sample ships a Gradle Wrapper (9.8.0). The verified build used JDK 25.0.3; the Gradle runtime usually needs a newer JDK than the application target, so follow the [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html).
- **Framework source build toolchain**: building the full source tree has its own JDK requirements and is separate from running the sample; see the project build configuration rather than the quick start.

```bash
git clone https://github.com/TAKETODAY/today-infrastructure.git
cd today-infrastructure/infra-samples
./gradlew :netty-app:infraRun --args='--server.port=8080'
```

On Windows, use `gradlew.bat` instead of `./gradlew`. The first run downloads dependencies. The framework version is read from the repository's root `gradle.properties`, and artifacts are resolved through the configured Maven repositories.

Once the server starts, run in another terminal:

```bash
curl http://localhost:8080/index
```

Expected response:

```text
Hello
```

See [NettyDemoApplication.java](infra-samples/netty-app/src/main/java/infra/samples/NettyDemoApplication.java) for the entry point and [DemoController.java](infra-samples/web-api/src/main/java/infra/samples/DemoController.java) for the endpoint. Modify the controller, stop the server and restart it to try your changes.

### Create your own application

After configuring dependencies as described in [Project setup](#project-setup), create `src/main/java/example/DemoApplication.java`:

```java
package example;

import infra.app.Application;
import infra.app.InfraApplication;
import infra.web.annotation.GET;
import infra.web.annotation.RestController;

@RestController
@InfraApplication
public class DemoApplication {

  public static void main(String[] args) {
    Application.run(DemoApplication.class, args);
  }

  @GET("/hello")
  public String hello() {
    return "Hello, TODAY!";
  }
}
```

- `@InfraApplication` declares the application configuration entry point and enables component scanning and auto-configuration.
- `@RestController` declares a controller whose methods return response content.
- `@GET("/hello")` maps GET requests to the method.
- `Application.run(...)` starts the application context and the configured web server.

The example combines the entry point and controller for simplicity. In larger applications, place controllers in the application package or its subpackages so that component scanning can discover them.

From your application project, with the `infra.application` plugin and Gradle Wrapper configured:

```bash
./gradlew infraRun --args='--server.port=8080'
```

Alternatively, run `DemoApplication.main()` in your IDE with `--server.port=8080` as a program argument.

```bash
curl http://localhost:8080/hello
```

Expected response: `Hello, TODAY!`. If port 8080 is occupied, change `server.port` and use the corresponding port in your request.

### Explore further

| Topic | Resource |
|-------|----------|
| Parameters, response objects and SSE | [web-api](infra-samples/web-api) |
| Netty application configuration | [netty-app](infra-samples/netty-app) |
| A complete business application | [TODAY BLOG](#real-world-application-today-blog) |
| Module usage and API documentation | [Online documentation](https://docs.today-tech.cn/today-infrastructure/) |

Some documentation and contribution instructions are currently in Chinese.

## Project setup

The sample build provides a reference for Gradle integration:

- [buildSrc/build.gradle](infra-samples/buildSrc/build.gradle): loads the framework BOM and Gradle plugin.
- [build.gradle](infra-samples/build.gradle): applies `infra.application` and configures repositories and common dependencies.
- [netty-app/build.gradle](infra-samples/netty-app/build.gradle): declares application and server starters.

For a Web MVC application using Netty, the main starters are:

```groovy
dependencies {
  implementation 'cn.taketoday:infra-starter-application'
  implementation 'cn.taketoday:infra-starter-webmvc'
  implementation 'cn.taketoday:infra-starter-netty-server'
}
```

This dependency snippet assumes version management and repositories have already been configured, as in the sample build. Snapshot artifacts use `https://central.sonatype.com/repository/maven-snapshots/`. Use the matching framework and plugin versions; see the root [gradle.properties](gradle.properties) for the current development version.

## Contributing

Contributions can include:

- Trying the examples and reporting reproducible problems.
- Improving setup instructions and English documentation.
- Adding tests and practical examples.
- Reproducing performance measurements and documenting their conditions.
- Discussing framework design and module boundaries.
- Fixing bugs and proposing improvements.

Read [CONTRIBUTING.md](CONTRIBUTING.md) before contributing. Browse [good first issues](https://github.com/TAKETODAY/today-infrastructure/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22), report problems in [Issues](https://github.com/TAKETODAY/today-infrastructure/issues), or join [Discussions](https://github.com/TAKETODAY/today-infrastructure/discussions).

## Performance measurements

The following historical measurements use the cached pagination API in [today-blog](https://github.com/TAKETODAY/today-blog). They describe a particular application workload, not the capacity of arbitrary endpoints or a comparison with Spring Boot or other frameworks.

| Condition | Value |
|-----------|-------|
| Hardware | Mac mini M4 Pro, 48 GB RAM |
| Java | Java 25 |
| Endpoint | `/api/articles?size=1`, with caching |
| Address | `127.0.0.1:8080`, local loopback |
| Load generator | `wrk` |

### 10 concurrent connections

Two load-generation threads, 10 connections, 30 seconds:

| Metric | Recorded value |
|--------|----------------|
| Average latency | 88.39 µs |
| P99 latency | 136 µs |
| Throughput | 116,481 requests/second |

P99 is the 99th-percentile latency; it does not establish P99.9 or a bound for all requests.

### 5000 concurrent connections

Twenty load-generation threads, 5000 connections, one minute:

| Mode | Requests/second | Average latency | P99 latency |
|------|-----------------|-----------------|-------------|
| JVM | 155,293 | 1.44 ms | ~5 ms |
| Native Image | 145,599 | 1.57 ms | ~6 ms |

Connections are a load-generator setting, not a count of online users. Errors, timeouts and non-successful HTTP responses must be evaluated from the complete `wrk` output; they are not included in these tables.

These records do not include application/framework commit IDs, full JVM or Native Image parameters, warm-up details, cache hit rates, response sizes or variation across repeated runs. Treat the numbers as reference measurements. Loopback tests do not include the network path of remote clients, and the observed JVM/Native Image difference does not establish a general ranking. Startup time and memory usage require separate measurements.

### Measurement entry point

Prepare the blog's configuration, database, initial data and other dependencies, install `wrk`, and verify the endpoint before measuring.

```bash
git clone https://github.com/TAKETODAY/today-blog
cd today-blog
./gradlew :blog-api:infraRun
```

In another terminal:

```bash
wrk -t2 -c10 -d30s --latency 'http://127.0.0.1:8080/api/articles?size=1'
wrk -t20 -c5000 -d1m --latency 'http://127.0.0.1:8080/api/articles?size=1'
```

For comparable results, fix versions, datasets and cache conditions; record warm-up, client/server placement, runtime versions, launch parameters and full output. Repeat measurements and report variation and error statistics. Native Image measurements additionally require the GraalVM version, build parameters and executable launch command; the Gradle command above starts JVM mode.

## Background

The project started with a personal blog built using Java Servlets. XML configuration and later annotations helped remove repetitive code. Studying dependency injection led to experiments with a small IoC container and, eventually, deeper study of Spring source code.

The work evolved from implementing features independently to porting, adapting and maintaining upstream implementations within an existing framework. That process involved integrating modules, improving tests and exploring design decisions. The framework continues to evolve according to practical application needs.

For the author's detailed account and historical source-size snapshot, see the [Chinese README](README.md#主要目的).

## License and acknowledgments

TODAY Infrastructure is licensed under the [Apache License 2.0](LICENSE).

The project benefits from the work of:

- [Spring Framework](https://github.com/spring-projects/spring-framework)
- [Spring Boot](https://github.com/spring-projects/spring-boot)
- [ASM](https://asm.ow2.io/)
- [JetBrains](https://www.jetbrains.com/), for providing an open-source IDE license.

Third-party source retains its original copyright and license notices. Spring Framework and Spring Boot code is used under Apache License 2.0; ASM code is used under BSD-3-Clause. See [NOTICE](NOTICE) for details.

If the project is useful to you, consider starring it, sharing feedback or contributing a pull request.
