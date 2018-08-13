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


   
