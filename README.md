# TODAY Infrastructure

🍎 A Java library for applications software infrastructure.

![Java17](https://img.shields.io/badge/JDK-17+-success.svg)
[![GPLv3](https://img.shields.io/badge/License-GPLv3-blue.svg)](./LICENSE)
[![Author](https://img.shields.io/badge/Author-TODAY-blue.svg)](https://github.com/TAKETODAY)
[![Snapshots Deployment Status](https://github.com/TAKETODAY/today-infrastructure/workflows/GitHub%20CI/badge.svg)](https://github.com/TAKETODAY/today-infrastructure/actions)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/3ad5eed64065496fba9244d149820f67)](https://www.codacy.com/gh/TAKETODAY/today-infrastructure/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=TAKETODAY/today-infrastructure&amp;utm_campaign=Badge_Grade)
[![Coverage Status](https://coveralls.io/repos/github/TAKETODAY/today-infrastructure/badge.svg)](https://coveralls.io/github/TAKETODAY/today-infrastructure)

**You ask me what the elegant code looks like? Then I have to show it!**

## 主要目的

主要为了学习技术，顺便给自己的博客网站 https://taketoday.cn 提供基础框架（其实写的博客网站也是为了学习练习技术）。

## 背景

起源于大学的时候学习编程，后来用 Java Servlet 做了一个博客网站。在开发过程中发现有很多重复代码，
我觉得这样的代码很不优雅，尽管那个时候刚学编程不久，于是在我学习 [Struts2](https://struts.apache.org/) 的时候自己尝试着写了一个类似的
通过 `XML` 配置干掉了大量的重复代码的程序。于是初代的 [today-web](https://gitee.com/I-TAKE-TODAY/today-web/tree/v1.1.1/) 诞生并开源。

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

## 🛠️ 安装

```xml
<dependency>
  <groupId>cn.taketoday</groupId>
  <artifactId>today-framework</artifactId>
  <version>4.0.0-Draft.3-SNAPSHOT</version>
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
    String requestURL = context.requestURL();
    String queryString = context.queryString();
    System.out.println(requestURL);
    System.out.println(queryString);

    return queryString;
  }

  @Getter
  static class Body {
    final String name;
    final int age;

    Body(String name, int age) {
      this.name = name;
      this.age = age;
    }
  }

  @Configuration
  static class AppConfig {

    @EventListener(MyEvent.class)
    public void event(MyEvent event) {
      log.info("event :{}", event);
    }
  }

  @ToString
  static class MyEvent {
    final String name;

    MyEvent(String name) {
      this.name = name;
    }
  }

  @ExceptionHandler(Throwable.class)
  public void throwable(Throwable throwable) {
    throwable.printStackTrace();
  }

}
```

## 🙏 鸣谢

本项目的诞生离不开以下项目：

* [Spring](https://github.com/spring-projects/spring-framework): Spring Framework
* [Spring Boot](https://github.com/spring-projects/spring-boot): Spring Boot
* [Jetbrains](https://www.jetbrains.com/?from=https://github.com/TAKETODAY/today-infrastructure): 感谢 Jetbrains 提供免费开源授权

## 📄 开源协议

使用 [GPLv3](https://github.com/TAKETODAY/today-infrastructure/blob/master/LICENSE) 开源协议

