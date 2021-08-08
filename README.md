# TODAY Context

🍎 A Java library for dependency injection and aspect oriented programing

![Java8](https://img.shields.io/badge/JDK-8+-success.svg)
[![GPLv3](https://img.shields.io/badge/License-GPLv3-blue.svg)](./LICENSE)
[![Author](https://img.shields.io/badge/Author-TODAY-blue.svg)](https://github.com/TAKETODAY)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3ad5eed64065496fba9244d149820f67)](https://www.codacy.com/app/TAKETODAY/today-context?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=TAKETODAY/today-context&amp;utm_campaign=Badge_Grade)
[![GitHub CI](https://github.com/TAKETODAY/today-context/workflows/GitHub%20CI/badge.svg)](https://github.com/TAKETODAY/today-context/actions)
[![Travis CI](https://travis-ci.org/TAKETODAY/today-context.svg?branch=master)](https://travis-ci.org/TAKETODAY/today-context)
[![Coverage Status](https://coveralls.io/repos/github/TAKETODAY/today-context/badge.svg?branch=master)](https://coveralls.io/github/TAKETODAY/today-context?branch=master)

## 🛠️ 安装

```xml
<dependency>
  <groupId>cn.taketoday</groupId>
  <artifactId>today-context</artifactId>
  <version>3.0.5.RELEASE</version>
</dependency>
```
- [Maven Central](https://search.maven.org/artifact/cn.taketoday/today-context/3.0.4.RELEASE/jar)

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
- 注入示例：

```java
@Controller
@SuppressWarnings("serial")
public class LoginController implements Constant, ServletContextAware {

    private String contextPath;
    @Autowired
    private UserService userService;
    //@Inject
    @Resource
    private BloggerService bloggerService;

    @GET("/login")
    public String login(@Cookie String email, String forward, Model model) {

        model.attribute(KEY_EMAIL, email);
        model.attribute("forward", forward);

        return "/login/index";
    }

    @POST("/login")
    @Logger(value = "登录", //
            content = "email:[${email}] " //
                    + "passwd:[${passwd}] "//
                    + "input code:[${randCode}] "//
                    + "in session:[${randCodeInSession}] "//
                    + "forward to:[${forward}] "//
                    + "msg:[${redirectModel.attribute('msg')}]"//
    )
    public String login(HttpSession session,
            @Cookie(KEY_EMAIL) String emailInCookie,
            @RequestParam(required = true) String email,
            @RequestParam(required = true) String passwd,
            @RequestParam(required = true) String randCode,
            @RequestParam(required = false) String forward,
            @Session(RAND_CODE) String randCodeInSession, RedirectModel redirectModel) //
    {
        session.removeAttribute(RAND_CODE);

        if (!randCode.equalsIgnoreCase(randCodeInSession)) {
            redirectModel.attribute(KEY_MSG, "验证码错误!");
            redirectModel.attribute(KEY_EMAIL, email);
            redirectModel.attribute(KEY_FORWARD, forward);
            return redirectLogin(forward);
        }

        User loginUser = userService.login(new User().setEmail(email));
        if (loginUser == null) {
            redirectModel.attribute(KEY_EMAIL, email);
            redirectModel.attribute(KEY_FORWARD, forward);
            redirectModel.attribute(KEY_MSG, email + " 账号不存在!");
            return redirectLogin(forward);
        }
     // 😋 略
    }
    @GET("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/index";
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.contextPath = servletContext.getContextPath();
    }
}
  
```

- 实现原理

```java
public class AutowiredPropertyResolver implements PropertyValueResolver {

    private static final Class<? extends Annotation> NAMED_CLASS = ClassUtils.loadClass("javax.inject.Named");
    private static final Class<? extends Annotation> INJECT_CLASS = ClassUtils.loadClass("javax.inject.Inject");

    @Override
    public boolean supports(Field field) {
        return field.isAnnotationPresent(Autowired.class)
                || field.isAnnotationPresent(Resource.class)
                || (NAMED_CLASS != null && field.isAnnotationPresent(NAMED_CLASS))
                || (INJECT_CLASS != null && field.isAnnotationPresent(INJECT_CLASS));
}
    
    @Override
    public PropertyValue resolveProperty(Field field) {
    
        final Autowired autowired = field.getAnnotation(Autowired.class); // auto wired
    
        String name = null;
        boolean required = true;
        final Class<?> propertyClass = field.getType();
    
        if (autowired != null) {
            name = autowired.value();
            required = autowired.required();
        }
        else if (field.isAnnotationPresent(Resource.class)) {
            name = field.getAnnotation(Resource.class).name(); // Resource.class
        }
        else if (NAMED_CLASS != null && field.isAnnotationPresent(NAMED_CLASS)) {// @Named
            name = AnnotationUtils.getAnnotationAttributes(NAMED_CLASS, field).getString(Constant.VALUE);
        } // @Inject or name is empty
    
        if (StringUtils.isEmpty(name)) {
            name = byType(propertyClass);
        }
    
        return new PropertyValue(new BeanReference(name, required, propertyClass), field);
    }
}
```

看到这你应该明白了注入原理了

### 使用`@Autowired`构造器注入

```java
// cn.taketoday.web.servlet.DispatcherServlet
public class DispatcherServlet implements Servlet, Serializable {

    private static final Logger log = LoggerFactory.getLogger(DispatcherServlet.class);

    /** exception resolver */
    private final ExceptionResolver exceptionResolver;
    /** Action mapping registry */
    private final HandlerMappingRegistry handlerMappingRegistry;
    /** intercepter registry */
    private final HandlerInterceptorRegistry handlerInterceptorRegistry;

    private final WebServletApplicationContext applicationContext;

    private ServletConfig servletConfig;

    @Autowired
    public DispatcherServlet(//
            ExceptionResolver exceptionResolver, //
            HandlerMappingRegistry handlerMappingRegistry, //
            WebServletApplicationContext applicationContext,
            HandlerInterceptorRegistry handlerInterceptorRegistry) //
    {
        if (exceptionResolver == null) {
            throw new ConfigurationException("You must provide an 'exceptionResolver'");
        }
        this.exceptionResolver = exceptionResolver;

        this.applicationContext = applicationContext;
        this.handlerMappingRegistry = handlerMappingRegistry;
        this.handlerInterceptorRegistry = handlerInterceptorRegistry;
    }

    public static RequestContext prepareContext(final ServletRequest request, final ServletResponse response) {
        return RequestContextHolder.prepareContext(//
                new ServletRequestContext((HttpServletRequest) request, (HttpServletResponse) response)//
        );
    }

    @Override
    public void service(final ServletRequest req, final ServletResponse res) //
            throws ServletException, IOException //
    {
        // Lookup handler mapping
        final HandlerMapping mapping = lookupHandlerMapping((HttpServletRequest) req);

        if (mapping == null) {
            ((HttpServletResponse) res).sendError(404);
            return;
        }

        final RequestContext context = prepareContext(req, res);
        try {

            final Object result;
            // Handler Method
            final HandlerMethod method;// = requestMapping.getHandlerMethod();
            if (mapping.hasInterceptor()) {
                // get intercepter s
                final int[] its = mapping.getInterceptors();
                // invoke intercepter
                final HandlerInterceptorRegistry registry = getHandlerInterceptorRegistry();
                for (final int i : its) {
                    if (!registry.get(i).beforeProcess(context, mapping)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Interceptor: [{}] return false", registry.get(i));
                        }
                        return;
                    }
                }
                result = invokeHandler(context, method = mapping.getHandlerMethod(), mapping);
                for (final int i : its) {
                    registry.get(i).afterProcess(context, mapping, result);
                }
            }
            else {
                result = invokeHandler(context, method = mapping.getHandlerMethod(), mapping);
            }

            method.resolveResult(context, result);
        }
        catch (Throwable e) {
            ResultUtils.resolveException(context, exceptionResolver, mapping, e);
        }
    }

    protected Object invokeHandler(final RequestContext request,
            final HandlerMethod method, final HandlerMapping mapping) throws Throwable //
    {
        // log.debug("set parameter start");
        return method.getMethod()//
                .invoke(mapping.getBean(), method.resolveParameters(request)); // invoke
    }

    protected HandlerMapping lookupHandlerMapping(final HttpServletRequest req) {
        // The key of handler
        String uri = req.getMethod() + req.getRequestURI();

        final HandlerMappingRegistry registry = getHandlerMappingRegistry();
        final Integer i = registry.getIndex(uri); // index of handler mapping
        if (i == null) {
            // path variable
            uri = StringUtils.decodeUrl(uri);// decode
            for (final RegexMapping regex : registry.getRegexMappings()) {
                // TODO path matcher pathMatcher.match(requestURI, requestURI)
                if (regex.pattern.matcher(uri).matches()) {
                    return registry.get(regex.index);
                }
            }
            log.debug("NOT FOUND -> [{}]", uri);
            return null;
        }
        return registry.get(i.intValue());
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        this.servletConfig = servletConfig;
    }

    @Override
    public ServletConfig getServletConfig() {
        return servletConfig;
    }

    public String getServletName() {
        return "DispatcherServlet";
    }

    @Override
    public String getServletInfo() {
        return "DispatcherServlet, Copyright © TODAY & 2017 - 2020 All Rights Reserved";
    }

    @Override
    public void destroy() {

        if (applicationContext != null) {
            final State state = applicationContext.getState();

            if (state != State.CLOSING && state != State.CLOSED) {

                applicationContext.close();

                final DateFormat dateFormat = new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT);//
                final String msg = new StringBuffer()//
                        .append("Your application destroyed at: [")//
                        .append(dateFormat.format(new Date()))//
                        .append("] on startup date: [")//
                        .append(dateFormat.format(applicationContext.getStartupDate()))//
                        .append("]")//
                        .toString();

                log.info(msg);
                applicationContext.getServletContext().log(msg);
            }
        }
    }

    public final HandlerInterceptorRegistry getHandlerInterceptorRegistry() {
        return this.handlerInterceptorRegistry;
    }

    public final HandlerMappingRegistry getHandlerMappingRegistry() {
        return this.handlerMappingRegistry;
    }

    public final ExceptionResolver getExceptionResolver() {
        return this.exceptionResolver;
    }

}
//cn.taketoday.web.view.FreeMarkerViewResolver

@Props(prefix = "web.mvc.view.")
@MissingBean(value = Constant.VIEW_RESOLVER, type = ViewResolver.class)
public class FreeMarkerViewResolver extends AbstractViewResolver implements InitializingBean, WebMvcConfiguration {

    private final ObjectWrapper wrapper;

    @Getter
    private final Configuration configuration;
    private final TaglibFactory taglibFactory;
    private final TemplateLoader templateLoader;
    private final ServletContextHashModel applicationModel;

    public FreeMarkerViewResolver(Configuration configuration, //
            TaglibFactory taglibFactory, TemplateLoader templateLoader, Properties settings) //
    {
        this(new DefaultObjectWrapper(Configuration.VERSION_2_3_28), //
                configuration, taglibFactory, templateLoader, settings);
    }

    @Autowired
    public FreeMarkerViewResolver(//
            @Autowired(required = false) ObjectWrapper wrapper, //
            @Autowired(required = false) Configuration configuration, //
            @Autowired(required = false) TaglibFactory taglibFactory, //
            @Autowired(required = false) TemplateLoader templateLoader, //
            @Props(prefix = "freemarker.", replace = true) Properties settings) //
    {

        WebServletApplicationContext webApplicationContext = //
                (WebServletApplicationContext) WebUtils.getWebApplicationContext();

        if (configuration == null) {
            configuration = new Configuration(Configuration.VERSION_2_3_28);
            webApplicationContext.registerSingleton(configuration.getClass().getName(), configuration);
        }

        this.configuration = configuration;
        if (wrapper == null) {
            wrapper = new DefaultObjectWrapper(Configuration.VERSION_2_3_28);
        }
        this.wrapper = wrapper;
        ServletContext servletContext = webApplicationContext.getServletContext();
        if (taglibFactory == null) {
            taglibFactory = new TaglibFactory(servletContext);
        }
        this.taglibFactory = taglibFactory;
        this.configuration.setObjectWrapper(wrapper);
        // Create hash model wrapper for servlet context (the application)
        this.applicationModel = new ServletContextHashModel(servletContext, wrapper);

        webApplicationContext.getBeansOfType(TemplateModel.class).forEach(configuration::setSharedVariable);

        this.templateLoader = templateLoader;
        try {
            if (settings != null && !settings.isEmpty()) {
                this.configuration.setSettings(settings);
            }
        }
        catch (TemplateException e) {
            throw new ConfigurationException("Set FreeMarker's Properties Error, With Msg: [" + e.getMessage() + "]", e);
        }
    }

    @Override
    public void afterPropertiesSet() throws ConfigurationException {

        this.configuration.setLocale(locale);
        this.configuration.setDefaultEncoding(encoding);
        if (templateLoader == null) {
            this.configuration.setServletContextForTemplateLoading(servletContext, prefix); // prefix -> /WEB-INF/..
        }
        else {
            configuration.setTemplateLoader(templateLoader);
        }
        LoggerFactory.getLogger(getClass()).info("Configuration FreeMarker View Resolver Success.");
    }

    @Override
    public void configureParameterResolver(List<ParameterResolver> resolvers) {

        resolvers.add(new DelegatingParameterResolver((m) -> m.isAssignableFrom(Configuration.class), //
                (ctx, m) -> configuration//
        ));

        resolvers.add(new DelegatingParameterResolver((m) -> m.isAnnotationPresent(SharedVariable.class), (ctx, m) -> {
            final TemplateModel sharedVariable = configuration.getSharedVariable(m.getName());

            if (m.isInstance(sharedVariable)) {
                return sharedVariable;
            }

            if (sharedVariable instanceof WrapperTemplateModel) {
                final Object wrappedObject = ((WrapperTemplateModel) sharedVariable).getWrappedObject();
                if (m.isInstance(wrappedObject)) {
                    return wrappedObject;
                }
                throw ExceptionUtils.newConfigurationException(null, "Not a instance of: " + m.getParameterClass());
            }
            return null;
        }));

    }

    protected TemplateHashModel createModel(RequestContext requestContext) {
        final ObjectWrapper wrapper = this.wrapper;

        final HttpServletRequest request = requestContext.nativeRequest();

        final AllHttpScopesHashModel allHttpScopesHashModel = //
                new AllHttpScopesHashModel(wrapper, servletContext, request);

        allHttpScopesHashModel.putUnlistedModel(FreemarkerServlet.KEY_JSP_TAGLIBS, taglibFactory);
        allHttpScopesHashModel.putUnlistedModel(FreemarkerServlet.KEY_APPLICATION, applicationModel);
        // Create hash model wrapper for request
        allHttpScopesHashModel.putUnlistedModel(FreemarkerServlet.KEY_REQUEST, new HttpRequestHashModel(request, wrapper));
        allHttpScopesHashModel.putUnlistedModel(FreemarkerServlet.KEY_REQUEST_PARAMETERS, new HttpRequestParametersHashModel(request));
        // Create hash model wrapper for session
        allHttpScopesHashModel.putUnlistedModel(FreemarkerServlet.KEY_SESSION,
                new HttpSessionHashModel(requestContext.nativeSession(), wrapper));

        return allHttpScopesHashModel;
    }

    /**
     * Resolve FreeMarker View.
     */
    @Override
    public void resolveView(final String template, final RequestContext requestContext) throws Throwable {

        configuration.getTemplate(template + suffix, locale, encoding)//
                .process(createModel(requestContext), requestContext.getWriter());
    }
}

```

### 使用`@Props` 注入Properties或Bean

- 构造器

```java
    @Autowired
    public PropsBean(@Props(prefix = "site.") Bean bean) {
       //---------
    }
    @Autowired
    public PropsBean(@Props(prefix = "site.") Properties properties) {
        //-------
    }
    @Autowired
    public PropsBean(@Props(prefix = "site.") Map properties) {
        //-------
    }
```

- Field

```java
    @Props(prefix = "site.")
    Bean bean;
    @Props(prefix = "site.")
    Map properties;
    @Props(prefix = "site.") 
    Properties properties
```

- 实现原理

```java
@Order(Ordered.HIGHEST_PRECEDENCE - 2)
public class PropsPropertyResolver implements PropertyValueResolver {

    @Override
    public boolean supports(Field field) {
        return field.isAnnotationPresent(Props.class);
    }

}
```

### 使用`@Value` 支持EL表达式
- 和`@Props`一样同样支持构造器，Field注入
- `#{key}` 和Environment#getProperty(String key, Class<T> targetType)效果一样
- `${1+1}` 支持EL表达式

```java 
@Configuration
public class WebMvc implements WebMvcConfiguration {

    private final String serverPath;

    @Autowired
    public WebMvc(@Value("#{site.serverPath}") String serverPath) {
        this.serverPath = serverPath;
    }
    @Override
    public void configureResourceMappings(ResourceMappingRegistry registry) {

        registry.addResourceMapping("/assets/**")//
//                .enableGzip()//
//                .gzipMinLength(10240)//
                // G:\Projects\Git\today-technology\blog
                .addLocations("file:///G:/Projects/Git/today-technology/blog/blog-web/src/main/webapp/assets/");

        registry.addResourceMapping("/webjars/**")//
                .addLocations("classpath:/META-INF/resources/webjars/");

        registry.addResourceMapping("/swagger/**")//
                .cacheControl(CacheControl.newInstance().publicCache())//
                .addLocations("classpath:/META-INF/resources/");

        registry.addResourceMapping("/upload/**")//
                .addLocations("file:///" + serverPath + "/upload/");

        registry.addResourceMapping("/favicon.ico")//
                .addLocations("classpath:/favicon.ico")//
                .cacheControl(CacheControl.newInstance().publicCache());

        registry.addResourceMapping(AdminInterceptor.class)//
                .setPathPatterns("/assets/admin/**")//
                .setOrder(Ordered.HIGHEST_PRECEDENCE)//
                .addLocations("file:///G:/Projects/Git/today-technology/blog/blog-web/src/main/webapp/assets/admin/");
    
    }
}
```

### `@Configuration`注解
该注解标识一个配置Bean,示例：

```java
@Configuration
@Props(prefix = { "redis.pool.", "redis." })
public class RedisConfiguration {

    private int maxIdle;
    private int minIdle;
    private int timeout;
    private int maxTotal;

    private int database;
    private String address;

    private String password;
    private String clientName;

    private int connectTimeout;

    @Singleton("fstCodec")
    public Codec codec() {
        return new FstCodec();
    }
//  @Singleton("limitLock")
//  public Lock limitLock(Redisson redisson) {
//      return redisson.getLock("limitLock");
//  }

    @Singleton(destroyMethods = "shutdown")
    public Redisson redisson(@Autowired("fstCodec") Codec codec) {
        Config config = new Config();
        config.setCodec(codec)//
                .useSingleServer()//
                .setAddress(address)//
                .setTimeout(timeout)//
                .setPassword(password)//
                .setDatabase(database)//
                .setClientName(clientName)//
                .setConnectionPoolSize(maxTotal)//
                .setConnectTimeout(connectTimeout)//
                .setConnectionMinimumIdleSize(minIdle)//
                .setConnectionMinimumIdleSize(maxIdle);

        return (Redisson) Redisson.create(config);
    }

    @Singleton("loggerDetails")
    public <T> Queue<T> loggerDetails(Redisson redisson) {
        return new ConcurrentLinkedQueue<T>();
    }

    @Singleton("cacheViews")
    public Map<Long, Long> cacheViews(Redisson redisson) {
        return redisson.getMap("cacheViews", LongCodec.INSTANCE);
    }

    @Singleton("articleLabels")
    public Map<Long, Set<Label>> articleLabels(Redisson redisson, @Autowired("fstCodec") Codec codec) {
        return redisson.getMap("articleLabels", codec);
    }

    @Singleton("optionsMap")
    public Map<String, String> optionsMap(Redisson redisson) {
//      redisson.getKeys().flushdb();
        return redisson.getMap("optionsMap", StringCodec.INSTANCE);
    }

    @Singleton("categories")
    public List<Category> categories(Redisson redisson, @Autowired("fstCodec") Codec codec) {
        return redisson.getList("categories", codec);
    }

    @Singleton("labels")
    public Set<Label> labels(Redisson redisson, @Autowired("fstCodec") Codec codec) {
        return redisson.getSet("labels", codec);
    }

    @Value(value = "#{limit.time.out}", required = false)
    private int limitTimeOut = Constant.ACCESS_TIME_OUT;

    @Singleton("limitCache")
    public Map<String, Long> limitCache(Redisson redisson) {
        final RMapCache<String, Long> mapCache = redisson.getMapCache("limitCache", LongCodec.INSTANCE);
        mapCache.expire(limitTimeOut, TimeUnit.MILLISECONDS);
        return mapCache;
    }

}

```

### 生命周期

```java
package test.context;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.junit.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.factory.DisposableBean;
import cn.taketoday.context.factory.InitializingBean;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TODAY <br>
 *         2019-07-25 22:44
 */
@Slf4j
@Singleton
public class LifecycleBean implements InitializingBean, DisposableBean {

    @PostConstruct
    public void initData() {
        log.info("@PostConstruct");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("afterPropertiesSet");
    }

    @PreDestroy
    public void preDestroy() {
        log.info("preDestroy");
    }

    @Override
    public void destroy() throws Exception {
        log.info("destroy");
    }

    @Test
    public void testLifecycle() {

        final Set<Class<?>> beans = new HashSet<>();
        beans.add(LifecycleBean.class);

        try (final ApplicationContext applicationContext = new StandardApplicationContext(beans)) {
            Map<String, BeanDefinition> beanDefinitionsMap = applicationContext.getEnvironment().getBeanDefinitionRegistry().getBeanDefinitions();

            System.out.println(beanDefinitionsMap);
        }
    }

}
```

```log
2019-07-25 23:14:37.712  INFO - [            main] c.t.context.AbstractApplicationContext    150 - Starting Application Context at [2019-07-25 23:14:37.707].
2019-07-25 23:14:37.751  INFO - [            main] c.t.context.env.StandardEnvironment       236 - Found Properties Resource: [file:/G:/Projects/Git/github/today-context/target/test-classes/info.properties]
2019-07-25 23:14:37.771  INFO - [            main] c.t.context.env.StandardEnvironment       129 - Active profiles: [test, dev]
2019-07-25 23:14:37.858 DEBUG - [            main] c.t.context.AbstractApplicationContext    325 - Loading Application Listeners.
2019-07-25 23:14:37.896 DEBUG - [            main] c.t.context.AbstractApplicationContext    480 - Publish event: [cn.taketoday.context.event.BeanDefinitionLoadingEvent]
2019-07-25 23:14:37.918 DEBUG - [            main] c.t.context.AbstractApplicationContext    480 - Publish event: [cn.taketoday.context.event.LoadingMissingBeanEvent]
2019-07-25 23:14:37.921 DEBUG - [            main] c.t.context.AbstractApplicationContext    480 - Publish event: [cn.taketoday.context.event.BeanDefinitionLoadedEvent]
2019-07-25 23:14:37.922 DEBUG - [            main] c.t.context.AbstractApplicationContext    480 - Publish event: [cn.taketoday.context.event.DependenciesHandledEvent]
2019-07-25 23:14:37.923 DEBUG - [            main] c.t.context.factory.AbstractBeanFactory   581 - Start loading BeanPostProcessor.
2019-07-25 23:14:37.926 DEBUG - [            main] c.t.context.AbstractApplicationContext    480 - Publish event: [cn.taketoday.context.event.ContextPreRefreshEvent]
2019-07-25 23:14:37.927 DEBUG - [            main] c.t.context.AbstractApplicationContext    480 - Publish event: [cn.taketoday.context.event.ContextRefreshEvent]
2019-07-25 23:14:37.928 DEBUG - [            main] c.t.context.factory.AbstractBeanFactory  1002 - Initialization of singleton objects.
2019-07-25 23:14:37.929 DEBUG - [            main] c.t.context.factory.AbstractBeanFactory   651 - Initializing bean named: [lifecycleBean].
2019-07-25 23:14:37.929  INFO - [            main] test.context.LifecycleBean                 59 - setBeanName: lifecycleBean
2019-07-25 23:14:37.930  INFO - [            main] test.context.LifecycleBean                 69 - setBeanFactory: cn.taketoday.context.factory.StandardBeanFactory@5ce81285
2019-07-25 23:14:37.930  INFO - [            main] test.context.LifecycleBean                 74 - setApplicationContext: cn.taketoday.context.StandardApplicationContext@78c03f1f
2019-07-25 23:14:37.930  INFO - [            main] test.context.LifecycleBean                 64 - setEnvironment: cn.taketoday.context.StandardEnvironment@5ec0a365
2019-07-25 23:14:37.931  INFO - [            main] test.context.LifecycleBean                 79 - @PostConstruct
2019-07-25 23:14:37.931  INFO - [            main] test.context.LifecycleBean                 84 - afterPropertiesSet
2019-07-25 23:14:37.931 DEBUG - [            main] c.t.context.factory.AbstractBeanFactory   511 - Singleton bean is being stored in the name of [lifecycleBean]
2019-07-25 23:14:37.932 DEBUG - [            main] c.t.context.factory.AbstractBeanFactory  1008 - The singleton objects are initialized.
2019-07-25 23:14:37.932 DEBUG - [            main] c.t.context.AbstractApplicationContext    480 - Publish event: [cn.taketoday.context.event.ContextStartedEvent]
2019-07-25 23:14:37.941  INFO - [            main] c.t.context.AbstractApplicationContext    210 - Application Context Startup in 234ms
{lifecycleBean={
    "name":"lifecycleBean",
    "scope":"SINGLETON",
    "beanClass":"class test.context.LifecycleBean",
    "initMethods":"[public void test.context.LifecycleBean.initData()]",
    "destroyMethods":"[]",
    "propertySetters":"[]",
    "initialized":"true",
    "factoryBean":"false",
    "abstract":"false"
}}
2019-07-25 23:14:37.943 DEBUG - [            main] c.t.context.AbstractApplicationContext    480 - Publish event: [cn.taketoday.context.event.ContextCloseEvent]
context is closing
2019-07-25 23:14:37.945  INFO - [            main] c.t.c.listener.ContextCloseListener        52 - Closing: [cn.taketoday.context.StandardApplicationContext@78c03f1f] at [2019-07-25 23:14:37.943]
2019-07-25 23:14:37.947  INFO - [            main] test.context.LifecycleBean                 89 - preDestroy
2019-07-25 23:14:37.947  INFO - [            main] test.context.LifecycleBean                 94 - destroy

```

## AOP部分

底层使用`CGLIB`，JDK动态代理，或自己写的`StandardAopProxy` ,`CGLIB`,`JDK` 版本功能最完善,来自`Spring Aop`使用了Spring的抽象接口，自己用字节码技术实现了`StandardAopProxy`  自以为性能更优。

> 使用@Aspect标注一个切面

```java
@Aspect
@Component
@EnableAspectAutoProxy
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LogAspect {

    @AfterReturning(Logger.class)
    public void afterReturning(@Returning Object returnValue) {
        log.debug("@AfterReturning returnValue: [{}]", returnValue);
    }
    
    @AfterThrowing(Logger.class)
    public void afterThrowing(@Throwing Throwable throwable) {
        log.error("@AfterThrowing With Msg: [{}]", throwable.getMessage(), throwable);
    }
    
    @Before(Logger.class)
    public void before(@Annotated Logger logger, @Argument User user) {
        log.debug("@Before method in class with logger: [{}] , Argument:[{}]", logger, user);
    }
    
    @After(Logger.class)
    public Object after(@Returning User returnValue, @Arguments Object[] arguments) {
        log.debug("@After method in class");
        return returnValue.setSex("女");
    }

    @Around(Logger.class)
    public Object around(@JoinPoint Joinpoint joinpoint) throws Throwable {
       log.debug("@Around Before method");
//      int i = 1 / 0;
        Object proceed = joinpoint.proceed();
        log.debug("@Around After method");
        return proceed;
    }
}

public @interface Logger {
    /** operation */
    String value() default "";
}

@Service
public class DefaultUserService implements UserService {

    @Autowired
    private UserDao userDao;
    
    @Logger("登录")
    @Override
    public User login(User user) {
        log.debug("login");
//      int i = 1 / 0;
        return userDao.login(user);
    }
    @Logger("注册")
    @Override
    public boolean register(User user) {
        return userDao.save(user);
    }
}
@Repository
public class UserDaoImpl implements UserDao {
    
    private Map<String, User> users = new HashMap<>();
    
    public UserDaoImpl() {
        users.put("666", new User(1, "杨海健", 20, "666", "666", "男", new Date()));
        users.put("6666", new User(2, "杨海健1", 20, "6666", "6666", "男", new Date()));
        users.put("66666", new User(3, "杨海健2", 20, "66666", "66666", "男", new Date()));
        users.put("666666", new User(4, "杨海健3", 20, "666666", "666666", "男", new Date()));
    }
    
    @Override
    public boolean save(User user) {
        users.put(user.getUserId(), user);
        return true;
    }
    @Override
    public User login(User user) {
    
        User user_ = users.get(user.getUserId());
        if (user_ == null) {
            return null;
        }
        if (!user_.getPasswd().equals(user.getPasswd())) {
            return null;
        }
        return user_;
    }
}

@Test
public void test_Login() throws NoSuchBeanDefinitionException {

    try (ApplicationContext applicationContext = new StandardApplicationContext("","")) {
        UserService bean = applicationContext.getBean(UserServiceImpl.class);
        User user = new User();
        user.setPasswd("666");
        user.setUserId("666");
        
        long start = System.currentTimeMillis();
        User login = bean.login(user);
        log.debug("{}ms", System.currentTimeMillis() - start);
        log.debug("Result:[{}]", login);
        log.debug("{}ms", System.currentTimeMillis() - start);
    }
}
```

### 3.0版本

#### 使用```ProxyFactoryBean```

```java
/**
 * @author TODAY 2021/2/20 21:26
 */
public class ProxyFactoryBeanTests {
  static final Logger log = LoggerFactory.getLogger(ProxyFactoryBeanTests.class);

  //需要实现接口，确定哪个通知，及告诉Aop应该执行哪个方法
  @Singleton
  static class MyAspect implements MethodInterceptor {
    public Object invoke(MethodInvocation mi) throws Throwable {
      log.debug("方法执行之前");
      // 0
      Object obj = mi.proceed();

      // test == 0

      final TargetBean obj1 = (TargetBean) mi.getThis();
      obj1.test = 10;
      obj = mi.proceed(); // toString

      log.debug("方法执行之后");
      return obj;
    }
  }

  @Singleton
  static class MyAfterReturning implements AfterReturningAdvice {

    @Override
    public void afterReturning(Object returnValue, MethodInvocation invocation) throws Throwable {
      // test == 0
      log.debug("方法执行之后 返回值： " + returnValue);
    }
  }

  @Singleton
  static class MyBefore implements MethodBeforeAdvice {

    @Override
    public void before(MethodInvocation invocation) throws Throwable {
      log.info("之前");
    }
  }

  @Singleton
  static class MyThrows implements ThrowsAdvice {

    @Override
    public Object afterThrowing(Throwable ex, MethodInvocation invocation) {
      log.info(ex.toString());

      return "异常数据";
    }
  }

  @ToString
  @Singleton
  static class TargetBean {

    int test;

    String throwsTest() {
      int i = 1 / 0;
      return "ok";
    }

  }

  @Test
  public void test() {

    try (StandardApplicationContext context = new StandardApplicationContext("", "cn.taketoday.aop.support")) {
      final ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
      proxyFactoryBean.setProxyTargetClass(true);
      proxyFactoryBean.setBeanFactory(context);
      proxyFactoryBean.setExposeProxy(true);

      proxyFactoryBean.setInterceptorNames("myAspect", "myAfterReturning", "myBefore", "myThrows");
      proxyFactoryBean.setTargetName("targetBean");

      final Object bean = proxyFactoryBean.getBean();
      log.debug(bean.toString());

      final String ret = ((TargetBean) bean).throwsTest();

      assert ret.equals("异常数据");
    }
  }
}
```

#### 类似Spring Aop的使用方法

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Aware { }

static class PrinterBean {

  @Aware
  void print() {
    System.out.println("print");
  }

  void none() {
    System.out.println("none");
  }

  void none(String arg) {
    System.out.println("none" + arg);
  }

  @Aware
  int none(Integer input) {
    System.out.println("none" + input);
    return input;
  }

}

static class LoggingInterceptor implements MethodInterceptor {

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    log.debug("LoggingInterceptor @Around Before method");
    final Object proceed = invocation.proceed();
    log.debug("LoggingInterceptor @Around After method");
    return proceed;
  }

}

static class MyInterceptor implements MethodInterceptor {

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    final Object proceed = invocation.proceed();
    final Object aThis = invocation.getThis();
    System.out.println(aThis);
    return proceed;
  }

}

@Import({ LoggingInterceptor.class, MyInterceptor.class })
static class LoggingConfig {

  @Singleton
  public DefaultPointcutAdvisor loggingAdvisor(LoggingInterceptor loggingAspect) {
    AnnotationMatchingPointcut pointcut = new AnnotationMatchingPointcut(null, Aware.class);

    DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor();
    advisor.setPointcut(pointcut);
    advisor.setAdvice(loggingAspect);

    return advisor;
  }

  @Singleton
  public DefaultPointcutAdvisor advisor(MyInterceptor interceptor) {
    AnnotationMatchingPointcut pointcut = new AnnotationMatchingPointcut(null, Aware.class);

    DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor();
    advisor.setPointcut(pointcut);
    advisor.setAdvice(interceptor);

    return advisor;
  }

}

@Test
public void testNewVersionAop() throws Throwable {

  try (StandardApplicationContext context = new StandardApplicationContext()) {

    final TargetSourceCreator targetSourceCreator = new TargetSourceCreator() {

      @Override
      public TargetSource getTargetSource(BeanDefinition def) {

        return new PrototypeTargetSource() {

          @Override
          public Class<?> getTargetClass() {
            return PrinterBean.class;
          }

          @Override
          protected Object newPrototypeInstance() {
            return new PrinterBean();
          }
        };
      }
    };

    final StandardBeanFactory beanFactory = context.getBeanFactory();
    final DefaultAutoProxyCreator autoProxyCreator = new DefaultAutoProxyCreator();
    context.addBeanPostProcessor(autoProxyCreator);
    autoProxyCreator.setBeanFactory(beanFactory);
    autoProxyCreator.setFrozen(true);
    autoProxyCreator.setExposeProxy(true);

    beanFactory.importBeans(LoggingConfig.class, PrinterBean.class);

    final PrinterBean bean = beanFactory.getBean(PrinterBean.class);
    final DefaultPointcutAdvisor pointcutAdvisor = beanFactory.getBean(DefaultPointcutAdvisor.class);
    System.out.println(pointcutAdvisor);

    bean.print();

    bean.none();
    bean.none("TODAY");
    final int none = bean.none(1);
    assertThat(none).isEqualTo(1);

    System.out.println(none);

//      Advised advised = (Advised) bean;
//      System.out.println(Arrays.toString(advised.getAdvisors()));

  }
}

```


## 🙏 鸣谢
本项目的诞生离不开以下项目：
* [Slf4j](https://github.com/qos-ch/slf4j): Simple Logging Facade for Java
* [Spring](https://github.com/spring-projects/spring-framework): Spring Framework
* [Lombok](https://github.com/rzwitserloot/lombok): Very spicy additions to the Java programming language
* [Redisson](https://github.com/redisson/redisson): Redisson - Redis Java client with features of In-Memory Data Grid

## 📄 开源协议

Today Context 使用 [GNU GENERAL PUBLIC LICENSE](https://github.com/TAKETODAY/today-context/blob/master/LICENSE) 开源协议

