# TODAY Context
    
## 前言
> 本人大学生一枚，专业是电子信息工程，大三在读。大一开始学Java，准确的说是高三最后的几周开始的. 果然兴趣是最好的老师， 在大一下学期自己独自一人从前端到后台写了我的个人网站：<a href="https://taketoday.cn" target="_blank">TODAY BLOG</a> 。 从注册域名到备案再到网站成功上线，我遇到过的困难数不计其数。因为感兴趣所以我坚持了下来。第一个版本使用的纯Servlet写的。后来了解到Java有很多开源框架可以简化我的开发。于是又投入到新一轮的学习之中...... 学了Struts2后自己学着写了一个小框架：[TODAY WEB](https://gitee.com/TAKETODAY/today_web/tree/v1.1.1/)，几百行搞定从解析xml定义的action到处理对应的请求。学了Spring MVC 我从自己的开发过程中发现了还是有不足的地方，于是我又写了[TODAY WEB 2.0](https://gitee.com/TAKETODAY/today_web) 。新的版本急需一个IOC容器，所以就有了此项目。


## 安装

```xml
<dependency>
    <groupId>cn.taketoday</groupId>
    <artifactId>today-context</artifactId>
    <version>2.1.4.RELEASE</version>
</dependency>
```
- [Maven Central](https://search.maven.org/artifact/cn.taketoday/today-context/2.1.3.RELEASE/jar)


## 使用说明

> 测试 Bean

```java
@Setter
@Getter
@Singleton
@NoArgsConstructor
@Prototype("prototype_config")
public final class Config {

    @Value("#{site.cdn}")
    private String                cdn;
    @Value("#{site.icp}")
    private String                icp;
    @Value("#{site.host}")
    private String                host;
    @Value("#{site.index}")
    private String                index;
    @Value("#{site.upload}")
    private String                upload;
    @Value("#{site.keywords}")
    private String                keywords;
    @Value("#{site.name}")
    private String                siteName;
    @Value("#{site.copyright}")
    private String                copyright;
    @Value("#{site.server.path}")
    private String                serverPath;
    @Value("#{site.description}")
    private String                description;
}

//FactoryBean
@Singleton
public class ConfigFactoryBean implements FactoryBean<Config>, InitializingBean, DisposableBean {

    @Props(value = "info", prefix = "site.")
    private Properties pro;
    
    private Config bean;
    
    @Override
    public Config getBean() {
        return bean;
    }
    
    @Override
    public String getBeanName() {
        return "FactoryBean-Config";
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void destroy() throws Exception {
        pro.clear();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        bean = new Config();
        
        bean.setCdn(pro.getProperty("site.cdn"));
        bean.setHost(pro.getProperty("site.host"));
        bean.setCopyright(pro.getProperty("site.copyright"));
    }

}

@Configuration
public final class ConfigurationBean {

	@Prototype
	public User user() {
		return new User().setId(12);
	}

	@Singleton("USER")
	public User user() {
		return new User();
	}
	
}
```
> 自动装载

```java
@Test
public void test_AutoLoadContextAndClearCache() throws NoSuchBeanDefinitionException {
	// auto load context and clear cache.
	try (ApplicationContext applicationContext = new DefaultApplicationContext(true)) {

		Map<String, BeanDefinition> beanDefinitions = applicationContext.getEnvironment()
				.getBeanDefinitionRegistry().getBeanDefinitionsMap();

		assert beanDefinitions.size() != 0 : "nothing in context.";

		Collection<Class<?>> classCache = ClassUtils.getClassCache();

		assert classCache.size() == 0 : "cache clear error.";
	}
}
```
> 手动装载
```java

@Test
public void test_ManualLoadContext() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException {

	try (ApplicationContext applicationContext = new DefaultApplicationContext()) {

		applicationContext.registerBean(User.class);
		applicationContext.registerBean("user", User.class);
		applicationContext.registerBean("user_", User.class);
//		applicationContext.onRefresh(); // init bean

		Map<String, BeanDefinition> beanDefinitionsMap = applicationContext.getEnvironment()
				.getBeanDefinitionRegistry().getBeanDefinitionsMap();
		
		System.out.println(beanDefinitionsMap);

		Object bean = applicationContext.getBean("user");
		assert beanDefinitionsMap.size() == 2 : "error";
		assert bean != null : "error";
	}
}
```
> FactoryBean
```java
@Test
public void test_AutoLoadFactoryBean() throws NoSuchBeanDefinitionException {
	try (ApplicationContext applicationContext = new StandardApplicationContext(true)) {
			
	    Config config = applicationContext.getBean("FactoryBean-Config", Config.class);
	    Config config_ = applicationContext.getBean("FactoryBean-Config", Config.class);
	
	    assert config == config_ : "FactoryBean error.";
	}
}
```
> 原型Bean

```java
@Test
public void test_AutoLoadPrototype() throws NoSuchBeanDefinitionException {
	try (ApplicationContext applicationContext = new StandardApplicationContext(true)) {
		
	    Config config = applicationContext.getBean("prototype_config", Config.class);
	    Config config_ = applicationContext.getBean("prototype_config", Config.class);
	    
	    assert config != config_ : "prototype error.";
	}
}
```
> @Configuration
```java
@Configuration
@NoArgsConstructor
@Props(prefix = "site.")
public class Config_ {

	private String cdn;
	private String icp;
	private String host;
	private String index;
	
	@Override
	public String toString() {
		return new StringBuilder()//
				.append("{\n\t\"cdn\":\"").append(cdn)//
				.append("\",\n\t\"icp\":\"").append(icp)//
				.append("\",\n\t\"host\":\"").append(host)//
				.append("\",\n\t\"index\":\"").append(index)//
				.append("\"\n}")//
				.toString();
	}
}
@Test
public void test_Props() {
	try (ApplicationContext applicationContext = new StandardApplicationContext(true)) {
		
		Config_ bean = applicationContext.getBean(Config_.class);
		assert "https://taketoday.cn".equals(bean.getHost());
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

## 联系方式
- 邮箱 taketoday@foxmail.com


## 开源协议

请查看 [GNU GENERAL PUBLIC LICENSE](https://github.com/TAKETODAY/today-context/blob/master/LICENSE)

