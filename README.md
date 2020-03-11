# TODAY Context

🍎 A Java library for dependency injection and aspect oriented programing

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3ad5eed64065496fba9244d149820f67)](https://www.codacy.com/app/TAKETODAY/today-context?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=TAKETODAY/today-context&amp;utm_campaign=Badge_Grade)
![Java CI](https://github.com/TAKETODAY/today-context/workflows/Java%20CI/badge.svg)


## 🛠️ 安装

```xml
<dependency>
    <groupId>cn.taketoday</groupId>
    <artifactId>today-context</artifactId>
    <version>2.1.6.RELEASE</version>
</dependency>
```
- [Maven Central](https://search.maven.org/artifact/cn.taketoday/today-context/2.1.6.RELEASE/jar)

## 🎉 前言

today-web 框架2.0刚出来时没有 ioc 容器感觉不是很方便，所以想自己实现一个。之前有看过Spring源码但是发现我对Spring源码无从下手😰完全懵逼。之前学过怎么用Spring但是对他的底层完全不了解的我带着试一试的心态开始到处查资料，就这样我又开始造起了轮子。**如何扫描类文件**、**学习Java注解**、**Java字节码**、**动态代理**、**重新认识接口**、**一些设计模式**、**学习使用Git**、**渐渐明白了单元测试的重要性** 等。随着学习的深入框架经历了数次重构，自己也对依赖注入有了自己的看法。慢慢的我发现我居然能看得明白Spring源码了。感觉Spring真心强大😮👍 。如果他说他是轻量级，那我的就是超轻量级😄 。自己在造轮子的过程中学习到了很多知识，越学感觉自己越空，觉得Java是越学越多，永远都学不完。


## 📝 使用说明

### 标识一个Bean
- 使用`@Component`
- 任意注解只要注解上有`@Component`注解就会标识为一个Bean不论多少层

```java
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Component {
    /** @return bean name */
    String[] value() default {};

    /** @return bean's scope */
    Scope scope() default Scope.SINGLETON;

    String[] initMethods() default {};

    String[] destroyMethods() default {};

}
```

`@Singleton` 

```java
@Component(scope = Scope.SINGLETON)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Singleton {

    // bean name
    String[] value() default {};

    String[] initMethods() default {};

    String[] destroyMethods() default {};
}

```

`@Prototype`
```java
@Retention(RetentionPolicy.RUNTIME)
@Component(scope = Scope.PROTOTYPE)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Prototype {

    // bean name
    String[] value() default {};

    String[] initMethods() default {};

    String[] destroyMethods() default {};
}
```

`@Configuration`
```java
@Target(ElementType.TYPE)
@Component(scope = Scope.SINGLETON)
public @interface Configuration {

}
```
`@Service`
```java
@Component(scope = Scope.SINGLETON)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Service {

    String[] value() default {};// bean names
}
```

### 注入Bean
- 使用`@Autowired`注入
- 使用`@Resource`注入
- 使用`@Inject`注入
- 可自定义注解和实现`PropertyValueResolver`：

```java
@FunctionalInterface
public interface PropertyValueResolver {

    default boolean supports(Field field) {
        return false;
    }
    PropertyValue resolveProperty(Field field) throws ContextException;
}
```

