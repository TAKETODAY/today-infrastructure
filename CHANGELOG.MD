# Today IOC Framework

### Today IOC Framework 是作者学习 Spring IOC 以及自己对IOC的理解之作

> ## v1.0.3

    1. 增加 FactoryBean 功能
    2. 增加 BeanFactoryAware
    3. 增加 BeanClassLoaderAware
    4. 增加 PropertyResolver 注解绑定依赖注入处理器
    5. 增加 Props 注解注入 直接注入Properties
    6. 优化依赖注入流程
    7. 加入 BeanPostProcessor
    8. 加入 DisposableBean
    9. 加入 InitializingBean

> 4:
```java
public @interface PropertyResolver {

    Class<? extends Annotation> value() default Autowired.class;>
 }
 ```
> 5:
```java
public @interface Props {

    /**
     * @return properties file name
     */
    String[] value() default {};

    /**
     * prefix of the key <br>
     * default ""
     *
     * @return
     */
    String[] prefix() default { "" };
}

```

> ## v1.1.1
   
- 修复单例情况下接口对象与实现类对象不一致问题
- 添加部分doc
- 添加refresh方法
- 添加test code
- Properties 注入时可以选择替换掉前缀，前缀有时是为了区分一类配置
- 优化注入可以自定义注解注入
   
> ## v1.2.0

- 添加 AnntationApplicationContext 支持 `Configuration` 注解
- `ClassPathApplicationContext` 改为 `DefaultApplicationContext`
- bean 注解支持方法标注

> ## v1.2.1

- 修复注入原型错误   


> ## 2.0.0

- 加入`ObjectFactory`
- 框架重构 
