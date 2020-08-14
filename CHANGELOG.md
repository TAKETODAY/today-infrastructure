# TODAY Context CHANGE LOG

:apple:  today-context is a lightweight dependency injection framework.    

## TODAY Context 是我学习 Spring 以及自己对IOC的理解


[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3ad5eed64065496fba9244d149820f67)](https://www.codacy.com/app/TAKETODAY/today-context?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=TAKETODAY/today-context&amp;utm_campaign=Badge_Grade)

## 安装

```xml
<dependency>
    <groupId>cn.taketoday</groupId>
    <artifactId>today-context</artifactId>
    <version>3.0.0.RELEASE</version>
</dependency>
```

- [Maven Central](https://search.maven.org/artifact/cn.taketoday/today-context/2.1.7.RELEASE/jar)

# 当前版本

## v3.0.0（v2.1.7）

- :bug: fix: loadBeanDefinition can't be catch exception
- :bug: fix: ContextUtils#loadProps() only support String
- :bug: fix: #11 JarEntryResource#getJarFilePath(String) jar file not found 
- :sparkles: feat: 使用方法名作为默认实例名称
- :hammer: 重构FactoryBean注册逻辑
- :hammer: 重构BeanFactory获取Bean的逻辑
- :sparkles: 利用cglib构建了真正的原型实例
- :sparkles: 增加导入配置(@Import)功能

- :sparkles: 去除ClassUtils的classesCache，在应用环境下可能会使用到相同class
- :sparkles: ConfigurableBeanFactory添加registerBean(BeanDefinition)方法
- :sparkles: ContextUtils添加loadFromMetaInfo(String)统一加载META-INF下的类
- :zap: 重构DefaultBeanNameCreator提升性能
- :sparkles: 6.添加StandardBeanDefinition#mergeAnnotations()方法来合并注解
- :sparkles: StandardBeanFactory实现BeanDefinitionLoader接口作为默认bean加载器原本的默认加载器被丢弃不在使用
- :zap: 优化StandardEnvironment获取BeanNameCreator逻辑
- :zap: 优化AutowiredPropertyResolver不必要的局部变量
- :zap: 优化AbstractBeanFactory#containsBeanDefinition(Class,boolean)
- :sparkles: 添加ExecutableParameterResolver增强构造器注入或方法注入的扩展性
- :sparkles: 添加 Method Invoker
- :bug: 修复#13注解扫描不完全
- :sparkles: 添加日志包适配主流日志框架
- :sparkles: 添加ApplicationEventCapable接口,提高扩展性
- :sparkles: 添加BeanDefinition#setInitMethods(String[])
- :bug: 修复重大漏洞：el执行期间潜在的并发问题
- :bug: 修复bean实例重复创建
- :sparkles: 新增ComponentScan支持自定义扫描包
- :sparkles: 支持ApplicationContextSupport
- :sparkles: 支持任意事件类型
- :zap: 优化循环依赖问题
- :sparkles: 实现 DestructionBeanPostProcessor
- :hammer: 重构 使用CandidateComponentScanner加载类
- :sparkles: 新增PathMatchingResourcePatternResolver
- :sparkles: 添加ClassUtils#getQualifiedMethodName
- :zap: 优化AnnotationAttributes
- :zap: 优化ContextUtils
- :bug: 修复listener顺序问题
- :bug: 修复META-INFO/beans
- :bug: 修复bean实例重复创建
- :sparkles: 新增StringUtils#parseParameters()
- :zap: 优化Resource
- :zap: 优化toArray
- :zap: 优化AntPathMatcher
- :sparkles: 新增OrderedSupport
- :sparkles: 新增CandidateComponentScannerCapable
- :zap: 优化Resource
- :sparkles: 增加Assert,MultiValueMap
- :sparkles: 新增ImportAware
- :sparkles: 将aop整合进来

- :hammer: 重大重构: 大量代码优化，重构，更正测试代码
- :sparkles: 新增AbstractFactoryBean
- :sparkles: 新增Spring AttributeAccessor
- :sparkles: 新增BeanClassLoaderAware
- :hammer: 重构Condition
- :sparkles: 新增BeanFactoryPostProcessor

- :fire: 除去MessageFactory
- :sparkles: 新的反射API


- :bug: 修复ResourceUtils#getResource文件路径带有转义字符时的错误
- :zap: 优化AutoProxyCreator, CandidateComponentScanner, AbstractApplicationContext, PropertyValueResolver, ExecutableParameterResolver, StandardBeanFactory, AbstractCacheInterceptor, AspectsDestroyListener
- :zap: 新增Environment#getFlag
- :zap: 优化ContextUtils#resolveInitMethod
- :zap: 减少lombok依赖
- :zap: 优化加载过程
- :sparkles: 支持使用Autowired方法注入
- :zap: 优化RedissonCache
- :memo: 优化部分Javadoc
- :zap: 优化扫描日志
- :zap: 优化PathMatchingResourcePatternResolver
- :zap: 优化AbstractAdvice代码
- :zap: 优化cglib
- :zap: 优化Environment初始化顺序
- :sparkles: 新的反射API


## v2.1.6
- :sparkles: feat: add `Environment` new api to get property
- :sparkles: feat: add jar-prefix file to ignore jar scanning
- :bug: fix: ensure ExpressionFactory's instance consistent
- :bug: fix: DataSize.parse()
- :zap: add ConcurrentProperties 
- :bug: fix #3 when get annotated beans that StandardBeanDefinition missed
- :bug: fix: NumberUtils String[].class can't be resolve
- :sparkles: feat: full Prototype supports (only support interface)
- :sparkles: feat: full Prototype Lifecycle supports (destroy bean after every single call)
- :bug: fix: can not access a member
- :bug: fix: can't getMethodArgsNames
- :bug: fix: ELProcessor not process
- :sparkles: feat: lazy loading
- :bug: fix: no constructor
- unify date format
- :sparkles: feat: add Resource api
- :sparkles: feat: add ResourceFilter to filter Resource
- :bug: fix: #6 Properties not found
- :bug: fix: ConfigurationException detail message
- :sparkles: feat: add `TypeConverter`,`StringTypeConverter` api
- :wrench: move jar-prefix to META-INF/jar-prefix
- :sparkles: feat: Props nested class feature
- :bug: fix: #7 The bean is not initialized due to the startup sequence not found
- :sparkles: feat: add new annotations: ConditionalOnClass, ConditionalOnExpression,ConditionalOnMissingClass,ConditionalOnProperty,ConditionalOnResource
- :bug: fix: #8 Property inject failure
- :bug: fix: NumberUtils#toArrayObject()
- :bug: fix: @since 2.1.6 elManager my be null
- :sparkles: feat: @Value default value feature
- :sparkles: feat: add @Env
- :sparkles: feat: add META-INF/beans to avoid scan all jar file
- :sparkles: feat: Use static method ContextUtils#getApplicationContext() to get ApplicationContext
- :bug: fix: #9 Some listener in a jar can't be load
- :bug: fix: #10 classes loading from a jar can't be load
- :bookmark: release v2.1.6 2019/7/24-1:37

## v2.1.5
- :sparkles: feat: Add context `state` feature
- :bug: fix: `StandardEnvironment`.`addActiveProfile()` when add profile before load context it is not work
- :sparkles: feat: support `Constructor` injection
- adjust: Adjust context event
- :sparkles: feat: Enhance `Props`
- :sparkles: feat: Add el support
- :bug: fix: When manually load context some properties can't be loaded
- :bug: fix: el Messages resource bundle not found


## v2.1.4
- fix some singletons could not be initialized.

## v2.1.3
- fix missing @Props injection
- sync to maven central

## v2.1.2
- Use `BeanNameCreator` to create bean name
- `FactoryBean`
- :bug: fix: handleDependency(): when handle dependency some bean definition has already exist 
- :bug: fix: same name of bean when applyPropertyValues() cause exception
- :sparkles: feat: add initMethods feature
- :sparkles: feat: add destroyMethods feature
- :sparkles: feat: add `@MissingBean` feature

## v2.1.1
- :sparkles: feat: add the destroy bean feature
- :bug: fix: #1  some singletons could not be initialized
- discard @PropertyResolver

## v2.1.0
- fix: fix BeanPostProcessor's Dependency 
- :sparkles: feat: add asm 7.0 under cn.taketoday.asm
- [feat: add `@Order` Ordered feature](/src/test/java/test/context/listener)
- [feat: add `@Conditional` feature](/src/test/java/test/context/profile/ProfileTest.java)
- [feat: add `@Profile` feature](/src/test/java/test/context/profile/ProfileTest.java)
- [feat: add `Environment` feature](/src/test/java/test/context/env/StandardEnvironmentTest.java)
- refactor: New Understanding Of IOC and coding

> examples

```java
@Singleton
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProxyBeanPostProcessor implements BeanPostProcessor {

}
```
> or

```java
@ContextListener
public class BeanDefinitionLoadedListener_2 implements ApplicationListener<BeanDefinitionLoadedEvent>, Ordered {
    
    @Override
    public void onApplicationEvent(BeanDefinitionLoadedEvent event) {
        log.debug("BeanDefinitionLoadedListener_2");
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
```
> @Profile("test")
```java
@Configuration
public class ConfigurationBean {

    @Prototype
    public User user() {
        return new User().setId(12);
    }

    @Singleton("user__")
    public User user__() {
        return new User().setId(12);
    }

    @Profile("test")
    @Prototype("user")
    public User testUser() {
        return new User().setUserName("TEST");
    }

    @Profile("prod")
    @Singleton("user")
    public User prodUser() {
        return new User().setUserName("PROD");
    }
    
    @Singleton("user_")
    @Conditional(WindowsCondition.class)
    public User windowsUser() {
        return new User().setUserName("Windows");
    }
}

public class WindowsCondition implements Condition {
    @Override
    public boolean matches(ApplicationContext applicationContext, AnnotatedElement annotatedElement) {
        String system = applicationContext.getEnvironment().getProperty("os.name");
        if(system.contains("Windows")) {
            return true;
        }
        return false;
    }
}

@Test
public void test_Profile() {

    try (ApplicationContext applicationContext = new StandardApplicationContext(true)) {
        
        User user = applicationContext.getBean("user", User.class);
        System.out.println(user);
        assert "TEST".equals(user.getUserName());
    }
}

@Test
public void test_Conditional() {
    
    try (ApplicationContext applicationContext = new StandardApplicationContext(true)) {
        User user = applicationContext.getBean("user_", User.class);
        System.out.println(user);
        assert "Windows".equals(user.getUserName());
    }
}
```


## v2.0.0
- 加入`ObjectFactory`
- 框架重构 


## v1.2.1
- 修复注入原型错误   


## v1.2.0

- 添加 AnntationApplicationContext 支持 `Configuration` 注解
- `ClassPathApplicationContext` 改为 `DefaultApplicationContext`
- bean 注解支持方法标注


## v1.1.1
   
- 修复单例情况下接口对象与实现类对象不一致问题
- 添加部分doc
- 添加refresh方法
- 添加test code
- Properties 注入时可以选择替换掉前缀，前缀有时是为了区分一类配置
- 优化注入可以自定义注解注入
   
## v1.0.3

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

	/**
	 * replace prefix.
	 * 
	 * @return
	 */
	boolean replace() default false;

}

```

