# TODAY Infrastructure

简体中文 | [English](README.en.md)

![Logo](./logo.svg) A modular Java application framework with dependency injection, auto-configuration, Web MVC, Netty and DATA Access support.

![Java17](https://img.shields.io/badge/JDK-17+-success.svg)
[![Apache License 2.0](https://img.shields.io/github/license/TAKETODAY/today-infrastructure?color=blue)](./LICENSE)
[![Deploy](https://github.com/TAKETODAY/today-infrastructure/actions/workflows/deploy-snapshots.yml/badge.svg)](https://github.com/TAKETODAY/today-infrastructure/actions/workflows/deploy-snapshots.yml)
[![Codecov](https://github.com/TAKETODAY/today-infrastructure/actions/workflows/codecov.yaml/badge.svg)](https://github.com/TAKETODAY/today-infrastructure/actions/workflows/codecov.yaml)
[![Coveralls](https://github.com/TAKETODAY/today-infrastructure/actions/workflows/coveralls.yaml/badge.svg)](https://github.com/TAKETODAY/today-infrastructure/actions/workflows/coveralls.yaml)
[![CI](https://github.com/TAKETODAY/today-infrastructure/actions/workflows/ci.yaml/badge.svg)](https://github.com/TAKETODAY/today-infrastructure/actions/workflows/ci.yaml)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/3ad5eed64065496fba9244d149820f67)](https://app.codacy.com/gh/TAKETODAY/today-infrastructure/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Coverage Status](https://codecov.io/gh/TAKETODAY/today-infrastructure/branch/master/graph/badge.svg?token=OUMKSYNTDC)](https://codecov.io/gh/TAKETODAY/today-infrastructure)
[![Coverage Status](https://coveralls.io/repos/github/TAKETODAY/today-infrastructure/badge.svg)](https://coveralls.io/github/TAKETODAY/today-infrastructure)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2FTAKETODAY%2Ftoday-infrastructure.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2FTAKETODAY%2Ftoday-infrastructure?ref=badge_shield)

[快速开始](#快速开始) · [安装配置](#installation) · [真实应用](#today-blog) · [在线文档](https://docs.today-tech.cn/today-infrastructure/) · [示例工程](infra-samples) · [贡献指南](CONTRIBUTING.md) · [交流讨论](https://github.com/TAKETODAY/today-infrastructure/discussions)

## 概述

该项目起源于 2017 年。主要用于构建高性能 Web 应用程序和普通应用程序的 Java 库。

## 适合谁

这个项目特别适合：

- 想深入理解 Spring、IoC、AOP、Bean 生命周期和 Web MVC 的 Java 开发者；
- 对 Java 框架设计、源码研究、字节码、动态代理和模块边界感兴趣的人；
- 希望研究 Netty 与应用框架如何结合的开发者；
- 想尝试轻量、可读、可修改的应用基础设施的小型项目或个人开发者；
- 愿意通过文档、测试、示例和设计讨论参与开源项目的人。

如果你的首要需求是成熟生态、广泛的第三方 Starter、商业支持或现成的企业级集成，Spring Boot 仍然是更稳妥的选择。


## 能做什么

主要能力包括：

| 领域         | 能力                                                                         |
|--------------|------------------------------------------------------------------------------|
| 应用启动     | `@InfraApplication`、`Application`、`ApplicationBuilder`、环境和应用生命周期 |
| IoC 容器     | Bean 定义、依赖注入、组件扫描、配置类、事件和生命周期回调                    |
| AOP          | Advisor、代理创建、方法拦截和自动代理基础设施                                |
| Web          | MVC 请求映射、参数绑定、异常处理、响应写入、WebSocket                        |
| HTTP 服务    | Netty Web Server、Web 抽象、HTTP 客户端和声明式 HTTP 服务                    |
| 数据访问     | JDBC、事务、持久化、数据库测试支持                                           |
| 序列化与集成 | Jackson、Gson、JSON-B、Protobuf、模板和邮件等可选模块                        |
| 工程工具     | BOM、Starter、Gradle 插件、配置处理器、测试模块和应用打包支持                |

模块是可选的，可以按应用需要选择 `infra-starter-*`，也可以直接使用更底层的 `infra-*` 模块。


## 和 Spring 的关系

TODAY Infrastructure 与 Spring 的关系可以概括为：**受其设计启发，研究其实现，并结合自身工程体系进行适配和演进。**

- 一些模块的设计思想和部分实现来自对 Spring Framework、Spring Boot 源码的学习与重构；
- 项目使用独立的包命名空间、模块坐标、应用启动模型和构建工具；
- 它不是 Spring Framework 或 Spring Boot 的二进制兼容替代品，Spring 生态组件不能因此默认直接兼容；
- 涉及第三方源码的文件保留原始版权和许可证声明，并在修改后追加 TODAY authors 的修改声明；
- 对 Spring 和 Spring Boot 的感谢及许可证说明见 [NOTICE](NOTICE)。

因此，本项目最适合被看作一个独立的 Java 应用框架实践和基础设施项目，而不是“另一个 Spring Boot 发行版”。


<a id="today-blog"></a>

## 真实应用：TODAY BLOG

[TODAY BLOG](https://github.com/TAKETODAY/today-blog) 是作者使用 TODAY Infrastructure 构建的开源博客应用，也是框架在真实业务中的实践案例。

| 项目     | 说明                                                                                                       |
|----------|------------------------------------------------------------------------------------------------------------|
| 在线网站 | [taketoday.cn](https://taketoday.cn)                                                                       |
| 应用源码 | [TAKETODAY/today-blog](https://github.com/TAKETODAY/today-blog)                                            |
| 后端     | Java + TODAY Infrastructure，源码见 [blog-api](https://github.com/TAKETODAY/today-blog/tree/main/blog-api) |
| 前端     | PC 端使用 React，移动端使用 Vue，后台管理使用 React + Ant Design                                           |

可以通过该项目了解框架如何用于业务工程：从后端依赖和应用配置，到接口实现及前后端协作。
项目介绍见 [today-blog README](https://github.com/TAKETODAY/today-blog#readme)，本地运行前请结合源码准备所需配置和外部依赖。

## 主要目的

主要为了学习技术，顺便给自己的博客网站 https://taketoday.cn
提供基础框架（其实写的博客网站也是为了学习练习技术）。博客也开源了：https://github.com/TAKETODAY/today-blog

<details>
  <summary>点击查看背景</summary>

## 背景

起源于大学的时候学习编程，后来用 Java Servlet 做了一个博客网站。在开发过程中发现有很多重复代码，
我觉得这样的代码很不优雅，尽管那个时候刚学编程不久，于是在我学习 [Struts2](https://struts.apache.org/) 的时候自己尝试着写了一个类似的
通过 `XML` 配置干掉了大量的重复代码的程序。于是初代的 [infra-web](https://gitee.com/I-TAKE-TODAY/today-web/tree/v1.1.1/) 诞生并开源。

后面学习了 `Java 注解` 又实现了通过注解配置的版本 [today-web 注解版](https://gitee.com/I-TAKE-TODAY/today-web/tree/2.1.x/)

[today-web 注解版](https://gitee.com/I-TAKE-TODAY/today-web/tree/2.1.x/) 刚出来时也正在学 `Spring` 感觉没有 `IoC`
容器感觉不是很方便。在网上看到很多自己写 什么 Mini Spring 之类，所以我大胆决定`我也要写一个`。有这个决心是因为我把 today-web 都写出来了，
再写个 IoC 应该不难吧。刚开始参考了各种 mini-spring，该说不说现在看来正是那个时候参考了他们的代码才导致我有些认知错误。在2021年6月-2021年12月期间
又去深入看 Spring 源码才纠正过来。事实证明写一个这样的东西确实不难，只是要优雅的话还是要点东西的。我自认为我的代码还是优雅的。不信？
[我的B站直播间欢迎你](https://live.bilibili.com/22702726) 。（在2021年开始直播写这个库，后面工作比较忙了就没怎么直播，后面有时间就会直播）。

刚开始写的时候（大概是2018年,我也是看的Git提交记录哈哈）有点无从下手，所以我去参考上文说到的各类 `Mini Spring`。 就这样又开启了一轮学习。
学习如何扫描类文件、学习Java注解、Java字节码、动态代理、重新认识接口、一些设计模式、学习使用Git、渐渐明白了单元测试的重要性
等。随着学习的深入框架经历了数次重构，自己也对依赖注入有了自己的看法。慢慢的我发现我居然能看得明白 Spring 源码了。 感觉Spring真心强大。

如果你问我怎么学习编程，我觉得造轮子是比较好的方式。自己还有很多要学的东西。比如分布式方面的知识，所以今后你定会看到诸如
[today-rpc](https://github.com/TAKETODAY/today-rpc), `today-distributed-*` 等项目诞生。

## 现状

学习维护一个项目，这是一个长久的过程。

在 2021 年随着学习的深入我觉得 Spring 功能强大，设计良好，自己虽然也能设计一些类似的功能，但是始终觉得靠我一个人的力量是不能设计
这么多完善的，于是我打算换种方式学习：移植。再转向到了现在这种学习方式：维护（通过维护学习新东西）。移植也不是一个简单的过程，不是单纯的
复制粘贴，我要结合之前的代码把 Spring 的代码适配到我原有的工程体系中，加之之前我的很多模块都是单独的 Git 仓库，我还要将他们合并到一个仓库。

我是一个模块一个模块的开始，一个模块我会学到很多东西。
这个过程持续了 2 年多（可以从 Git 提交记录看到）。后续的 Spring 主分支更新我也一直在同步（BUG 修复，新功能）。
在这期间我发现了 Spring 的一些问题，针对其中的一部分我还给 [Spring](https://github.com/spring-projects/spring-framework) 提交过多次优化改进的 PR。

这个库目前只有我在自己的生产环境使用，移植的模块是根据我自己的一些项目使用情况来的，例如没有移植 JMS, R2dbc, Web Flux。
有些是我觉得没必要封装有的功能是我还没使用上。

```text
截止: f72a88a9e72f86a28b96a4e46dc684a20ad8762f

cloc ./

   11926 text files.
   11400 unique files.                                          
     958 files ignored.

github.com/AlDanial/cloc v 2.04  T=27.43 s (415.6 files/s, 61331.5 lines/s)
----------------------------------------------------------------------------------------
Language                              files          blank        comment           code
----------------------------------------------------------------------------------------
Java                                   9570         221822         455007         889459
AsciiDoc                                355          11685            844          39546
XML                                     675           3005           3541          20370
Text                                     68           1858              0           7997
Gradle                                  233           1050            357           5169
XSD                                      17            277            223           4118
SQL                                      54             49             52           2227
JSON                                     34              1              0           2219
HTML                                     34            400            210           1999
YAML                                     61            102             14            985
Properties                              210             93            560            897
Markdown                                  4            232              4            894
SVG                                       2              2             17            617
DTD                                       3            156            548            521
Bourne Shell                              3             84            354            311
AspectJ                                   9            102            449            266
Bourne Again Shell                        1             37             58            229
diff                                      5             47            176            221
DOS Batch                                 3             63              6            212
Groovy                                   16             54              4            126
Maven                                     5             17             35            121
XSLT                                      8             23              1             94
Freemarker Template                       5              4              4             37
JSP                                       4              3              0             35
CSS                                       6              3              0             15
JavaScript                                7              1              0             12
Protocol Buffers                          1              2              0             11
INI                                       1              5              0              8
Mustache                                  1              0              0              8
Python                                    1              1              0              4
Velocity Template Language                4              0              0              4
----------------------------------------------------------------------------------------
SUM:                                  11400         241178         462464         978732
----------------------------------------------------------------------------------------

```

</details>

<a id="installation"></a>

## 🛠️ 安装

### Gradle

推荐方式：[buildSrc/build.gradle](infra-samples/buildSrc/build.gradle)

```groovy
plugins {
  id 'java-gradle-plugin'
}

repositories {
  mavenLocal()
  maven { url = "https://central.sonatype.com/repository/maven-snapshots/" }
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation(platform("cn.taketoday:infra-bom:$infraVersion"))
  implementation "cn.taketoday:infra-gradle-plugin"
  
}
```

以上配置在 IDEA 里有代码提示。下面的配置则没有。

一般项目：[settings.gradle](./infra-samples/settings.gradle)

```groovy
buildscript {
  repositories {
    mavenLocal()
    maven { url = "https://central.sonatype.com/repository/maven-snapshots/" }
    mavenCentral()
  }

  dependencies {
    classpath "cn.taketoday:infra-gradle-plugin:$infraVersion"
  }
}

```

以上两种配置用一种即可。

子模块 [build.gradle](./infra-samples/build.gradle)

使用了上面的任何一种配置之后子模块就可以使用 `apply plugin: 'infra.application'` 来引入 Gradle 插件

```groovy
apply plugin: "java"
apply plugin: 'infra.application'

repositories {
  mavenLocal()
  maven { url "https://central.sonatype.com/repository/maven-snapshots/" }
  maven { url "https://maven.aliyun.com/repository/public" }
  mavenCentral()
}

dependencies {
  implementation 'cn.taketoday:infra-starter-netty-server'
  implementation 'cn.taketoday:infra-starter-webmvc'
}

```

具体工程可以参见 [infra-samples](./infra-samples)


## 快速开始

### 直接运行示例

首次体验可以直接运行 [infra-samples](infra-samples)，无需手动创建工程或配置依赖。
准备好 Git 和 JDK，首次运行时 Gradle 会下载所需依赖。

```bash
git clone https://github.com/TAKETODAY/today-infrastructure.git
cd today-infrastructure/infra-samples
./gradlew infraRun --args='--server.port=8080'
```

Windows 下使用 `gradlew.bat` 替代 `./gradlew`。
示例使用的框架版本从仓库根目录的 `gradle.properties` 读取，依赖通过配置的 Maven 仓库解析。

服务启动后，在另一个终端执行：

```bash
curl http://localhost:8080/index
```

预期响应：

```text
Hello
```

应用入口见 [NettyDemoApplication.java](infra-samples/netty-app/src/main/java/infra/samples/NettyDemoApplication.java)，
接口实现见 [DemoController.java](infra-samples/web-api/src/main/java/infra/samples/DemoController.java)。
可以修改控制器代码，停止服务后重新运行上述命令，观察响应变化。

### 创建自己的应用

下面创建一个最小的 HTTP 接口，完成「编写应用 → 启动服务 → 验证响应」。
请先按照上面的 [安装](#installation) 部分配置依赖；本示例使用 `infra-starter-webmvc` 和
`infra-starter-netty-server`，运行环境为 JDK 17+。

#### 1. 创建应用

在 `src/main/java/example/DemoApplication.java` 中添加以下代码：

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

- `@InfraApplication`：声明应用配置入口，启用组件扫描和自动配置。
- `@RestController`：声明返回响应内容的控制器。
- `@GET("/hello")`：将 GET 请求映射到 `hello()` 方法。
- `Application.run(...)`：启动应用上下文和配置的 Web 服务。

入门示例将入口和控制器放在同一个类中。实际项目可以拆分为独立的控制器，
放在应用入口所在包或其子包中，便于组件扫描。

#### 2. 启动服务

在你的应用工程根目录执行以下命令（需要已配置 Gradle Wrapper 和 `infra.application` 插件）：

```bash
./gradlew infraRun --args='--server.port=8080'
```

Windows 下使用 `gradlew.bat` 替代 `./gradlew`。
也可以在 IDE 中直接运行 `DemoApplication.main()`，并将 `--server.port=8080` 设置为程序参数。

#### 3. 验证接口

服务启动后，在另一个终端执行：

```bash
curl http://localhost:8080/hello
```

预期响应：

```text
Hello, TODAY!
```

也可以直接在浏览器中访问 <http://localhost:8080/hello>。
如果 8080 端口被占用，可修改启动参数中的 `server.port`，并使用对应端口访问。

### 继续探索

跑通第一个接口后，可以通过以下示例了解更多功能：

| 内容                               | 示例入口                                                     |
|------------------------------------|--------------------------------------------------------------|
| 请求参数、响应对象、SSE 等 Web API | [web-api](infra-samples/web-api)                             |
| Netty 应用启动与工程配置           | [netty-app](infra-samples/netty-app)                         |
| 完整业务应用与框架使用实践         | [真实应用：TODAY BLOG](#today-blog)                          |
| 模块用法与 API 文档                | [在线文档](https://docs.today-tech.cn/today-infrastructure)  |

## 参与项目

欢迎通过以下方式参与：

- 使用示例并报告可复现的问题；
- 改进快速开始、模块文档和英文翻译；
- 增加测试和真实场景示例；
- 复现性能测试并补充环境信息；
- 讨论 Web 和模块设计；
- 提交 Bug 修复或功能改进。

开始贡献前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。建议先查看带有 [`good first issue`](https://github.com/TAKETODAY/today-infrastructure/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) 标签的 Issue。


## ⚡ 性能表现

以下为基于 [today-blog](https://github.com/TAKETODAY/today-blog) 的带缓存分页查询 API 的历史压测记录，使用 `wrk` 测量吞吐量和请求延迟。

| 项目     | 测试条件                                 |
|----------|------------------------------------------|
| 硬件     | Mac mini M4 Pro，48GB 内存               |
| Java     | Java 25                                  |
| 接口     | `/api/articles?size=1`，带缓存的分页查询 |
| 请求地址 | `127.0.0.1:8080`，本机回环地址           |
| 工具     | `wrk`                                    |

这些数据反映该应用在特定环境和工作负载下的表现，不代表任意业务接口的处理能力，也不构成与 Spring Boot 或其他框架的性能对比。

### 10 个并发连接

使用 2 个压测线程、10 个并发连接，持续测试 30 秒，记录如下：

| 指标             | 数值                           |
|:-----------------|:-------------------------------|
| **平均延迟**     | **88.39 微秒 (µs)**            |
| **P99 延迟**     | **136 微秒 (µs)**              |
| **吞吐量 (QPS)** | **116,481**                    |
| **测试命令**     | `wrk -t2 -c10 -d30s --latency` |

本次记录的平均延迟为 88.39 µs，P99 为 136 µs。P99 表示第 99 百分位延迟，不能据此推导 P99.9 或所有请求的延迟。

### 5000 个并发连接

使用 20 个压测线程、5000 个并发连接，持续测试 1 分钟，JVM 和 Native Image 的记录如下：

| 运行模式         | QPS (吞吐量) | 平均延迟 | P99 延迟 | 测试参数           |
|:-----------------|:-------------|:---------|:---------|:-------------------|
| **传统 JVM**     | **155,293**  | 1.44 ms  | ~5 ms    | `-t20 -c5000 -d1m` |
| **Native Image** | **145,599**  | 1.57 ms  | ~6 ms    | `-t20 -c5000 -d1m` |

并发连接数是 `wrk` 的负载配置，不等同于在线用户数。连接错误、超时和非成功 HTTP 响应应结合完整的 `wrk` 输出单独统计；上表未列出这些数据。

### 性能解读

- **吞吐量**：在上述 5000 连接测试中，JVM 记录为 155,293 QPS，Native Image 记录为 145,599 QPS；实际业务容量还取决于数据访问、缓存、响应大小和部署环境。
- **延迟**：低连接数和高连接数测试采用不同负载配置，应分别解读；本机回环测试也不包含真实客户端到服务端的网络路径。
- **运行模式**：这次记录中 JVM 的吞吐量高于 Native Image，但单次记录不足以判断两种模式在其他工作负载下的优劣。
- **启动与内存**：本节仅展示吞吐量和请求延迟。启动耗时和内存占用需要另行记录构建配置、测量方法及结果。

历史记录尚未列出应用和框架的提交版本、完整 JVM/Native Image 参数、预热过程、缓存命中率、响应大小及多轮测试的波动范围。因此，这些数字适合作为参考记录，精确复现和横向比较仍需补齐上述条件。

### 如何复现测试？

以下命令是 JVM 模式的压测入口。运行前需根据 [today-blog](https://github.com/TAKETODAY/today-blog) 的代码和配置准备数据库、初始化数据及其他依赖，安装 `wrk`，并确认接口正常返回。

```bash
# 克隆示例项目
git clone https://github.com/TAKETODAY/today-blog
cd today-blog

# 启动应用（JVM 模式）
./gradlew :blog-api:infraRun

# 低并发延迟测试
wrk -t2 -c10 -d30s --latency 'http://127.0.0.1:8080/api/articles?size=1'

# 高并发吞吐测试
wrk -t20 -c5000 -d1m --latency 'http://127.0.0.1:8080/api/articles?size=1'
```

为了得到可比较的结果，请固定应用和框架版本、数据集及缓存状态，说明是否预热以及压测客户端是否与服务端共用机器，并记录 JDK/Gradle 版本、启动参数和完整 `wrk` 输出。重复多轮测试，报告吞吐量、延迟分位数、错误统计及结果波动。

Native Image 测试还需记录 GraalVM 版本、构建参数和可执行文件的启动命令；上面的 Gradle 命令仅用于 JVM 模式。

## 🙏 鸣谢

本项目的诞生离不开以下项目：

* [Spring Framework](https://github.com/spring-projects/spring-framework)
* [Spring Boot](https://github.com/spring-projects/spring-boot)
* [ASM Bytecode Manipulation Framework](https://asm.ow2.io/)
* [JetBrains](https://www.jetbrains.com/?from=https://github.com/TAKETODAY/today-infrastructure): 感谢 JetBrains 提供免费 IDE 开源授权

## 📄 开源协议

本项目使用 [Apache License 2.0](LICENSE) 开源。

* **Spring Framework & Spring Boot**: 项目部分模块的设计思想与代码实现，源于对 Spring 生态的深度研究与现代化重构。我们严格遵守其 Apache License
  2.0许可证，并在源码文件中保留了所有原始版权声明。
* **ASM Bytecode Manipulation Framework**: 用于核心的字节码操作，遵循其BSD-3-Clause许可证。

由衷感谢上述团队和社区的开创性工作。本项目的所有修改与创新部分，均在上述原始许可证的条款下开源。

涉及第三方源码时，项目遵守其原始许可证并保留版权声明。详细信息请参阅 [NOTICE](NOTICE)。

如果这个项目对你有帮助，欢迎 Star、提出 Issue、参与讨论或提交 Pull Request。

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2FTAKETODAY%2Ftoday-infrastructure.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2FTAKETODAY%2Ftoday-infrastructure?ref=badge_large)
