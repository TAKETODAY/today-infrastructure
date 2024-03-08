/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.framework;

import org.crac.management.CRaCMXBean;

import java.lang.StackWalker.StackFrame;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.aot.AotDetector;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.CachedIntrospectionResults;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.AbstractAutowireCapableBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.BeanNameGenerator;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigUtils;
import cn.taketoday.context.annotation.ConfigurationClassPostProcessor;
import cn.taketoday.context.aot.AotApplicationContextInitializer;
import cn.taketoday.context.event.ApplicationContextEvent;
import cn.taketoday.context.event.ContextClosedEvent;
import cn.taketoday.context.event.ContextRefreshedEvent;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.BindableRuntimeHintsRegistrar;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertySources;
import cn.taketoday.context.support.AbstractApplicationContext;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.env.CommandLinePropertySource;
import cn.taketoday.core.env.CompositePropertySource;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.env.SimpleCommandLinePropertySource;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.format.support.ApplicationConversionService;
import cn.taketoday.framework.BootstrapRegistry.InstanceSupplier;
import cn.taketoday.framework.builder.ApplicationBuilder;
import cn.taketoday.framework.diagnostics.ApplicationExceptionReporter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.Instantiator;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.util.function.ThrowingConsumer;
import cn.taketoday.util.function.ThrowingSupplier;

/**
 * Class that can be used to bootstrap and launch a application from a Java main
 * method. By default, class will perform the following steps to bootstrap your
 * application:
 *
 * <ul>
 * <li>Create an appropriate {@link ApplicationContext} instance (depending on your
 * classpath)</li>
 * <li>Register a {@link CommandLinePropertySource} to expose command line arguments as
 * properties</li>
 * <li>Refresh the application context, loading all singleton beans</li>
 * <li>Trigger any {@link CommandLineRunner} beans</li>
 * </ul>
 *
 * In most circumstances the static {@link #run(Class, String[])} method can be called
 * directly from your {@literal main} method to bootstrap your application:
 *
 * <pre>{@code
 * @Configuration
 * public class MyApplication  {
 *
 *   // ... Bean definitions
 *
 *   public static void main(String[] args) {
 *     // Application.run(MyApplication.class, args);
 *     WebApplication.run(MyApplication.class, args);
 *   }
 * }
 * }</pre>
 *
 * <p>
 * For more advanced configuration a {@link Application} instance can be created and
 * customized before being run:
 *
 * <pre> {@code
 * public static void main(String[] args) {
 *   Application application = new Application(MyApplication.class);
 *   // ... customize application settings here
 *   application.run(args)
 * }
 * }</pre>
 *
 * {@link Application}s can read beans from a {@code mainApplicationClass} or {@code configSources}
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Jeremy Rickard
 * @author Craig Burke
 * @author Michael Simons
 * @author Madhura Bhave
 * @author Brian Clozel
 * @author Ethan Rubinson
 * @author Chris Bono
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #run(Class, String[])
 * @see #Application(Class...)
 * @since 4.0 2021/10/5 23:49
 */
public class Application {

  public static final String PROPERTIES_BINDER_PREFIX = "app.main";

  private static final String SYSTEM_PROPERTY_JAVA_AWT_HEADLESS = "java.awt.headless";

  static final ApplicationShutdownHook shutdownHook = new ApplicationShutdownHook();

  private static final ThreadLocal<ApplicationHook> applicationHook = new ThreadLocal<>();

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final Set<Class<?>> primarySources;

  private final ApplicationTemp applicationTemp;

  private final List<BootstrapRegistryInitializer> bootstrapRegistryInitializers;

  @Nullable
  private Class<?> mainApplicationClass;

  private List<ApplicationContextInitializer> initializers;

  private ApplicationContextFactory applicationContextFactory = ApplicationContextFactory.DEFAULT;

  private ApplicationType applicationType;

  @Nullable
  private ConfigurableEnvironment environment;

  @Nullable
  private Map<String, Object> defaultProperties;

  private Set<String> additionalProfiles = Collections.emptySet();

  @Nullable
  private ResourceLoader resourceLoader;

  @Nullable
  private BeanNameGenerator beanNameGenerator;

  private Set<String> sources = new LinkedHashSet<>();

  @Nullable
  private String environmentPrefix;

  private List<ApplicationListener<?>> listeners;

  @Nullable
  private List<ApplicationStartupListener> startupListeners;

  @Nullable
  private Banner banner;

  private Banner.Mode bannerMode = Banner.Mode.CONSOLE;

  private boolean headless = true;
  private boolean logStartupInfo = true;
  private boolean registerShutdownHook = true;
  private boolean addCommandLineProperties = true;

  private boolean addConversionService = true;

  private boolean allowBeanDefinitionOverriding;

  private boolean allowCircularReferences;
  private boolean lazyInitialization = false;
  private boolean isCustomEnvironment = false;

  private boolean keepAlive;

  /**
   * Create a new {@link Application} instance. The application context will load
   * beans from the specified primary sources (see {@link Application class-level}
   * documentation for details). The instance can be customized before calling
   * {@link #run(String...)}.
   *
   * @param primarySources the primary bean sources
   * @see #run(Class, String[])
   * @see #Application(ResourceLoader, Class...)
   * @see #setSources(Set)
   */
  public Application(Class<?>... primarySources) {
    this(null, primarySources);
  }

  /**
   * Create a new {@link Application} instance. The application context will load
   * beans from the specified primary sources (see {@link Application class-level}
   * documentation for details). The instance can be customized before calling
   * {@link #run(String...)}.
   *
   * @param resourceLoader the resource loader to use
   * @param primarySources the primary bean sources
   * @see #run(Class, String[])
   * @see #setSources(Set)
   */
  public Application(@Nullable ResourceLoader resourceLoader, Class<?>... primarySources) {
    Assert.notNull(primarySources, "PrimarySources is required");
    this.resourceLoader = resourceLoader;
    this.primarySources = CollectionUtils.newLinkedHashSet(primarySources);
    this.applicationType = ApplicationType.forClasspath();
    this.mainApplicationClass = deduceMainApplicationClass();
    this.applicationTemp = new ApplicationTemp(mainApplicationClass);
    this.bootstrapRegistryInitializers = TodayStrategies.find(BootstrapRegistryInitializer.class);
    setInitializers(TodayStrategies.find(ApplicationContextInitializer.class));
    setListeners(TodayStrategies.find(ApplicationListener.class));
  }

  @Nullable
  private Class<?> deduceMainApplicationClass() {
    return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
            .walk(this::getMainApplicationClass);
  }

  @Nullable
  private Class<?> getMainApplicationClass(Stream<StackFrame> stackFrame) {
    return stackFrame.filter(s -> "main".equals(s.getMethodName()))
            .map(StackFrame::getDeclaringClass)
            .findFirst()
            .orElse(null);
  }

  /**
   * Returns the main application class that has been deduced or explicitly configured.
   *
   * @return the main application class or {@code null}
   */
  @Nullable
  public Class<?> getMainApplicationClass() {
    return this.mainApplicationClass;
  }

  /**
   * Application temp
   */
  public ApplicationTemp getApplicationTemp() {
    return applicationTemp;
  }

  /**
   * Set a specific main application class that will be used as a log source and to
   * obtain version information. By default the main application class will be deduced.
   * Can be set to {@code null} if there is no explicit application class.
   *
   * @param mainApplicationClass the mainApplicationClass to set or {@code null}
   */
  public void setMainApplicationClass(@Nullable Class<?> mainApplicationClass) {
    this.mainApplicationClass = mainApplicationClass;
  }

  /**
   * Returns the type of application that is being run.
   *
   * @return the type of application
   */
  public ApplicationType getApplicationType() {
    return applicationType;
  }

  /**
   * Sets the type of web application to be run. If not explicitly set the type of web
   * application will be deduced based on the classpath.
   *
   * @param applicationType the application type
   */
  public void setApplicationType(ApplicationType applicationType) {
    Assert.notNull(applicationType, "ApplicationType is required");
    this.applicationType = applicationType;
  }

  /**
   * Run the application, creating and refreshing a new
   * {@link ApplicationContext}.
   *
   * @param args the application arguments (usually passed from a Java main method)
   * @return a running {@link ApplicationContext}
   */
  public ConfigurableApplicationContext run(String... args) {
    Startup startup = Startup.create();
    if (this.registerShutdownHook) {
      Application.shutdownHook.enableShutdownHookAddition();
    }
    ApplicationArguments arguments = new ApplicationArguments(args);
    DefaultBootstrapContext bootstrapContext = createBootstrapContext();

    bootstrapContext.register(Application.class, InstanceSupplier.of(this));
    bootstrapContext.register(ApplicationTemp.class, InstanceSupplier.of(applicationTemp));
    bootstrapContext.register(ApplicationArguments.class, InstanceSupplier.of(arguments));

    // prepare startup
    prepareStartup(arguments);

    ConfigurableApplicationContext context = null;
    ApplicationStartupListeners listeners = getStartupListeners(bootstrapContext, arguments);
    listeners.starting(bootstrapContext, mainApplicationClass, arguments);
    try {
      ConfigurableEnvironment environment = prepareEnvironment(bootstrapContext, listeners, arguments);
      configureIgnoreBeanInfo(environment);

      Banner printedBanner = printBanner(environment);

      context = createApplicationContext();
      // prepare context
      prepareContext(bootstrapContext, context, listeners, arguments, environment, printedBanner);
      // refresh context
      refreshContext(context);
      // after refresh
      afterRefresh(context, arguments);
      Duration timeTakenToStarted = startup.started();
      if (this.logStartupInfo) {
        new StartupLogging(this.mainApplicationClass).logStarted(getApplicationLog(), startup);
      }
      listeners.started(context, timeTakenToStarted);
      callRunners(context, arguments);
    }
    catch (Throwable e) {
      throw handleRunFailure(context, e, listeners);
    }
    try {
      if (context.isRunning()) {
        listeners.ready(context, startup.ready());
      }
      return context;
    }
    catch (Throwable ex) {
      throw handleRunFailure(context, ex, null);
    }
  }

  private DefaultBootstrapContext createBootstrapContext() {
    DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();
    for (BootstrapRegistryInitializer initializer : bootstrapRegistryInitializers) {
      initializer.initialize(bootstrapContext);
    }
    return bootstrapContext;
  }

  private void refreshContext(ConfigurableApplicationContext context) {
    if (this.registerShutdownHook) {
      shutdownHook.registerApplicationContext(context);
    }
    refresh(context);
  }

  /**
   * Refresh the underlying {@link ApplicationContext}.
   *
   * @param context the application context to refresh
   */
  protected void refresh(ConfigurableApplicationContext context) {
    context.refresh();
  }

  /**
   * Called after the context has been refreshed.
   *
   * @param context the application context
   * @param args the application arguments
   */
  protected void afterRefresh(ConfigurableApplicationContext context, ApplicationArguments args) { }

  /**
   * find all ApplicationStartupListeners
   * <p>
   * from TodayStrategies, {@link #startupListeners} ,{@link ApplicationHook}
   */
  private ApplicationStartupListeners getStartupListeners(BootstrapContext bootstrapContext, ApplicationArguments arguments) {
    var instantiator = new Instantiator<ApplicationStartupListener>(ApplicationStartupListener.class,
            parameters -> {
              parameters.add(Application.class, this);
              parameters.add(Logger.class, getApplicationLog());
              parameters.add(ApplicationArguments.class, arguments);
              parameters.add(ApplicationTemp.class, applicationTemp);
              parameters.add(String[].class, arguments.getSourceArgs());
              parameters.add(BootstrapRegistry.class, bootstrapContext);
              parameters.add(ConfigurableBootstrapContext.class, bootstrapContext);
            });

    List<String> strategiesNames = TodayStrategies.findNames(ApplicationStartupListener.class, getClassLoader());
    List<ApplicationStartupListener> strategies = instantiator.instantiate(strategiesNames);

    // user defined
    if (CollectionUtils.isNotEmpty(startupListeners)) {
      strategies.addAll(startupListeners);
      AnnotationAwareOrderComparator.sort(strategies);
    }

    ApplicationHook hook = applicationHook.get();
    if (hook != null) {
      ApplicationStartupListener hookListener = hook.getStartupListener(this);
      if (hookListener != null) {
        strategies.add(hookListener);
      }
    }

    return new ApplicationStartupListeners(logger, strategies);
  }

  private ConfigurableEnvironment prepareEnvironment(ConfigurableBootstrapContext context,
          ApplicationStartupListeners listeners, ApplicationArguments applicationArguments) {
    // Create and configure the environment
    ConfigurableEnvironment environment = getOrCreateEnvironment();
    configureEnvironment(environment, applicationArguments.getSourceArgs());

    ConfigurationPropertySources.attach(environment);
    listeners.environmentPrepared(context, environment);
    DefaultPropertiesPropertySource.moveToEnd(environment);

    Assert.state(!environment.containsProperty("app.main.environment-prefix"),
            "Environment prefix cannot be set via properties.");

    bindToApplication(environment);

    if (!this.isCustomEnvironment) {
      environment = EnvironmentConverter.convertIfNecessary(getClassLoader(), environment, switch (applicationType) {
        case NETTY_WEB -> ApplicationNettyWebEnvironment.class;
        case SERVLET_WEB -> ApplicationServletEnvironment.class;
        case REACTIVE_WEB -> ApplicationReactiveWebEnvironment.class;
        default -> ApplicationEnvironment.class;
      });
    }

    ConfigurationPropertySources.attach(environment);
    return environment;
  }

  /**
   * Bind the environment to the {@link Application}.
   *
   * @param environment the environment to bind
   */
  protected void bindToApplication(ConfigurableEnvironment environment) {
    try {
      Binder.get(environment).bind(PROPERTIES_BINDER_PREFIX, Bindable.ofInstance(this));
    }
    catch (Exception ex) {
      throw new IllegalStateException("Cannot bind to Application", ex);
    }
  }

  private void configureIgnoreBeanInfo(ConfigurableEnvironment environment) {
    if (System.getProperty(CachedIntrospectionResults.IGNORE_BEANINFO_PROPERTY_NAME) == null) {
      boolean ignore = environment.getFlag(CachedIntrospectionResults.IGNORE_BEANINFO_PROPERTY_NAME, true);
      System.setProperty(CachedIntrospectionResults.IGNORE_BEANINFO_PROPERTY_NAME, Boolean.toString(ignore));
    }
  }

  @Nullable
  private Banner printBanner(ConfigurableEnvironment environment) {
    if (this.bannerMode == Banner.Mode.OFF) {
      return null;
    }
    ResourceLoader resourceLoader = this.resourceLoader;
    if (resourceLoader == null) {
      resourceLoader = new DefaultResourceLoader(null);
    }

    var bannerPrinter = new InfraBannerPrinter(resourceLoader, banner);
    if (this.bannerMode == Banner.Mode.LOG) {
      return bannerPrinter.print(environment, mainApplicationClass, logger);
    }
    return bannerPrinter.print(environment, mainApplicationClass, System.out);
  }

  /**
   * Strategy method used to create the {@link ApplicationContext}. By default this
   * method will respect any explicitly set application context class or factory before
   * falling back to a suitable default.
   *
   * @return the application context (not yet refreshed)
   * @see #setApplicationContextFactory(ApplicationContextFactory)
   */
  protected ConfigurableApplicationContext createApplicationContext() {
    ConfigurableApplicationContext context = applicationContextFactory.create(applicationType);
    Assert.state(context != null, "No suitable ConfigurableApplicationContext");
    return context;
  }

  protected void prepareStartup(ApplicationArguments arguments) {
    configureHeadlessProperty();
  }

  private void prepareContext(DefaultBootstrapContext bootstrapContext,
          ConfigurableApplicationContext context, ApplicationStartupListeners listeners,
          ApplicationArguments arguments, ConfigurableEnvironment environment, @Nullable Banner printedBanner) //
  {
    context.setEnvironment(environment);
    postProcessApplicationContext(context);
    addAotGeneratedInitializerIfNecessary(initializers);
    applyInitializers(context);
    listeners.contextPrepared(context);
    bootstrapContext.close(context);

    if (logStartupInfo) {
      logStartupInfo(context.getParent() == null);
      logStartupProfileInfo(context);
    }

    // Add specific singleton beans
    ConfigurableBeanFactory beanFactory = context.getBeanFactory();
    beanFactory.registerSingleton(this);
    beanFactory.registerSingleton(applicationTemp);
    beanFactory.registerSingleton(ApplicationArguments.BEAN_NAME, arguments);

    if (printedBanner != null) {
      beanFactory.registerSingleton(Banner.BEAN_NAME, printedBanner);
    }

    if (beanFactory instanceof AbstractAutowireCapableBeanFactory acBeanFactory) {
      acBeanFactory.setAllowCircularReferences(allowCircularReferences);
      if (beanFactory instanceof StandardBeanFactory stdBeanFactory) {
        stdBeanFactory.setAllowBeanDefinitionOverriding(allowBeanDefinitionOverriding);
      }
    }

    if (lazyInitialization) {
      context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor());
    }

    if (keepAlive) {
      context.addApplicationListener(new KeepAlive());
    }

    if (CollectionUtils.isNotEmpty(defaultProperties)) {
      context.addBeanFactoryPostProcessor(new PropertySourceOrderingBeanFactoryPostProcessor(context));
    }

    if (!AotDetector.useGeneratedArtifacts()) {
      // Load the sources
      Set<Object> sources = getAllSources();
      Assert.notEmpty(sources, "Sources must not be empty");
      load(context, sources.toArray());
    }

    listeners.contextLoaded(context);
  }

  private void addAotGeneratedInitializerIfNecessary(List<ApplicationContextInitializer> initializers) {
    if (AotDetector.useGeneratedArtifacts()) {
      List<ApplicationContextInitializer> aotInitializers = new ArrayList<>(
              initializers.stream().filter(AotApplicationContextInitializer.class::isInstance).toList());
      if (aotInitializers.isEmpty()) {
        Assert.state(mainApplicationClass != null, "No mainApplicationClass");
        String initializerClassName = mainApplicationClass.getName() + "__ApplicationContextInitializer";
        Assert.state(ClassUtils.isPresent(initializerClassName, getClassLoader()),
                "You are starting the application with AOT mode enabled but AOT processing hasn't happened. "
                        + "Please build your application with enabled AOT processing first, "
                        + "or remove the system property 'infra.aot.enabled' to run the application in regular mode");
        aotInitializers.add(AotApplicationContextInitializer.forInitializerClasses(initializerClassName));
      }
      initializers.removeAll(aotInitializers);
      initializers.addAll(0, aotInitializers);
    }
  }

  /**
   * Load beans into the application context.
   *
   * @param context the context to load beans into
   * @param sources the sources to load
   */
  protected void load(ApplicationContext context, Object[] sources) {
    if (logger.isDebugEnabled()) {
      logger.debug("Loading source {}", StringUtils.arrayToCommaDelimitedString(sources));
    }
    ApplicationBeanDefinitionLoader loader = createBeanDefinitionLoader(getBeanDefinitionRegistry(context), sources);
    if (this.beanNameGenerator != null) {
      loader.setBeanNameGenerator(this.beanNameGenerator);
    }
    if (this.resourceLoader != null) {
      loader.setResourceLoader(this.resourceLoader);
    }
    if (this.environment != null) {
      loader.setEnvironment(this.environment);
    }
    loader.load();
  }

  /**
   * Get the bean definition registry.
   *
   * @param context the application context
   * @return the BeanDefinitionRegistry if it can be determined
   */
  private BeanDefinitionRegistry getBeanDefinitionRegistry(ApplicationContext context) {
    if (context instanceof BeanDefinitionRegistry) {
      return (BeanDefinitionRegistry) context;
    }
    if (context instanceof AbstractApplicationContext) {
      return (BeanDefinitionRegistry) ((AbstractApplicationContext) context).getBeanFactory();
    }
    throw new IllegalStateException("Could not locate BeanDefinitionRegistry");
  }

  /**
   * Factory method used to create the {@link ApplicationBeanDefinitionLoader}.
   *
   * @param registry the bean definition registry
   * @param sources the sources to load
   * @return the {@link ApplicationBeanDefinitionLoader} that will be used to load beans
   */
  protected ApplicationBeanDefinitionLoader createBeanDefinitionLoader(BeanDefinitionRegistry registry, Object[] sources) {
    return new ApplicationBeanDefinitionLoader(registry, sources);
  }

  /**
   * Apply any relevant post-processing the {@link ApplicationContext}. Subclasses can
   * apply additional processing as required.
   *
   * @param context the application context
   */
  protected void postProcessApplicationContext(ConfigurableApplicationContext context) {
    if (this.beanNameGenerator != null) {
      context.getBeanFactory().registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR,
              this.beanNameGenerator);
    }
    if (this.resourceLoader != null) {
      if (context instanceof GenericApplicationContext) {
        ((GenericApplicationContext) context).setResourceLoader(this.resourceLoader);
      }
      if (context instanceof DefaultResourceLoader) {
        ((DefaultResourceLoader) context).setClassLoader(this.resourceLoader.getClassLoader());
      }
    }
    if (this.addConversionService) {
      context.getBeanFactory().setConversionService(context.getEnvironment().getConversionService());
    }
  }

  private ConfigurableEnvironment getOrCreateEnvironment() {
    if (this.environment != null) {
      return this.environment;
    }
    return switch (applicationType) {
      case NETTY_WEB -> new ApplicationNettyWebEnvironment();
      case SERVLET_WEB -> new ApplicationServletEnvironment();
      case REACTIVE_WEB -> new ApplicationReactiveWebEnvironment();
      default -> new ApplicationEnvironment();
    };
  }

  /**
   * Template method delegating to
   * {@link #configurePropertySources(ConfigurableEnvironment, String[])} and
   * {@link #configureProfiles(ConfigurableEnvironment, String[])} in that order.
   * Override this method for complete control over Environment customization, or one of
   * the above for fine-grained control over property sources or profiles, respectively.
   *
   * @param environment this application's environment
   * @param args arguments passed to the {@code run} method
   * @see #configureProfiles(ConfigurableEnvironment, String[])
   * @see #configurePropertySources(ConfigurableEnvironment, String[])
   */
  protected void configureEnvironment(ConfigurableEnvironment environment, String[] args) {
    if (this.addConversionService) {
      environment.setConversionService(new ApplicationConversionService());
    }
    configurePropertySources(environment, args);
    configureProfiles(environment, args);
  }

  /**
   * Configure which profiles are active (or active by default) for this application
   * environment. Additional profiles may be activated during configuration file
   * processing via the {@code infra.profiles.active} property.
   *
   * @param environment this application's environment
   * @param args arguments passed to the {@code run} method
   * @see #configureEnvironment(ConfigurableEnvironment, String[])
   */
  protected void configureProfiles(ConfigurableEnvironment environment, String[] args) { }

  /**
   * Add, remove or re-order any {@link PropertySource}s in this application's
   * environment.
   *
   * @param environment this application's environment
   * @param args arguments passed to the {@code run} method
   * @see #configureEnvironment(ConfigurableEnvironment, String[])
   */
  protected void configurePropertySources(ConfigurableEnvironment environment, String[] args) {
    PropertySources sources = environment.getPropertySources();
    if (CollectionUtils.isNotEmpty(defaultProperties)) {
      DefaultPropertiesPropertySource.addOrMerge(defaultProperties, sources);
    }

    // CommandLine
    if (addCommandLineProperties && args.length > 0) {
      String name = CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME;
      if (sources.contains(name)) {
        PropertySource<?> source = sources.get(name);
        CompositePropertySource composite = new CompositePropertySource(name);
        composite.addPropertySource(new SimpleCommandLinePropertySource("applicationCommandLineArgs", args));
        composite.addPropertySource(source);
        sources.replace(name, composite);
      }
      else {
        sources.addFirst(new SimpleCommandLinePropertySource(args));
      }
    }
  }

  /**
   * Sets the factory that will be called to create the application context.
   *
   * @param applicationContextFactory the factory for the context
   */
  public void setApplicationContextFactory(@Nullable ApplicationContextFactory applicationContextFactory) {
    this.applicationContextFactory = applicationContextFactory != null
            ? applicationContextFactory : ApplicationContextFactory.DEFAULT;
  }

  /**
   * Sets the {@link ApplicationContextInitializer} that will be applied to the
   * {@link ApplicationContext}.
   *
   * @param initializers the initializers to set
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void setInitializers(Collection<ApplicationContextInitializer> initializers) {
    this.initializers = new ArrayList(initializers);
  }

  /**
   * Add {@link ApplicationContextInitializer}s to be applied to the
   * {@link ApplicationContext}.
   *
   * @param initializers the initializers to add
   */
  public void addInitializers(ApplicationContextInitializer... initializers) {
    CollectionUtils.addAll(this.initializers, initializers);
  }

  /**
   * Returns read-only ordered Set of the {@link ApplicationContextInitializer}s that
   * will be applied to the {@link ApplicationContext}.
   *
   * @return the initializers
   */
  public Set<ApplicationContextInitializer> getInitializers() {
    return asUnmodifiableOrderedSet(this.initializers);
  }

  /**
   * Apply any {@link ApplicationContextInitializer}s to the context before it is
   * refreshed.
   *
   * @param context the configured ApplicationContext (not refreshed yet)
   * @see ConfigurableApplicationContext#refresh()
   */
  protected void applyInitializers(ConfigurableApplicationContext context) {
    for (ApplicationContextInitializer initializer : getInitializers()) {
      initializer.initialize(context);
    }
  }

  /**
   * Called to log startup information, subclasses may override to add additional
   * logging.
   *
   * @param isRoot true if this application is the root of a context hierarchy
   */
  protected void logStartupInfo(boolean isRoot) {
    if (isRoot) {
      new StartupLogging(this.mainApplicationClass).logStarting(getApplicationLog());
    }
  }

  /**
   * Called to log active profile information.
   *
   * @param context the application context
   */
  protected void logStartupProfileInfo(ConfigurableApplicationContext context) {
    Logger log = getApplicationLog();
    if (log.isInfoEnabled()) {
      List<String> activeProfiles = quoteProfiles(context.getEnvironment().getActiveProfiles());
      if (ObjectUtils.isEmpty(activeProfiles)) {
        List<String> defaultProfiles = quoteProfiles(context.getEnvironment().getDefaultProfiles());
        String message = String.format("%s default %s: ", defaultProfiles.size(),
                (defaultProfiles.size() <= 1) ? "profile" : "profiles");
        log.info("No active profile set, falling back to " + message
                + StringUtils.collectionToDelimitedString(defaultProfiles, ", "));
      }
      else {
        String message = (activeProfiles.size() == 1) ? "1 profile is active: "
                : activeProfiles.size() + " profiles are active: ";
        log.info("The following " + message + StringUtils.collectionToDelimitedString(activeProfiles, ", "));
      }
    }
  }

  private List<String> quoteProfiles(String[] profiles) {
    return Arrays.stream(profiles).map((profile) -> "\"" + profile + "\"").collect(Collectors.toList());
  }

  /**
   * Returns the {@link Logger} for the application. By default will be deduced.
   *
   * @return the application log
   */
  public Logger getApplicationLog() {
    if (this.mainApplicationClass == null) {
      return logger;
    }
    return LoggerFactory.getLogger(this.mainApplicationClass);
  }

  /**
   * Sets if the application is headless and should not instantiate AWT. Defaults to
   * {@code true} to prevent java icons appearing.
   *
   * @param headless if the application is headless
   */
  public void setHeadless(boolean headless) {
    this.headless = headless;
  }

  /**
   * Sets if the created {@link ApplicationContext} should have a shutdown hook
   * registered. Defaults to {@code true} to ensure that JVM shutdowns are handled
   * gracefully.
   *
   * @param registerShutdownHook if the shutdown hook should be registered
   */
  public void setRegisterShutdownHook(boolean registerShutdownHook) {
    this.registerShutdownHook = registerShutdownHook;
  }

  private void configureHeadlessProperty() {
    System.setProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, System.getProperty(
            SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, Boolean.toString(this.headless)));
  }

  /**
   * Sets the underlying environment that should be used with the created application
   * context.
   *
   * @param environment the environment
   */
  public void setEnvironment(@Nullable ConfigurableEnvironment environment) {
    this.isCustomEnvironment = true;
    this.environment = environment;
  }

  /**
   * Add additional items to the primary sources that will be added to an
   * ApplicationContext when {@link #run(String...)} is called.
   * <p>
   * The sources here are added to those that were set in the constructor. Most users
   * should consider using {@link #getSources()}/{@link #setSources(Set)} rather than
   * calling this method.
   *
   * @param additionalPrimarySources the additional primary sources to add
   * @see #Application(Class...)
   * @see #getSources()
   * @see #setSources(Set)
   * @see #getAllSources()
   */
  public void addPrimarySources(Collection<Class<?>> additionalPrimarySources) {
    this.primarySources.addAll(additionalPrimarySources);
  }

  /**
   * Returns a mutable set of the sources that will be added to an ApplicationContext
   * when {@link #run(String...)} is called.
   * <p>
   * Sources set here will be used in addition to any primary sources set in the
   * constructor.
   *
   * @return the application sources.
   * @see #Application(Class...)
   * @see #getAllSources()
   */
  public Set<String> getSources() {
    return this.sources;
  }

  /**
   * Set additional sources that will be used to create an ApplicationContext. A source
   * can be: a class name, package name, or an XML resource location.
   * <p>
   * Sources set here will be used in addition to any primary sources set in the
   * constructor.
   *
   * @param sources the application sources to set
   * @see #Application(Class...)
   * @see #getAllSources()
   */
  public void setSources(Set<String> sources) {
    Assert.notNull(sources, "Sources is required");
    this.sources = new LinkedHashSet<>(sources);
  }

  /**
   * Return an immutable set of all the sources that will be added to an
   * ApplicationContext when {@link #run(String...)} is called. This method combines any
   * primary sources specified in the constructor with any additional ones that have
   * been {@link #setSources(Set) explicitly set}.
   *
   * @return an immutable set of all sources
   */
  public Set<Object> getAllSources() {
    Set<Object> allSources = new LinkedHashSet<>();
    if (CollectionUtils.isNotEmpty(this.primarySources)) {
      allSources.addAll(this.primarySources);
    }
    if (CollectionUtils.isNotEmpty(this.sources)) {
      allSources.addAll(this.sources);
    }
    return Collections.unmodifiableSet(allSources);
  }

  /**
   * Sets the {@link ResourceLoader} that should be used when loading resources.
   *
   * @param resourceLoader the resource loader
   */
  public void setResourceLoader(ResourceLoader resourceLoader) {
    Assert.notNull(resourceLoader, "ResourceLoader is required");
    this.resourceLoader = resourceLoader;
  }

  /**
   * The ResourceLoader that will be used in the ApplicationContext.
   *
   * @return the resourceLoader the resource loader that will be used in the
   * ApplicationContext (or null if the default)
   */
  @Nullable
  public ResourceLoader getResourceLoader() {
    return this.resourceLoader;
  }

  /**
   * Either the ClassLoader that will be used in the ApplicationContext (if
   * {@link #setResourceLoader(ResourceLoader) resourceLoader} is set), or the context
   * class loader (if not null), or the loader of the {@link ClassUtils} class.
   *
   * @return a ClassLoader (never null)
   */
  public ClassLoader getClassLoader() {
    ClassLoader classLoader = null;
    if (resourceLoader != null) {
      classLoader = resourceLoader.getClassLoader();
    }
    if (classLoader == null) {
      classLoader = ClassUtils.getDefaultClassLoader();
    }
    Assert.state(classLoader != null, "Cannot determine ClassLoader");
    return classLoader;
  }

  /**
   * Return a prefix that should be applied when obtaining configuration properties from
   * the system environment.
   *
   * @return the environment property prefix
   */
  @Nullable
  public String getEnvironmentPrefix() {
    return this.environmentPrefix;
  }

  /**
   * Set the prefix that should be applied when obtaining configuration properties from
   * the system environment.
   *
   * @param environmentPrefix the environment property prefix to set
   */
  public void setEnvironmentPrefix(@Nullable String environmentPrefix) {
    this.environmentPrefix = environmentPrefix;
  }

  /**
   * Sets the {@link ApplicationListener}s that will be applied to the Application
   * and registered with the {@link ApplicationContext}.
   *
   * @param listeners the listeners to set
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void setListeners(Collection<ApplicationListener> listeners) {
    this.listeners = new ArrayList(listeners);
  }

  /**
   * Add {@link ApplicationListener}s to be applied to the Application and
   * registered with the {@link ApplicationContext}.
   *
   * @param listeners the listeners to add
   */
  public void addListeners(ApplicationListener<?>... listeners) {
    CollectionUtils.addAll(this.listeners, listeners);
  }

  /**
   * Returns read-only ordered Set of the {@link ApplicationListener}s that will be
   * applied to the Application and registered with the {@link ApplicationContext}
   *
   * @return the listeners
   */
  public Set<ApplicationListener<?>> getListeners() {
    return asUnmodifiableOrderedSet(listeners);
  }

  /**
   * Sets the {@link ApplicationStartupListener}s that will be applied to the
   * {@link #getStartupListeners}
   *
   * @param listeners the listeners to add
   */
  public void addStartupListeners(ApplicationStartupListener... listeners) {
    if (startupListeners == null) {
      startupListeners = new ArrayList<>();
    }
    CollectionUtils.addAll(startupListeners, listeners);
  }

  /**
   * Sets the {@link ApplicationStartupListener}s that will be applied to the
   * {@link #getStartupListeners}
   *
   * @param listeners the startup listeners to set
   */
  public void setStartupListeners(Collection<ApplicationStartupListener> listeners) {
    this.startupListeners = new ArrayList<>(listeners);
  }

  /**
   * Sets the bean name generator that should be used when generating bean names.
   *
   * @param beanNameGenerator the bean name generator
   */
  public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
    this.beanNameGenerator = beanNameGenerator;
  }

  /**
   * Sets if a {@link CommandLinePropertySource} should be added to the application
   * context in order to expose arguments. Defaults to {@code true}.
   *
   * @param addCommandLineProperties if command line arguments should be exposed
   */
  public void setAddCommandLineProperties(boolean addCommandLineProperties) {
    this.addCommandLineProperties = addCommandLineProperties;
  }

  /**
   * Sets if the application information should be logged when the application starts.
   * Defaults to {@code true}.
   *
   * @param logStartupInfo if startup info should be logged.
   */
  public void setLogStartupInfo(boolean logStartupInfo) {
    this.logStartupInfo = logStartupInfo;
  }

  /**
   * Sets if the {@link ApplicationConversionService} should be added to the application
   * context's {@link Environment}.
   *
   * @param addConversionService if the application conversion service should be added
   */
  public void setAddConversionService(boolean addConversionService) {
    this.addConversionService = addConversionService;
  }

  /**
   * Sets if bean definition overriding, by registering a definition with the same name
   * as an existing definition, should be allowed. Defaults to {@code false}.
   *
   * @param allowBeanDefinitionOverriding if overriding is allowed
   * @see StandardBeanFactory#setAllowBeanDefinitionOverriding(boolean)
   */
  public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
    this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
  }

  /**
   * Sets whether to allow circular references between beans and automatically try to
   * resolve them. Defaults to {@code false}.
   *
   * @param allowCircularReferences if circular references are allowed
   * @see AbstractAutowireCapableBeanFactory#setAllowCircularReferences(boolean)
   */
  public void setAllowCircularReferences(boolean allowCircularReferences) {
    this.allowCircularReferences = allowCircularReferences;
  }

  /**
   * Sets if beans should be initialized lazily. Defaults to {@code false}.
   *
   * @param lazyInitialization if initialization should be lazy
   * @see BeanDefinition#setLazyInit(boolean)
   */
  public void setLazyInitialization(boolean lazyInitialization) {
    this.lazyInitialization = lazyInitialization;
  }

  /**
   * Sets the {@link Banner} instance which will be used to print the banner when no
   * static banner file is provided.
   *
   * @param banner the Banner instance to use
   */
  public void setBanner(@Nullable Banner banner) {
    this.banner = banner;
  }

  /**
   * Sets the mode used to display the banner when the application runs. Defaults to
   * {@code Banner.Mode.CONSOLE}.
   *
   * @param bannerMode the mode used to display the banner
   */
  public void setBannerMode(Banner.Mode bannerMode) {
    this.bannerMode = bannerMode;
  }

  /**
   * Set default environment properties which will be used in addition to those in the
   * existing {@link Environment}.
   *
   * @param defaultProperties the additional properties to set
   */
  public void setDefaultProperties(@Nullable Map<String, Object> defaultProperties) {
    this.defaultProperties = defaultProperties;
  }

  /**
   * Set additional profile values to use (on top of those set in system or command line
   * properties).
   *
   * @param profiles the additional profiles to set
   */
  public void setAdditionalProfiles(String... profiles) {
    this.additionalProfiles = Collections.unmodifiableSet(CollectionUtils.newLinkedHashSet(profiles));
  }

  /**
   * Return an immutable set of any additional profiles in use.
   *
   * @return the additional profiles
   */
  public Set<String> getAdditionalProfiles() {
    return this.additionalProfiles;
  }

  /**
   * Convenient alternative to {@link #setDefaultProperties(Map)}.
   *
   * @param defaultProperties some {@link Properties}
   */
  public void setDefaultProperties(Properties defaultProperties) {
    this.defaultProperties = new HashMap<>();
    for (String name : defaultProperties.stringPropertyNames()) {
      this.defaultProperties.put(name, defaultProperties.get(name));
    }
  }

  /**
   * Adds {@link BootstrapRegistryInitializer} instances that can be used to initialize
   * the {@link BootstrapRegistry}.
   *
   * @param bootstrapRegistryInitializer the bootstrap registry initializer to add
   */
  public void addBootstrapRegistryInitializer(BootstrapRegistryInitializer bootstrapRegistryInitializer) {
    Assert.notNull(bootstrapRegistryInitializer, "BootstrapRegistryInitializer is required");
    this.bootstrapRegistryInitializers.add(bootstrapRegistryInitializer);
  }

  /**
   * Whether to keep the application alive even if there are no more non-daemon threads.
   *
   * @return whether to keep the application alive even if there are no more non-daemon
   * threads
   */
  public boolean isKeepAlive() {
    return this.keepAlive;
  }

  /**
   * Whether to keep the application alive even if there are no more non-daemon threads.
   *
   * @param keepAlive whether to keep the application alive even if there are no more
   * non-daemon threads
   */
  public void setKeepAlive(boolean keepAlive) {
    this.keepAlive = keepAlive;
  }

  private RuntimeException handleRunFailure(@Nullable ConfigurableApplicationContext context,
          Throwable exception, @Nullable ApplicationStartupListeners listeners) {
    if (exception instanceof AbandonedRunException abandonedRunException) {
      return abandonedRunException;
    }
    try {
      try {
        handleExitCode(context, exception);
        if (listeners != null) {
          listeners.failed(context, exception);
        }
      }
      finally {
        reportFailure(getExceptionReporters(context), exception);
        if (context != null) {
          context.close();
          shutdownHook.deregisterFailedApplicationContext(context);
        }
      }
    }
    catch (Exception ex) {
      logger.warn("Unable to close ApplicationContext", ex);
    }
    return exception instanceof RuntimeException runtimeException
            ? runtimeException : new IllegalStateException(exception);
  }

  private List<ApplicationExceptionReporter> getExceptionReporters(@Nullable ConfigurableApplicationContext context) {
    ClassLoader classLoader = getClassLoader();
    var instantiator = new Instantiator<ApplicationExceptionReporter>(ApplicationExceptionReporter.class, parameters -> {
      parameters.add(ConfigurableApplicationContext.class, context);
    });

    List<String> strategiesNames = TodayStrategies.findNames(ApplicationExceptionReporter.class, classLoader);
    return instantiator.instantiate(strategiesNames);
  }

  private void reportFailure(List<ApplicationExceptionReporter> exceptionReporters, Throwable failure) {
    try {
      for (ApplicationExceptionReporter reporter : exceptionReporters) {
        if (reporter.reportException(failure)) {
          registerLoggedException(failure);
          return;
        }
      }
    }
    catch (Throwable ex) {
      // Continue with normal handling of the original failure
    }
    if (logger.isErrorEnabled()) {
      logger.error("Application run failed", failure);
      registerLoggedException(failure);
    }
  }

  /**
   * Register that the given exception has been logged. By default, if the running in
   * the main thread, this method will suppress additional printing of the stacktrace.
   *
   * @param exception the exception that was logged
   */
  protected void registerLoggedException(Throwable exception) {
    StartupExceptionHandler handler = getStartupExceptionHandler();
    if (handler != null) {
      handler.registerLoggedException(exception);
    }
  }

  @Nullable
  StartupExceptionHandler getStartupExceptionHandler() {
    if (isMainThread(Thread.currentThread())) {
      return StartupExceptionHandler.forCurrentThread();
    }
    return null;
  }

  private boolean isMainThread(Thread currentThread) {
    return ("main".equals(currentThread.getName())
            || "restartedMain".equals(currentThread.getName()))
            && "main".equals(currentThread.getThreadGroup().getName());
  }

  private void handleExitCode(@Nullable ConfigurableApplicationContext context, Throwable exception) {
    int exitCode = getExitCodeFromException(context, exception);
    if (exitCode != 0) {
      if (context != null) {
        context.publishEvent(new ExitCodeEvent(context, exitCode));
      }
      StartupExceptionHandler handler = getStartupExceptionHandler();
      if (handler != null) {
        handler.registerExitCode(exitCode);
      }
    }
  }

  private int getExitCodeFromException(@Nullable ConfigurableApplicationContext context, Throwable exception) {
    int exitCode = getExitCodeFromMappedException(context, exception);
    if (exitCode == 0) {
      exitCode = getExitCodeFromExitCodeGeneratorException(exception);
    }
    return exitCode;
  }

  private int getExitCodeFromMappedException(@Nullable ConfigurableApplicationContext context, Throwable exception) {
    if (context == null || context.getState() == ApplicationContext.State.NONE) {
      return 0;
    }
    ExitCodeGenerators generators = new ExitCodeGenerators();
    List<ExitCodeExceptionMapper> beans = context.getBeanFactory().getBeans(ExitCodeExceptionMapper.class);
    generators.addAll(exception, beans);
    return generators.getExitCode();
  }

  private int getExitCodeFromExitCodeGeneratorException(@Nullable Throwable exception) {
    if (exception == null) {
      return 0;
    }
    if (exception instanceof ExitCodeGenerator) {
      return ((ExitCodeGenerator) exception).getExitCode();
    }
    return getExitCodeFromExitCodeGeneratorException(exception.getCause());
  }

  private void callRunners(ApplicationContext context, ApplicationArguments args) {
    HashSet<ApplicationRunner> called = new HashSet<>(16);
    for (ApplicationRunner runner : context.getBeanProvider(ApplicationRunner.class)) {
      if (called.add(runner)) {
        try {
          runner.run(args);
        }
        catch (Exception ex) {
          throw new IllegalStateException("Failed to execute ApplicationRunner: " + runner, ex);
        }
      }
    }
  }

  /**
   * Return a {@link ApplicationShutdownHandlers} instance that can be used to add
   * or remove handlers that perform actions before the JVM is shutdown.
   *
   * @return a {@link ApplicationShutdownHandlers} instance
   */
  public static ApplicationShutdownHandlers getShutdownHandlers() {
    return shutdownHook.handlers;
  }

  /**
   * Static helper that can be used to run a {@link Application} from the
   * specified source using default settings.
   *
   * @param primarySource the primary source to load
   * @param args the application arguments (usually passed from a Java main method)
   * @return the running {@link ApplicationContext}
   */
  public static ConfigurableApplicationContext run(Class<?> primarySource, String... args) {
    return run(new Class<?>[] { primarySource }, args);
  }

  /**
   * Static helper that can be used to run a {@link Application} from the
   * specified sources using default settings and user supplied arguments.
   *
   * @param primarySources the primary sources to load
   * @param args the application arguments (usually passed from a Java main method)
   * @return the running {@link ApplicationContext}
   */
  public static ConfigurableApplicationContext run(Class<?>[] primarySources, String[] args) {
    return new Application(primarySources).run(args);
  }

  /**
   * A basic main that can be used to launch an application. This method is useful when
   * application sources are defined via a {@literal --app.main.sources} command line
   * argument.
   * <p>
   * Most developers will want to define their own main method and call the
   * {@link #run(Class, String...) run} method instead.
   *
   * @param args command line arguments
   * @see Application#run(Class[], String[])
   * @see Application#run(Class, String...)
   */
  public static void main(String[] args) {
    Application.run(Constant.EMPTY_CLASSES, args);
  }

  public static ApplicationBuilder forBuilder(Class<?>... sources) {
    return ApplicationBuilder.from(sources);
  }

  public static ApplicationBuilder forBuilder(ResourceLoader resourceLoader, Class<?>... sources) {
    return ApplicationBuilder.from(resourceLoader, sources);
  }

  /**
   * Static helper that can be used to exit a {@link Application} and obtain a
   * code indicating success (0) or otherwise. Does not throw exceptions but should
   * print stack traces of any encountered. Applies the specified
   * {@link ExitCodeGenerator} in addition to any Framework beans that implement
   * {@link ExitCodeGenerator}. In the case of multiple exit codes the highest value
   * will be used (or if all values are negative, the lowest value will be used)
   *
   * @param context the context to close if possible
   * @param exitCodeGenerators exit code generators
   * @return the outcome (0 if successful)
   */
  public static int exit(ApplicationContext context, ExitCodeGenerator... exitCodeGenerators) {
    Assert.notNull(context, "Context is required");
    int exitCode = 0;
    try {
      try {
        ExitCodeGenerators generators = new ExitCodeGenerators();
        Collection<ExitCodeGenerator> beans = context.getBeansOfType(ExitCodeGenerator.class).values();
        generators.addAll(exitCodeGenerators);
        generators.addAll(beans);
        exitCode = generators.getExitCode();
        if (exitCode != 0) {
          context.publishEvent(new ExitCodeEvent(context, exitCode));
        }
      }
      finally {
        close(context);
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
      exitCode = (exitCode != 0) ? exitCode : 1;
    }
    return exitCode;
  }

  /**
   * Perform the given action with the given {@link ApplicationHook} attached if
   * the action triggers an {@link Application#run(String...) application run}.
   *
   * @param hook the hook to apply
   * @param action the action to run
   * @see #withHook(ApplicationHook, ThrowingSupplier)
   */
  public static void withHook(ApplicationHook hook, Runnable action) {
    withHook(hook, () -> {
      action.run();
      return null;
    });
  }

  /**
   * Perform the given action with the given {@link ApplicationHook} attached if
   * the action triggers an {@link Application#run(String...) application run}.
   *
   * @param <T> the result type
   * @param hook the hook to apply
   * @param action the action to run
   * @return the result of the action
   * @see #withHook(ApplicationHook, Runnable)
   */
  public static <T> T withHook(ApplicationHook hook, ThrowingSupplier<T> action) {
    applicationHook.set(hook);
    try {
      return action.get();
    }
    finally {
      applicationHook.remove();
    }
  }

  /**
   * Create an application from an existing {@code main} method that can run with
   * additional {@code @Configuration} or bean classes. This method can be helpful when
   * writing a test harness that needs to start an application with additional
   * configuration.
   *
   * @param main the main method entry point that runs the {@link Application}
   * @return an {@link Application.Augmented} instance that can be used to add
   * configuration and run the application.
   * @see #withHook(ApplicationHook, Runnable)
   */
  public static Application.Augmented from(ThrowingConsumer<String[]> main) {
    Assert.notNull(main, "Main ThrowingConsumer is required");
    return new Augmented(main, Collections.emptySet());
  }

  private static void close(ApplicationContext context) {
    if (context instanceof ConfigurableApplicationContext closable) {
      closable.close();
    }
  }

  private static <E> Set<E> asUnmodifiableOrderedSet(Collection<E> elements) {
    ArrayList<E> list = new ArrayList<>(elements);
    list.sort(AnnotationAwareOrderComparator.INSTANCE);
    return new LinkedHashSet<>(list);
  }

  /**
   * Exception that can be thrown to silently exit a running {@link Application}
   * without handling run failures.
   */
  public static class AbandonedRunException extends RuntimeException {

    @Nullable
    private final ConfigurableApplicationContext applicationContext;

    /**
     * Create a new {@link AbandonedRunException} instance with the given application
     * context.
     *
     * @param applicationContext the application context that was available when the
     * run was abandoned
     */
    public AbandonedRunException(@Nullable ConfigurableApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
    }

    /**
     * Return the application context that was available when the run was abandoned or
     * {@code null} if no context was available.
     *
     * @return the application context
     */
    @Nullable
    public ConfigurableApplicationContext getApplicationContext() {
      return this.applicationContext;
    }

  }

  /**
   * Used to configure and run an augmented {@link Application} where additional
   * configuration should be applied.
   */
  public static class Augmented {

    private final ThrowingConsumer<String[]> main;

    private final Set<Class<?>> sources;

    Augmented(ThrowingConsumer<String[]> main, Set<Class<?>> sources) {
      this.main = main;
      this.sources = Set.copyOf(sources);
    }

    /**
     * Return a new {@link Application.Augmented} instance with additional
     * sources that should be applied when the application runs.
     *
     * @param sources the sources that should be applied
     * @return a new {@link Application.Augmented} instance
     */
    public Augmented with(Class<?>... sources) {
      LinkedHashSet<Class<?>> merged = new LinkedHashSet<>(this.sources);
      merged.addAll(Arrays.asList(sources));
      return new Augmented(this.main, merged);
    }

    /**
     * Run the application using the given args.
     *
     * @param args the main method args
     */
    public Running run(String... args) {
      RunListener runListener = new RunListener();
      ApplicationHook hook = ApplicationHook.forSingleUse((application) -> {
        application.addPrimarySources(this.sources);
        return runListener;
      });
      withHook(hook, () -> this.main.accept(args));
      return runListener;
    }

    /**
     * {@link ApplicationStartupListener} to capture {@link Running} application
     * details.
     */
    private static class RunListener implements ApplicationStartupListener, Running {

      private final List<ConfigurableApplicationContext> contexts = Collections.synchronizedList(new ArrayList<>());

      @Override
      public void contextLoaded(ConfigurableApplicationContext context) {
        this.contexts.add(context);
      }

      @Override
      public ConfigurableApplicationContext getApplicationContext() {
        List<ConfigurableApplicationContext> rootContexts = this.contexts.stream()
                .filter((context) -> context.getParent() == null)
                .toList();
        Assert.state(!rootContexts.isEmpty(), "No root application context located");
        Assert.state(rootContexts.size() == 1, "No unique root application context located");
        return rootContexts.get(0);
      }

    }

  }

  /**
   * Provides access to details of a {@link Application} run using
   * {@link Augmented#run(String...)}.
   */
  public interface Running {

    /**
     * Return the root {@link ConfigurableApplicationContext} of the running
     * application.
     *
     * @return the root application context
     */
    ConfigurableApplicationContext getApplicationContext();

  }

  /**
   * {@link BeanFactoryPostProcessor} to re-order our property sources below any
   * {@code @PropertySource} items added by the {@link ConfigurationClassPostProcessor}.
   */
  private static class PropertySourceOrderingBeanFactoryPostProcessor implements BeanFactoryPostProcessor, Ordered {

    private final ConfigurableApplicationContext context;

    PropertySourceOrderingBeanFactoryPostProcessor(ConfigurableApplicationContext context) {
      this.context = context;
    }

    @Override
    public int getOrder() {
      return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
      DefaultPropertiesPropertySource.moveToEnd(context.getEnvironment());
    }

  }

  static class ApplicationRuntimeHints extends BindableRuntimeHintsRegistrar {

    ApplicationRuntimeHints() {
      super(Application.class);
    }

  }

  /**
   * A non-daemon thread to keep the JVM alive. Reacts to {@link ContextClosedEvent} to
   * stop itself when the application context is closed.
   */
  private static final class KeepAlive implements ApplicationListener<ApplicationContextEvent> {

    private final AtomicReference<Thread> thread = new AtomicReference<>();

    @Override
    public void onApplicationEvent(ApplicationContextEvent event) {
      if (event instanceof ContextRefreshedEvent) {
        startKeepAliveThread();
      }
      else if (event instanceof ContextClosedEvent) {
        stopKeepAliveThread();
      }
    }

    private void startKeepAliveThread() {
      Thread thread = new Thread(() -> {
        while (true) {
          try {
            Thread.sleep(Long.MAX_VALUE);
          }
          catch (InterruptedException ex) {
            break;
          }
        }
      });
      if (this.thread.compareAndSet(null, thread)) {
        thread.setDaemon(false);
        thread.setName("keep-alive");
        thread.start();
      }
    }

    private void stopKeepAliveThread() {
      Thread thread = this.thread.getAndSet(null);
      if (thread == null) {
        return;
      }
      thread.interrupt();
    }

  }

  abstract static class Startup {

    private Duration timeTakenToStarted;

    abstract long startTime();

    @Nullable
    abstract Long processUptime();

    abstract String action();

    final Duration started() {
      long now = System.currentTimeMillis();
      this.timeTakenToStarted = Duration.ofMillis(now - startTime());
      return this.timeTakenToStarted;
    }

    private Duration ready() {
      long now = System.currentTimeMillis();
      return Duration.ofMillis(now - startTime());
    }

    Duration timeTakenToStarted() {
      return this.timeTakenToStarted;
    }

    static Startup create() {
      return (!ClassUtils.isPresent("jdk.crac.management.CRaCMXBean", Startup.class))
              ? new StandardStartup() : new CoordinatedRestoreAtCheckpointStartup();
    }

  }

  private static class CoordinatedRestoreAtCheckpointStartup extends Startup {

    private final StandardStartup fallback = new StandardStartup();

    @Override
    Long processUptime() {
      long uptime = CRaCMXBean.getCRaCMXBean().getUptimeSinceRestore();
      return (uptime >= 0) ? uptime : this.fallback.processUptime();
    }

    @Override
    String action() {
      if (restoreTime() >= 0) {
        return "Restored";
      }
      return this.fallback.action();
    }

    private long restoreTime() {
      return CRaCMXBean.getCRaCMXBean().getRestoreTime();
    }

    @Override
    long startTime() {
      long restoreTime = restoreTime();
      if (restoreTime >= 0) {
        return restoreTime;
      }
      return this.fallback.startTime();
    }

  }

  private static class StandardStartup extends Startup {

    private final Long startTime = System.currentTimeMillis();

    @Override
    long startTime() {
      return this.startTime;
    }

    @Override
    Long processUptime() {
      try {
        return ManagementFactory.getRuntimeMXBean().getUptime();
      }
      catch (Throwable ex) {
        return null;
      }
    }

    @Override
    String action() {
      return "Started";
    }

  }

}
