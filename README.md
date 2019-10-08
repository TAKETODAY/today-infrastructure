# TODAY Context

:apple: today-context is a lightweight dependency injection framework.

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

## :tada: 前言

today-web 框架2.0刚出来时没有 ioc 容器感觉不是很方便，所以想自己实现一个。之前有看过Spring源码但是发现我对Spring源码无从下手😰完全懵逼。之前学过怎么用Spring但是对他的底层完全不了解的我带着试一试的心态开始到处查资料，就这样我又开始造起了轮子。**如何扫描类文件**、**学习Java注解**、**Java字节码**、**动态代理**、**重新认识接口**、**一些设计模式**、**学习使用Git**、**渐渐明白了单元测试的重要性** 等。随着学习的深入框架经历了数次重构，自己也对依赖注入有了自己的看法。慢慢的我发现我居然能看得明白Spring源码了。感觉Spring真心强大😮👍 慢慢的我感觉他并不是轻量级的🙏 。如果他说他是轻量级，那我的就是超轻量级😄 。自己在造轮子的过程中学习到了很多知识，越学感觉自己越空，觉得Java是越学越多，永远都学不完。


## :memo: 使用说明

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

        return field.isAnnotationPresent(Autowired.class) //
                || field.isAnnotationPresent(Resource.class) //
                || (NAMED_CLASS != null && field.isAnnotationPresent(NAMED_CLASS))//
                || (INJECT_CLASS != null && field.isAnnotationPresent(INJECT_CLASS));
    }
    @Override
    public PropertyValue resolveProperty(Field field) {
        final ApplicationContext applicationContext = ContextUtils.getApplicationContext();
        final BeanNameCreator beanNameCreator = applicationContext.getEnvironment().getBeanNameCreator();

        final Autowired autowired = field.getAnnotation(Autowired.class); // auto wired

        String name = null;
        boolean required = true;
        final Class<?> propertyClass = field.getType();

        if (autowired != null) {
            name = autowired.value();
            if (StringUtils.isEmpty(name)) {
                name = byType(applicationContext, propertyClass, beanNameCreator);
            }
            required = autowired.required();
        }
        else if (field.isAnnotationPresent(Resource.class)) {
            // Resource.class
            final Resource resource = field.getAnnotation(Resource.class);
            name = resource.name();
            if (StringUtils.isEmpty(name)) { // fix resource.type() != Object.class) {
                name = byType(applicationContext, propertyClass, beanNameCreator);
            }
        }
        else if (NAMED_CLASS != null) {// @Named
            final Collection<AnnotationAttributes> annotationAttributes = //
                    ClassUtils.getAnnotationAttributes(field, NAMED_CLASS); // @Named

            if (annotationAttributes.isEmpty()) {
                name = byType(applicationContext, propertyClass, beanNameCreator);
            }
            else {
                name = annotationAttributes.iterator().next().getString(Constant.VALUE); // name attr
            }
        }
        else {// @Inject
            name = byType(applicationContext, propertyClass, beanNameCreator);
        }
        return new PropertyValue(new BeanReference(name, required, propertyClass), field);
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
        return "DispatcherServlet, Copyright © TODAY & 2017 - 2019 All Rights Reserved";
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

    /**
     * Use {@link afterPropertiesSet}
     * 
     * @since 2.3.3
     */
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

    /**
     * Create Model Attributes.
     * 
     * @param requestContext
     *            Current request context
     * @return {@link TemplateHashModel}
     */
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
public class LifecycleBean //
        implements DisposableBean, BeanNameAware,
        InitializingBean, BeanFactoryAware,
        EnvironmentAware, ApplicationContextAware {

    @Override
    public void setBeanName(String name) {
        log.info("setBeanName: {}", name);
    }

    @Override
    public void setEnvironment(Environment environment) {
        log.info("setEnvironment: {}", environment);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        log.info("setBeanFactory: {}", beanFactory);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        log.info("setApplicationContext: {}", applicationContext);
    }

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
        ClassUtils.setClassCache(beans);
        try (final ApplicationContext applicationContext = new StandardApplicationContext("info.properties")) {

            applicationContext.loadContext(beans);

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
2019-07-25 23:14:37.930  INFO - [            main] test.context.LifecycleBean                 64 - setEnvironment: cn.taketoday.context.env.StandardEnvironment@5ec0a365
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
    "propertyValues":"[]",
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

## 🙏 鸣谢
本项目的诞生离不开以下项目：
* [Spring](https://github.com/spring-projects/spring-framework): Spring Framework
* [Cglib](https://github.com/cglib/cglib): Byte Code Generation Library
* [Slf4j](https://github.com/qos-ch/slf4j): Simple Logging Facade for Java
* [EL](https://github.com/TAKETODAY/today-expression): Java Unified Expression Language
* [Lombok](https://github.com/rzwitserloot/lombok): Very spicy additions to the Java programming language


## 📄 开源协议

Today Context 使用 [GNU GENERAL PUBLIC LICENSE](https://github.com/TAKETODAY/today-context/blob/master/LICENSE) 开源协议

