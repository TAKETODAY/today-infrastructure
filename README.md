# TODAY Infrastructure

![Logo](./logo.svg) A Java library for applications software infrastructure.

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

## 概述

该项目起源于 2017 年。主要用于构建高性能 Web 应用程序和普通应用程序的 Java 库。

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

这个库目前只有我在自己的生产环境使用，移植的模块是根据我自己的一些项目使用情况来的，例如没有移植 JMS, Messaging, R2dbc, Web Flux。
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

### Maven

```xml
<dependency>
  <groupId>cn.taketoday</groupId>
  <artifactId>infra-starter-webmvc</artifactId>
  <version>${infraVersion}</version>
</dependency>

<dependency>
  <groupId>cn.taketoday</groupId>
  <artifactId>infra-starter-netty-server</artifactId>
  <version>${infraVersion}</version>
</dependency>
```

## 开始

```java
@Slf4j
@RestController
@InfraApplication
public class DemoApplication {

  public static void main(String[] args) {
    Application.run(DemoApplication.class, args);
  }

  @GET("/index")
  public String index() {
    return "Hello";
  }

  @GET("/body/{name}/{age}")
  public Body body(String name, int age) {
    return new Body(name, age);
  }

  @GET("/publish-event")
  public void index(String name, @Autowired ApplicationEventPublisher publisher) {
    publisher.publishEvent(new MyEvent(name));
  }

  @GET("/request-context")
  public String context(RequestContext context) {
    String requestURL = context.getRequestURL();
    String queryString = context.getQueryString();
    System.out.println(requestURL);
    System.out.println(queryString);

    return queryString;
  }

  record Body(String name, int age) {

  }

  @Configuration
  static class AppConfig {

    @EventListener(MyEvent.class)
    public void event(MyEvent event) {
      log.info("event :{}", event);
    }
  }

  record MyEvent(String name) {
    
  }

  @ExceptionHandler(Throwable.class)
  public void throwable(Throwable throwable) {
    throwable.printStackTrace();
  }

}
```

## ⚡ 性能表现

TODAY 框架在真实业务场景（带缓存的分页查询API）下，使用 `wrk` 工具进行了全面的压力测试。测试环境：Mac mini M4 Pro，48GB 内存，Java 25。

### 低并发 · 极致响应

模拟常规流量（10个并发连接），框架展现出**微秒级**的响应能力：

| 指标 | 数值 |
|:---|:---|
| **平均延迟** | **88.39 微秒 (µs)** |
| **P99 延迟** | **136 微秒 (µs)** |
| **吞吐量 (QPS)** | **116,481** |
| **测试命令** | `wrk -t2 -c10 -d30s --latency` |

> 99.9% 的请求在 0.2 毫秒内完成，无任何连接错误。

### 高并发 · 超高吞吐

模拟极端流量（5000个并发连接），框架依然保持稳定：

| 运行模式 | QPS (吞吐量) | 平均延迟 | P99 延迟 | 测试参数 |
|:---|:---|:---|:---|:---|
| **传统 JVM** | **155,293** | 1.44 ms | ~5 ms | `-t20 -c5000 -d1m` |
| **Native Image** | **145,599** | 1.57 ms | ~6 ms | `-t20 -c5000 -d1m` |

> 在 5000 并发、百万级请求的压力下，平均延迟仍控制在 1.5 毫秒左右，错误率极低（<0.1%）。

### 性能解读

- **低延迟极限**：在常规负载下，请求可在 **<0.1 毫秒** 内完成，适合延迟敏感型应用（如游戏、实时通信）。
- **高吞吐能力**：单节点可支撑 **15万+ QPS**，轻松应对流量洪峰。
- **双模式可选**：
    - **JVM 模式**：峰值性能略高，适合长期运行的服务。
    - **Native Image 模式**：启动时间 **< 0.1 秒**，内存占用 **< 50 MB**，同时仍保持 **14.6万 QPS** 的强劲性能，是 Serverless、边缘计算、容器化部署的理想选择。

### 如何复现测试？

```bash
# 克隆示例项目
git clone https://github.com/TAKETODAY/today-blog
cd today-blog

# 启动应用（JVM 模式）
./gradlew :blog-api:infraRun

# 低并发延迟测试
wrk -t2 -c10 -d30s --latency http://127.0.0.1:8080/api/articles?size=1

# 高并发吞吐测试
wrk -t20 -c5000 -d1m http://127.0.0.1:8080/api/articles?size=1
```

## 🙏 鸣谢

本项目的诞生离不开以下项目：

* [Spring Framework](https://github.com/spring-projects/spring-framework)
* [Spring Boot](https://github.com/spring-projects/spring-boot)
* [ASM Bytecode Manipulation Framework](https://asm.ow2.io/)
* [JetBrains](https://www.jetbrains.com/?from=https://github.com/TAKETODAY/today-infrastructure): 感谢 JetBrains 提供免费开源授权

## 📄 开源协议

使用 [Apache License 2.0](https://github.com/TAKETODAY/today-infrastructure/blob/master/LICENSE) 开源协议

* **Spring Framework & Spring Boot**: 项目部分模块的设计思想与代码实现，源于对 Spring 生态的深度研究与现代化重构。我们严格遵守其 Apache License
  2.0许可证，并在源码文件中保留了所有原始版权声明。
* **ASM Bytecode Manipulation Framework**: 用于核心的字节码操作，遵循其BSD-3-Clause许可证。

由衷感谢上述团队和社区的开创性工作。本项目的所有修改与创新部分，均在上述原始许可证的条款下开源。

详细的法律声明，请参阅根目录的 [NOTICE](NOTICE) 文件。


如果这个项目对你有帮助，请给我们一个 ⭐️！你的认可是我们持续前进的动力。


[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2FTAKETODAY%2Ftoday-infrastructure.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2FTAKETODAY%2Ftoday-infrastructure?ref=badge_large)

