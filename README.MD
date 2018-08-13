# TODAY IOC
    
#### Today IOC 是我学习 Spring IOC 以及自己对IOC的理解之作。
### 前言
> 本人大学生一枚，专业是电子信息工程，大二在读。大一开始学Java，准确的说是高三最后的几周开始的. 果然兴趣是最好的老师， 在大一下学期自己独自一人从前端到后台写了我的个人网站：<a href="https://yanghaijian.top" target="_blank">TODAY BLOG</a> 。 从注册域名到备案再到网站成功上线，我遇到过的困难数不计其数。因为感兴趣所以我坚持了下来。第一个版本使用的纯Servlet写的。后来了解到Java有很多开源框架可以简化我的开发。于是又投入到新一轮的学习之中...... 学了Struts2后自己学着写了一个小框架：[TODAY WEB](https://gitee.com/TAKETODAY/today_web/tree/v1.1.1/)，几百行搞定从解析xml定义的action到处理对应的请求。学了Spring MVC 我从自己的开发过程中发现了还是有不足的地方，于是我又写了[TODAY WEB 2.0](https://gitee.com/TAKETODAY/today_web) 。

### 使用说明

> 测试 Bean
```java
@Setter
@Getter
@Singleton
@Prototype("prototype_config")
public final class Config {

    private Integer               id;

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
    @Value("#{site.baiduCode}")
    private String                baiduCode;
    @Value("#{site.server.path}")
    private String                serverPath;
    @Value("#{site.description}")
    private String                description;

    public Config() {

    }

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
        bean.setBaiduCode(pro.getProperty("site.baiduCode"));
        bean.setCopyright(pro.getProperty("site.copyright"));
    }

}


```

> 自动装载
```java
public void test_AutoLoadContextAndClearCache() throws NoSuchBeanDefinitionException {
        // auto load context and clear cache.
        ApplicationContext applicationContext = new ClassPathApplicationContext(true);

        Map<String, BeanDefinition> beanDefinitions = applicationContext.getBeanDefinitionRegistry()
                .getBeanDefinitionsMap();
        
        assert beanDefinitions.size() != 0 : "nothing in context.";

        Set<Class<?>> classCache = ClassUtils.getClassCache();
        
        assert classCache.size() == 0 : "cache clear error.";
        applicationContext.close();
    }
```
> 手动装载
```java

public void test_ManualLoadContext() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException {
        
        ApplicationContext applicationContext = new ClassPathApplicationContext();
        
        applicationContext.registerBean("user", User.class);//singleton
        applicationContext.registerBean("user_", User.class);
        applicationContext.onRefresh(); //init bean
        
        Map<String, BeanDefinition> beanDefinitionsMap = applicationContext.getBeanDefinitionRegistry().getBeanDefinitionsMap();
        System.out.println(beanDefinitionsMap);
        
        Object bean = applicationContext.getBean("user");
        
        assert bean != null : "error";
        
        applicationContext.close();
    }
```
> FactoryBean
```java
public void test_AutoLoadFactoryBean_() throws NoSuchBeanDefinitionException {
    ApplicationContext applicationContext = new ClassPathApplicationContext(true);
    Config config = applicationContext.getBean("FactoryBean-Config", Config.class);
    Config config_ = applicationContext.getBean("FactoryBean-Config", Config.class);

    assert config == config_ : "FactoryBean error.";

    applicationContext.close();
}
```
> 原型Bean
```java
public void test_AutoLoadPrototype() throws NoSuchBeanDefinitionException {
    ApplicationContext applicationContext = new ClassPathApplicationContext(true);
    Config config = applicationContext.getBean("prototype_config", Config.class);
    Config config_ = applicationContext.getBean("prototype_config", Config.class);
    
    assert config != config_ : "prototype error.";

    applicationContext.close();
}
```
### 联系方式
- 邮箱 taketoday@foxmail.com


### 开源协议

请查看 [GNU GENERAL PUBLIC LICENSE](https://gitee.com/TAKETODAY/today_context/blob/master/LICENSE)

