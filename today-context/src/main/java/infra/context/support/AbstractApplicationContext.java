/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.support;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import infra.beans.BeansException;
import infra.beans.CachedIntrospectionResults;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryInitializer;
import infra.beans.factory.BeanNotOfRequiredTypeException;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.ObjectProvider;
import infra.beans.factory.config.AutowireCapableBeanFactory;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.config.BeanPostProcessor;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.ExpressionEvaluator;
import infra.beans.factory.support.DependencyInjector;
import infra.beans.factory.support.DependencyResolvingStrategies;
import infra.beans.factory.support.DependencyResolvingStrategy;
import infra.beans.support.ResourceEditorRegistrar;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.ApplicationContextException;
import infra.context.ApplicationEvent;
import infra.context.ApplicationEventPublisher;
import infra.context.ApplicationEventPublisherAware;
import infra.context.ApplicationListener;
import infra.context.BootstrapContext;
import infra.context.BootstrapContextAware;
import infra.context.ConfigurableApplicationContext;
import infra.context.EnvironmentAware;
import infra.context.HierarchicalMessageSource;
import infra.context.LifecycleProcessor;
import infra.context.MessageSource;
import infra.context.MessageSourceAware;
import infra.context.MessageSourceResolvable;
import infra.context.NoSuchMessageException;
import infra.context.PayloadApplicationEvent;
import infra.context.ResourceLoaderAware;
import infra.context.event.ApplicationEventMulticaster;
import infra.context.event.ContextClosedEvent;
import infra.context.event.ContextRefreshedEvent;
import infra.context.event.ContextRestartedEvent;
import infra.context.event.ContextStartedEvent;
import infra.context.event.ContextStoppedEvent;
import infra.context.event.SimpleApplicationEventMulticaster;
import infra.context.expression.EmbeddedValueResolverAware;
import infra.context.expression.StandardBeanExpressionResolver;
import infra.context.weaving.LoadTimeWeaverAware;
import infra.context.weaving.LoadTimeWeaverAwareProcessor;
import infra.core.NativeDetector;
import infra.core.ResolvableType;
import infra.core.annotation.AnnotationUtils;
import infra.core.annotation.MergedAnnotation;
import infra.core.conversion.ConversionService;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.env.StandardEnvironment;
import infra.core.io.DefaultResourceLoader;
import infra.core.io.PathMatchingPatternResourceLoader;
import infra.core.io.PatternResourceLoader;
import infra.core.io.Resource;
import infra.core.io.ResourceConsumer;
import infra.core.io.ResourceLoader;
import infra.core.io.SmartResourceConsumer;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.lang.TodayStrategies;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.CollectionUtils;
import infra.util.ObjectUtils;
import infra.util.ReflectionUtils;

/**
 * Abstract implementation of the {@link infra.context.ApplicationContext}
 * interface. Doesn't mandate the type of storage used for configuration; simply
 * implements common context functionality. Uses the Template Method design pattern,
 * requiring concrete subclasses to implement abstract methods.
 *
 * <p>In contrast to a plain BeanFactory, an ApplicationContext is supposed
 * to detect special beans defined in its internal bean factory:
 * Therefore, this class automatically registers
 * {@link BeanFactoryPostProcessor BeanFactoryPostProcessors},
 * {@link BeanPostProcessor BeanPostProcessors},
 * and {@link infra.context.ApplicationListener ApplicationListeners}
 * which are defined as beans in the context.
 *
 * <p>A {@link infra.context.MessageSource} may also be supplied
 * as a bean in the context, with the name "messageSource"; otherwise, message
 * resolution is delegated to the parent context. Furthermore, a multicaster
 * for application events can be supplied as an "applicationEventMulticaster" bean
 * of type {@link ApplicationEventMulticaster}
 * in the context; otherwise, a default multicaster of type
 * {@link SimpleApplicationEventMulticaster} will be used.
 *
 * <p>Implements resource loading by extending
 * {@link DefaultResourceLoader}.
 * Consequently treats non-URL resource paths as class path resources
 * (supporting full class path resource names that include the package path,
 * e.g. "mypackage/myresource.dat"), unless the {@link #getResourceByPath}
 * method is overridden in a subclass.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #refreshBeanFactory
 * @see #getBeanFactory
 * @see BeanFactoryPostProcessor
 * @see BeanPostProcessor
 * @see ApplicationEventMulticaster
 * @see infra.context.ApplicationListener
 * @see infra.context.MessageSource
 * @since 2018-09-09 22:02
 */
@SuppressWarnings({ "unchecked" })
public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * The name of the {@link LifecycleProcessor} bean in the context.
   * If none is supplied, a {@link DefaultLifecycleProcessor} is used.
   *
   * @see infra.context.LifecycleProcessor
   * @see DefaultLifecycleProcessor
   * @see #start()
   * @see #stop()
   */
  public static final String LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor";

  /**
   * The name of the {@link MessageSource} bean in the context.
   * If none is supplied, message resolution is delegated to the parent.
   *
   * @see infra.context.MessageSource
   * @see ResourceBundleMessageSource
   * @see ReloadableResourceBundleMessageSource
   * @see #getMessage(MessageSourceResolvable, Locale)
   */
  public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

  /**
   * The name of the {@link ApplicationEventMulticaster} bean in the context.
   * If none is supplied, a {@link SimpleApplicationEventMulticaster} is used.
   *
   * @see ApplicationEventMulticaster
   * @see SimpleApplicationEventMulticaster
   * @see #publishEvent(ApplicationEvent)
   * @see #addApplicationListener(ApplicationListener)
   */
  public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";

  private Instant startupDate = Instant.now();

  @Nullable
  private ConfigurableEnvironment environment;

  // @since 2.1.5
  private State state = State.NONE;

  private final ArrayList<BeanFactoryPostProcessor> factoryPostProcessors = new ArrayList<>();

  /** Unique id for this context, if any. @since 4.0 */
  private String id = ObjectUtils.identityToString(this);

  /** Display name. */
  private String displayName = ObjectUtils.identityToString(this);

  /** Parent context. @since 4.0 */
  @Nullable
  private ApplicationContext parent;

  /** @since 4.0 */
  private final PatternResourceLoader patternResourceLoader = getPatternResourceLoader();

  /** @since 4.0 */

  @Nullable
  private ExpressionEvaluator expressionEvaluator;

  /** Reference to the JVM shutdown hook, if registered. */
  @Nullable
  private Thread shutdownHook;

  /** LifecycleProcessor for managing the lifecycle of beans within this context. @since 4.0 */
  @Nullable
  private LifecycleProcessor lifecycleProcessor;

  /** Helper class used in event publishing. @since 4.0 */
  @Nullable
  private ApplicationEventMulticaster applicationEventMulticaster;

  /** Statically specified listeners. @since 4.0 */
  private final LinkedHashSet<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();

  /** Local listeners registered before refresh. @since 4.0 */
  @Nullable
  private Set<ApplicationListener<?>> earlyApplicationListeners;

  /** ApplicationEvents published before the multicaster setup. @since 4.0 */
  @Nullable
  private Set<ApplicationEvent> earlyApplicationEvents;

  /** MessageSource we delegate our implementation of this interface to. @since 4.0 */
  @Nullable
  private MessageSource messageSource;

  @Nullable
  private BootstrapContext bootstrapContext;

  /** Flag that indicates whether this context is currently active. */
  private final AtomicBoolean active = new AtomicBoolean();

  /** Flag that indicates whether this context has been closed already. @since 4.0 */
  private final AtomicBoolean closed = new AtomicBoolean();

  /** Synchronization lock for "refresh" and "close". */
  private final ReentrantLock startupShutdownLock = new ReentrantLock();

  /** Currently active startup/shutdown thread. */
  @Nullable
  private volatile Thread startupShutdownThread;

  /**
   * Create a new AbstractApplicationContext with no parent.
   */
  public AbstractApplicationContext() { }

  /**
   * Create a new AbstractApplicationContext with the given parent context.
   *
   * @param parent the parent context
   */
  public AbstractApplicationContext(@Nullable ApplicationContext parent) {
    this();
    setParent(parent);
  }

  //---------------------------------------------------------------------
  // BootstrapContext @since 4.0
  //---------------------------------------------------------------------

  /**
   * Return the DefinitionLoadingContext to use for loading this context
   *
   * @return the DefinitionLoadingContext for this context
   * @since 4.0
   */
  protected BootstrapContext createBootstrapContext() {
    return new BootstrapContext(getBeanFactory(), this);
  }

  /**
   * set BootstrapContext
   *
   * @param bootstrapContext BootstrapContext
   * @since 4.0
   */
  public void setBootstrapContext(@Nullable BootstrapContext bootstrapContext) {
    this.bootstrapContext = bootstrapContext;
  }

  /**
   * Returns BootstrapContext
   *
   * @return Returns BootstrapContext
   * @since 4.0
   */
  @Override
  public BootstrapContext getBootstrapContext() {
    BootstrapContext bootstrapContext = this.bootstrapContext;
    if (bootstrapContext == null) {
      bootstrapContext = createBootstrapContext();
      this.bootstrapContext = bootstrapContext;
    }
    return bootstrapContext;
  }

  //---------------------------------------------------------------------
  // Implementation of PatternResourceLoader interface
  //---------------------------------------------------------------------

  @Override
  public Set<Resource> getResources(String locationPattern) throws IOException {
    return patternResourceLoader.getResources(locationPattern);
  }

  @Override
  public void scan(String locationPattern, ResourceConsumer consumer) throws IOException {
    patternResourceLoader.scan(locationPattern, consumer);
  }

  @Override
  public void scan(String locationPattern, SmartResourceConsumer consumer) throws IOException {
    patternResourceLoader.scan(locationPattern, consumer);
  }

  /**
   * Return the PatternResourceLoader to use for resolving location patterns
   * into Resource instances. Default is a {@link PathMatchingPatternResourceLoader},
   * supporting Ant-style location patterns.
   * <p>Can be overridden in subclasses, for extended resolution strategies,
   * for example in a web environment.
   * <p><b>Do not call this when needing to resolve a location pattern.</b>
   * Call the context's {@code getResources} method instead, which
   * will delegate to the PatternResourceLoader.
   *
   * @return the PatternResourceLoader for this context
   * @see #getResources
   * @see PathMatchingPatternResourceLoader
   */
  protected PatternResourceLoader getPatternResourceLoader() {
    return new PathMatchingPatternResourceLoader(this);
  }

  //---------------------------------------------------------------------
  // Implementation of ApplicationContext interface
  //---------------------------------------------------------------------

  /**
   * Set the unique id of this application context.
   * <p>Default is the object id of the context instance, or the name
   * of the context bean if the context is itself defined as a bean.
   *
   * @param id the unique id of the context
   */
  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return this.id;
  }

  /**
   * Return this application name for this context.
   *
   * @return a display name for this context (never {@code null})
   */
  @Override
  public String getApplicationName() {
    return "";
  }

  /**
   * Set a friendly name for this context.
   * Typically done during initialization of concrete context implementations.
   * <p>Default is the object id of the context instance.
   *
   * @since 4.0
   */
  public void setDisplayName(String displayName) {
    Assert.hasLength(displayName, "Display name must not be empty");
    this.displayName = displayName;
  }

  /**
   * Return a friendly name for this context.
   *
   * @return a display name for this context (never {@code null})
   * @since 4.0
   */
  @Override
  public String getDisplayName() {
    return this.displayName;
  }

  /**
   * Return the parent context, or {@code null} if there is no parent
   * (that is, this context is the root of the context hierarchy).
   */
  @Override
  @Nullable
  public ApplicationContext getParent() {
    return this.parent;
  }

  @Override
  public AutowireCapableBeanFactory getAutowireCapableBeanFactory() {
    return getBeanFactory();
  }

  @Override
  public ExpressionEvaluator getExpressionEvaluator() {
    if (expressionEvaluator == null) {
      expressionEvaluator = new ExpressionEvaluator(getBeanFactory());
    }
    return expressionEvaluator;
  }

  //---------------------------------------------------------------------
  // Implementation of HierarchicalBeanFactory interface
  //---------------------------------------------------------------------

  @Override
  @Nullable
  public BeanFactory getParentBeanFactory() {
    return getParent();
  }

  @Override
  public boolean containsLocalBean(String name) {
    return getBeanFactory().containsLocalBean(name);
  }

  /**
   * Return the internal bean factory of the parent context if it implements
   * ConfigurableApplicationContext; else, return the parent context itself.
   *
   * @see ConfigurableApplicationContext#unwrapFactory
   */
  @Nullable
  protected BeanFactory getInternalParentBeanFactory() {
    ApplicationContext parent = getParent();
    return parent instanceof ConfigurableApplicationContext cac ? cac.getBeanFactory() : parent;
  }

  //---------------------------------------------------------------------
  // Implementation of MessageSource interface
  //---------------------------------------------------------------------

  @Nullable
  @Override
  public String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, @Nullable Locale locale) {
    return getMessageSource().getMessage(code, args, defaultMessage, locale);
  }

  @Override
  public String getMessage(String code, @Nullable Object[] args, @Nullable Locale locale) throws NoSuchMessageException {
    return getMessageSource().getMessage(code, args, locale);
  }

  @Override
  public String getMessage(MessageSourceResolvable resolvable, @Nullable Locale locale) throws NoSuchMessageException {
    return getMessageSource().getMessage(resolvable, locale);
  }

  /**
   * Return the internal MessageSource used by the context.
   *
   * @return the internal MessageSource (never {@code null})
   * @throws IllegalStateException if the context has not been initialized yet
   */
  private MessageSource getMessageSource() throws IllegalStateException {
    if (this.messageSource == null) {
      throw new IllegalStateException("MessageSource not initialized - call 'refresh' before accessing messages via the context: " + this);
    }
    return this.messageSource;
  }

  /**
   * Return the internal message source of the parent context if it is an
   * AbstractApplicationContext too; else, return the parent context itself.
   */
  @Nullable
  protected MessageSource getInternalParentMessageSource() {
    ApplicationContext parent = getParent();
    return parent instanceof AbstractApplicationContext abc ? abc.messageSource : parent;
  }

  /**
   * Reset reflection metadata caches, in particular the
   * {@link ReflectionUtils}, {@link AnnotationUtils}, {@link ResolvableType}
   *
   * @see ReflectionUtils#clearCache()
   * @see AnnotationUtils#clearCache()
   * @see ResolvableType#clearCache()
   * @since 4.0
   */
  protected void resetCommonCaches() {
    ReflectionUtils.clearCache();
    AnnotationUtils.clearCache();
    ResolvableType.clearCache();
    CachedIntrospectionResults.clearClassLoader(getClassLoader());
    if (bootstrapContext != null) {
      bootstrapContext.clearCache();
    }
  }

  //---------------------------------------------------------------------
  // Implementation of ApplicationContext interface
  //---------------------------------------------------------------------

  @Override
  public void refresh() throws BeansException, IllegalStateException {
    this.startupShutdownLock.lock();
    try {
      this.startupShutdownThread = Thread.currentThread();

      // Prepare this context for refreshing.
      prepareRefresh();

      // Tell the subclass to refresh the internal bean factory.
      ConfigurableBeanFactory beanFactory = obtainFreshBeanFactory();

      // register framework beans
      registerFrameworkComponents(beanFactory);

      // Prepare BeanFactory
      prepareBeanFactory(beanFactory);

      try {
        // Allows post-processing of the bean factory in context subclasses.
        postProcessBeanFactory(beanFactory);

        // Invoke factory processors registered as beans in the context.
        invokeBeanFactoryPostProcessors(beanFactory);

        // Register bean processors that intercept bean creation.
        registerBeanPostProcessors(beanFactory);

        // Initialize message source for this context.
        initMessageSource();

        // Initialize event multicaster for this context.
        initApplicationEventMulticaster();

        // Initialization singletons that has already in context
        // Initialize other special beans in specific context subclasses.
        // for example a Web Server
        onRefresh();

        // Check for listener beans and register them.
        registerApplicationListeners();

        // Instantiate all remaining (non-lazy-init) singletons.
        finishBeanFactoryInitialization(beanFactory);

        // Finish refresh
        finishRefresh();
      }

      catch (RuntimeException | Error ex) {
        applyState(State.FAILED);

        logger.warn("Exception encountered during context initialization - cancelling refresh attempt: {}",
                ex.toString());

        // Destroy already created singletons to avoid dangling resources.
        destroyBeans();

        // Reset 'active' flag.
        cancelRefresh(ex);

        // Propagate exception to caller.
        throw ex;
      }
    }
    finally {
      this.startupShutdownThread = null;
      this.startupShutdownLock.unlock();
    }
  }

  /**
   * Prepare to load context
   */
  protected void prepareRefresh() {
    this.startupDate = Instant.now();
    this.closed.set(false);
    this.active.set(true);
    applyState(State.STARTING);
    ApplicationContextHolder.register(this); // @since 4.0

    logger.info("Starting application context at '{}'", startupDate);

    ConfigurableEnvironment environment = getEnvironment();

    // Initialize any placeholder property sources in the context environment.
    initPropertySources();

    environment.validateRequiredProperties();

    // Store pre-refresh ApplicationListeners...
    if (this.earlyApplicationListeners == null) {
      this.earlyApplicationListeners = new LinkedHashSet<>(this.applicationListeners);
    }
    else {
      // Reset local application listeners to pre-refresh state.
      this.applicationListeners.clear();
      this.applicationListeners.addAll(this.earlyApplicationListeners);
    }

    // Allow for the collection of early ApplicationEvents,
    // to be published once the multicaster is available...
    this.earlyApplicationEvents = new LinkedHashSet<>();

    if (logger.isDebugEnabled()) {
      if (logger.isTraceEnabled()) {
        logger.trace("Refreshing {}", this);
      }
      else {
        logger.debug("Refreshing {}", getDisplayName());
      }
    }
  }

  /**
   * <p>
   * load properties files or itself strategies
   */
  protected void initPropertySources() throws ApplicationContextException {
    // for subclasses loading properties or prepare property-source
  }

  /**
   * Register Framework Beans
   */
  protected void registerFrameworkComponents(ConfigurableBeanFactory beanFactory) {
    logger.debug("Registering framework components");

    BootstrapContext bootstrapContext = getBootstrapContext();
    beanFactory.registerSingleton(getDependencyInjector(bootstrapContext));

    if (!beanFactory.containsLocalBean(BootstrapContext.BEAN_NAME)) {
      beanFactory.registerSingleton(BootstrapContext.BEAN_NAME, bootstrapContext);
    }
    // Register default environment beans.
    if (!beanFactory.containsLocalBean(Environment.ENVIRONMENT_BEAN_NAME)) {
      beanFactory.registerSingleton(Environment.ENVIRONMENT_BEAN_NAME, getEnvironment());
    }
    if (!beanFactory.containsLocalBean(Environment.SYSTEM_PROPERTIES_BEAN_NAME)) {
      beanFactory.registerSingleton(Environment.SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
    }
    if (!beanFactory.containsLocalBean(Environment.SYSTEM_ENVIRONMENT_BEAN_NAME)) {
      beanFactory.registerSingleton(Environment.SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
    }
  }

  private DependencyInjector getDependencyInjector(BootstrapContext bootstrapContext) {
    DependencyInjector injector = getInjector();
    var strategies = TodayStrategies.find(DependencyResolvingStrategy.class, getClassLoader(), bootstrapContext);
    injector.setResolvingStrategies(new DependencyResolvingStrategies(strategies));
    return injector;
  }

  /**
   * Initialization singletons that has already in context
   */
  protected void onRefresh() {
    // sub-classes Initialization
  }

  /**
   * Tell the subclass to refresh the internal bean factory.
   *
   * @return the fresh BeanFactory instance
   * @see #refreshBeanFactory()
   * @see #getBeanFactory()
   * @since 4.0
   */
  protected ConfigurableBeanFactory obtainFreshBeanFactory() {
    refreshBeanFactory();
    return getBeanFactory();
  }

  /**
   * Configure the factory's standard context characteristics,
   * such as the context's ClassLoader and post-processors.
   *
   * @param beanFactory the BeanFactory to configure
   */
  protected void prepareBeanFactory(ConfigurableBeanFactory beanFactory) {
    logger.debug("Preparing bean-factory: {}", beanFactory);
    // Tell the internal bean factory to use the context's class loader etc.
    ClassLoader classLoader = getClassLoader();
    beanFactory.setBeanClassLoader(classLoader);
    beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
    beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

    // Configure the bean factory with context callbacks.
    beanFactory.addBeanPostProcessor(new ContextAwareProcessor(this, getBootstrapContext()));
    // Register early post-processor for detecting inner beans as ApplicationListeners.
    beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

    // Detect a LoadTimeWeaver and prepare for weaving, if found.
    if (!NativeDetector.inNativeImage() && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
      beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
      // Set a temporary ClassLoader for type matching.
      beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
    }

    // @since 4.0
    beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
    beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
    beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
    beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
    beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
    beanFactory.ignoreDependencyInterface(BootstrapContextAware.class);
    beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);

    // BeanFactory interface not registered as resolvable type in a plain factory.
    // MessageSource registered (and found for autowiring) as a bean.
    beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
    beanFactory.registerResolvableDependency(ResourceLoader.class, this);
    beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
    beanFactory.registerResolvableDependency(ApplicationContext.class, this);

    // loading some outside beans

    BootstrapContext bootstrapContext = getBootstrapContext();
    var strategies = TodayStrategies.find(BeanDefinitionLoader.class, classLoader, bootstrapContext);
    if (!strategies.isEmpty()) {
      for (BeanDefinitionLoader loader : strategies) {
        loader.loadBeanDefinitions(bootstrapContext);
      }
    }
  }

  // post-processor

  /**
   * Modify the application context's internal bean factory after its standard
   * initialization. The initial definition resources will have been loaded but no
   * post-processors will have run and no derived bean definitions will have been
   * registered, and most importantly, no beans will have been instantiated yet.
   * <p>This template method allows for registering special BeanPostProcessors
   * etc in certain AbstractApplicationContext subclasses.
   *
   * @param beanFactory the bean factory used by the application context
   */
  protected void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) { }

  /**
   * Instantiate and invoke all registered BeanFactoryPostProcessor beans,
   * respecting explicit order if given.
   * <p>Must be called before singleton instantiation.
   */
  protected void invokeBeanFactoryPostProcessors(ConfigurableBeanFactory beanFactory) {
    logger.debug("Invoking bean-factory-post-processors");
    PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, factoryPostProcessors);

    // Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime
    // (e.g. through an @Bean method registered by ConfigurationClassPostProcessor)
    if (!NativeDetector.inNativeImage()
            && beanFactory.getTempClassLoader() == null
            && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
      beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
      beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
    }
  }

  /**
   * Instantiate and register all BeanPostProcessor beans,
   * respecting explicit order if given.
   * <p>Must be called before any instantiation of application beans.
   */
  protected void registerBeanPostProcessors(ConfigurableBeanFactory beanFactory) {
    logger.debug("Registering bean-post-processors");
    PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
  }

  /**
   * Initialize the MessageSource.
   * Use parent's if none defined in this context.
   */
  protected void initMessageSource() {
    ConfigurableBeanFactory beanFactory = getBeanFactory();
    if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
      this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
      // Make MessageSource aware of parent MessageSource.
      if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource hms) {
        if (hms.getParentMessageSource() == null) {
          // Only set parent context as parent MessageSource if no parent MessageSource
          // registered already.
          hms.setParentMessageSource(getInternalParentMessageSource());
        }
      }
      if (logger.isTraceEnabled()) {
        logger.trace("Using MessageSource [{}]", messageSource);
      }
    }
    else {
      // Use empty MessageSource to be able to accept getMessage calls.
      DelegatingMessageSource dms = new DelegatingMessageSource();
      dms.setParentMessageSource(getInternalParentMessageSource());
      this.messageSource = dms;
      beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, messageSource);
      if (logger.isTraceEnabled()) {
        logger.trace("No '{}' bean, using [{}]", MESSAGE_SOURCE_BEAN_NAME, messageSource);
      }
    }
  }

  /**
   * Cancel this context's refresh attempt, after an exception got thrown.
   *
   * @param ex the exception that led to the cancellation
   */
  protected void cancelRefresh(Throwable ex) {
    this.active.set(false);

    // Reset common introspection caches in core infrastructure.
    resetCommonCaches();
  }

  /**
   * Actually performs context closing: publishes a ContextClosedEvent and
   * destroys the singletons in the bean factory of this application context.
   * <p>Called by both {@code close()} and a JVM shutdown hook, if any.
   *
   * @see ContextClosedEvent
   * @see #destroyBeans()
   * @see #close()
   * @since 4.0
   */
  protected void doClose() {
    // Check whether an actual close attempt is necessary...
    if (active.get() && closed.compareAndSet(false, true)) {
      logger.info("Closing: [{}] at [{}]", this, Instant.now());

      try {
        // Publish shutdown event.
        publishEvent(new ContextClosedEvent(this));
      }
      catch (Throwable ex) {
        logger.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", ex);
      }

      // Stop all Lifecycle beans, to avoid delays during individual destruction.
      if (lifecycleProcessor != null) {
        try {
          lifecycleProcessor.onClose();
        }
        catch (Throwable ex) {
          logger.warn("Exception thrown from LifecycleProcessor on context close", ex);
        }
      }
      // Destroy all cached singletons in the context's BeanFactory.
      destroyBeans();

      // Close the state of this context itself.
      closeBeanFactory();

      // Let subclasses do some final clean-up if they wish...
      onClose();

      // Reset common introspection caches to avoid class reference leaks.
      resetCommonCaches();

      // Reset local application listeners to pre-refresh state.
      if (earlyApplicationListeners != null) {
        applicationListeners.clear();
        applicationListeners.addAll(earlyApplicationListeners);
      }

      // Reset internal delegates.
      this.applicationEventMulticaster = null;
      this.messageSource = null;
      this.lifecycleProcessor = null;

      // Switch to inactive.
      active.set(false);
    }
  }

  /**
   * Register a shutdown hook {@linkplain Thread#getName() named}
   * {@code ContextShutdownHook} with the JVM runtime, closing this
   * context on JVM shutdown unless it has already been closed at that time.
   * <p>Delegates to {@code doClose()} for the actual closing procedure.
   *
   * @see Runtime#addShutdownHook
   * @see ConfigurableApplicationContext#SHUTDOWN_HOOK_THREAD_NAME
   * @see #close()
   * @see #doClose()
   */
  @Override
  public void registerShutdownHook() {
    if (shutdownHook == null) {
      // No shutdown hook registered yet.
      this.shutdownHook = new Thread(SHUTDOWN_HOOK_THREAD_NAME) {
        @Override
        public void run() {
          if (isStartupShutdownThreadStuck()) {
            active.set(false);
            return;
          }
          startupShutdownLock.lock();
          try {
            doClose();
          }
          finally {
            startupShutdownLock.unlock();
          }
        }
      };
      Runtime.getRuntime().addShutdownHook(shutdownHook);
    }
  }

  /**
   * Determine whether an active startup/shutdown thread is currently stuck,
   * e.g. through a {@code System.exit} call in a user component.
   */
  private boolean isStartupShutdownThreadStuck() {
    Thread activeThread = this.startupShutdownThread;
    if (activeThread != null && activeThread.getState() == Thread.State.WAITING) {
      // Indefinitely waiting: might be Thread.join or the like, or System.exit
      activeThread.interrupt();
      try {
        // Leave just a little bit of time for the interruption to show effect
        Thread.sleep(1);
      }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }

      // Interrupted but still waiting: very likely a System.exit call
      return activeThread.getState() == Thread.State.WAITING;
    }
    return false;
  }

  /**
   * Close this application context, destroying all beans in its bean factory.
   * <p>Delegates to {@code doClose()} for the actual closing procedure.
   * Also removes a JVM shutdown hook, if registered, as it's not needed anymore.
   *
   * @see #doClose()
   * @see #registerShutdownHook()
   */
  @Override
  public void close() {
    if (isStartupShutdownThreadStuck()) {
      active.set(false);
      return;
    }
    startupShutdownLock.lock();
    applyState(State.CLOSING);
    try {
      startupShutdownThread = Thread.currentThread();
      doClose();
      // If we registered a JVM shutdown hook, we don't need it anymore now:
      // We've already explicitly closed the context.
      if (shutdownHook != null) {
        try {
          Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
        catch (IllegalStateException ex) {
          // ignore - VM is already shutting down
        }
      }
    }
    finally {
      applyState(State.CLOSED);
      ApplicationContextHolder.remove(this);
      startupShutdownThread = null;
      startupShutdownLock.unlock();
    }
  }

  /**
   * Template method for destroying all beans that this context manages.
   * The default implementation destroy all cached singletons in this context,
   * invoking {@code DisposableBean.destroy()} and/or the specified
   * "destroy-method".
   * <p>Can be overridden to add context-specific bean destruction steps
   * right before or right after standard singleton destruction,
   * while the context's BeanFactory is still active.
   *
   * @see #getBeanFactory()
   * @see ConfigurableBeanFactory#destroySingletons()
   * @since 4.0
   */
  protected void destroyBeans() {
    getBeanFactory().destroySingletons();
  }

  /**
   * Template method which can be overridden to add context-specific shutdown work.
   * The default implementation is empty.
   * <p>Called at the end of {@link #doClose}'s shutdown procedure, after
   * this context's BeanFactory has been closed. If custom shutdown logic
   * needs to execute while the BeanFactory is still active, override
   * the {@link #destroyBeans()} method instead.
   *
   * @since 4.0
   */
  protected void onClose() {
    // For subclasses: do nothing by default.
  }

  @Override
  public boolean isClosed() {
    return this.closed.get();
  }

  @Override
  public boolean isActive() {
    return active.get();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T unwrapFactory(Class<T> requiredType) {
    ConfigurableBeanFactory beanFactory = getBeanFactory();
    if (requiredType.isInstance(beanFactory)) {
      return (T) beanFactory;
    }
    throw new IllegalArgumentException("bean factory must be a " + requiredType);
  }

  @Override
  public State getState() {
    return state;
  }

  protected void applyState(State state) {
    this.state = state;
  }

  @Override
  public Instant getStartupDate() {
    return startupDate;
  }

  //---------------------------------------------------------------------
  // Implementation of ConfigurableApplicationContext interface
  //---------------------------------------------------------------------

  /**
   * Set the parent of this application context.
   * <p>The parent {@linkplain ApplicationContext#getEnvironment() environment} is
   * {@linkplain ConfigurableEnvironment#merge(ConfigurableEnvironment) merged} with
   * this (child) application context environment if the parent is non-{@code null} and
   * its environment is an instance of {@link ConfigurableEnvironment}.
   *
   * @see ConfigurableEnvironment#merge(ConfigurableEnvironment)
   */
  @Override
  public void setParent(@Nullable ApplicationContext parent) {
    this.parent = parent;
    if (parent != null) {
      Environment parentEnvironment = parent.getEnvironment();
      if (parentEnvironment instanceof ConfigurableEnvironment) {
        getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
      }
    }
  }

  @Override
  public ConfigurableEnvironment getEnvironment() {
    ConfigurableEnvironment environment = this.environment;
    if (environment == null) {
      environment = createEnvironment();
      this.environment = environment;
    }
    return environment;
  }

  /**
   * Create and return a new {@link StandardEnvironment}.
   * <p>Subclasses may override this method in order to supply
   * a custom {@link ConfigurableEnvironment} implementation.
   */
  protected ConfigurableEnvironment createEnvironment() {
    return new StandardEnvironment();
  }

  @Override
  public void setEnvironment(@Nullable ConfigurableEnvironment environment) {
    this.environment = environment;
  }

  @Override
  public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
    Assert.notNull(postProcessor, "BeanFactoryPostProcessor is required");

    factoryPostProcessors.add(postProcessor);
  }

  //---------------------------------------------------------------------
  // Implementation of BeanFactory interface
  //---------------------------------------------------------------------

  /**
   * Assert that this context's BeanFactory is currently active,
   * throwing an {@link IllegalStateException} if it isn't.
   * <p>Invoked by all {@link BeanFactory} delegation methods that depend
   * on an active context, i.e. in particular all bean accessor methods.
   */
  protected void assertBeanFactoryActive() {
    if (!active.get()) {
      if (closed.get()) {
        throw new IllegalStateException(getDisplayName() + " has been closed already");
      }
      else {
        throw new IllegalStateException(getDisplayName() + " has not been refreshed yet");
      }
    }
  }

  @Nullable
  @Override
  public Object getBean(String name) {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(name);
  }

  @Nullable
  @Override
  public Object getBean(String name, Object... args) throws BeansException {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(name, args);
  }

  @Override
  public <T> T getBean(Class<T> requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(requiredType);
  }

  @Override
  public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(requiredType, args);
  }

  @Override
  public <T> T getBean(String name, Class<T> requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(name, requiredType);
  }

  @Nullable
  @Override
  public <A extends Annotation> A findSynthesizedAnnotation(String beanName, Class<A> annotationType) {
    assertBeanFactoryActive();
    return getBeanFactory().findSynthesizedAnnotation(beanName, annotationType);
  }

  @Override
  public <A extends Annotation> MergedAnnotation<A> findAnnotationOnBean(
          String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
    assertBeanFactoryActive();
    return getBeanFactory().findAnnotationOnBean(beanName, annotationType);
  }

  @Override
  public <A extends Annotation> MergedAnnotation<A> findAnnotationOnBean(
          String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
          throws NoSuchBeanDefinitionException {
    assertBeanFactoryActive();
    return getBeanFactory().findAnnotationOnBean(beanName, annotationType, allowFactoryBeanInit);
  }

  @Override
  public <A extends Annotation> Set<A> findAllAnnotationsOnBean(
          String beanName, Class<A> annotationType, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
    assertBeanFactoryActive();
    return getBeanFactory().findAllAnnotationsOnBean(beanName, annotationType, allowFactoryBeanInit);
  }

  @Override
  public <T> List<T> getAnnotatedBeans(Class<? extends Annotation> annotationType) {
    assertBeanFactoryActive();
    return getBeanFactory().getAnnotatedBeans(annotationType);
  }

  @Override
  public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeansWithAnnotation(annotationType);
  }

  @Override
  public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType, boolean includeNonSingletons) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeansWithAnnotation(annotationType, includeNonSingletons);
  }

  @Override
  public boolean isSingleton(String name) {
    assertBeanFactoryActive();
    return getBeanFactory().isSingleton(name);
  }

  @Override
  public boolean isPrototype(String name) {
    assertBeanFactoryActive();
    return getBeanFactory().isPrototype(name);
  }

  @Nullable
  @Override
  public Class<?> getType(String name) {
    assertBeanFactoryActive();
    return getBeanFactory().getType(name);
  }

  @Nullable
  @Override
  public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
    assertBeanFactoryActive();
    return getBeanFactory().getType(name, allowFactoryBeanInit);
  }

  @Override
  public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanNamesForAnnotation(annotationType);
  }

  @Override
  public boolean containsBean(String name) {
    return getBeanFactory().containsBean(name);
  }

  @Override
  public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
    assertBeanFactoryActive();
    return getBeanFactory().isTypeMatch(name, typeToMatch);
  }

  @Override
  public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
    assertBeanFactoryActive();
    return getBeanFactory().isTypeMatch(name, typeToMatch);
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanProvider(requiredType);
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanProvider(requiredType);
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
  }

  @Override
  public String[] getAliases(String name) {
    assertBeanFactoryActive();
    return getBeanFactory().getAliases(name);
  }

  // type lookup

  @Override
  public <T> List<T> getBeans(Class<T> requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeans(requiredType);
  }

  @Override
  public String[] getBeanNamesForType(@Nullable Class<?> type) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanNamesForType(type);
  }

  @Override
  public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(Class<T> type) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeansOfType(type);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(@Nullable Class<T> requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeansOfType(requiredType, includeNonSingletons, allowEagerInit);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeansOfType(requiredType, includeNonSingletons, allowEagerInit);
  }

  @Override
  public String[] getBeanNamesForType(ResolvableType requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanNamesForType(requiredType);
  }

  @Override
  public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
  }

  @Override
  public boolean containsBeanDefinition(String beanName) {
    assertBeanFactoryActive();
    return getBeanFactory().containsBeanDefinition(beanName);
  }

  @Override
  public BeanDefinition getBeanDefinition(String beanName) throws BeansException {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanDefinition(beanName);
  }

  @Override
  public int getBeanDefinitionCount() {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanDefinitionCount();
  }

  @Override
  public String[] getBeanDefinitionNames() {
    assertBeanFactoryActive();
    return new String[0];
  }

  @Override
  public DependencyInjector getInjector() {
    return getBeanFactory().getInjector();
  }

  //---------------------------------------------------------------------
  // Implementation of Lifecycle interface
  //---------------------------------------------------------------------

  @Override
  public void start() {
    getLifecycleProcessor().start();
    publishEvent(new ContextStartedEvent(this));
  }

  @Override
  public void stop() {
    getLifecycleProcessor().stop();
    publishEvent(new ContextStoppedEvent(this));
  }

  @Override
  public void restart() {
    getLifecycleProcessor().onRestart();
    publishEvent(new ContextRestartedEvent(this));
  }

  @Override
  public boolean isRunning() {
    return this.lifecycleProcessor != null && this.lifecycleProcessor.isRunning();
  }

  // lifecycleProcessor

  /**
   * Initialize the LifecycleProcessor.
   * Uses DefaultLifecycleProcessor if none defined in the context.
   *
   * @see DefaultLifecycleProcessor
   */
  protected void initLifecycleProcessor() {
    if (lifecycleProcessor == null) {
      ConfigurableBeanFactory beanFactory = getBeanFactory();
      if (beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
        this.lifecycleProcessor = beanFactory.getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
        if (logger.isTraceEnabled()) {
          logger.trace("Using LifecycleProcessor [{}]", lifecycleProcessor);
        }
      }
      else {
        DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
        defaultProcessor.setBeanFactory(beanFactory);
        this.lifecycleProcessor = defaultProcessor;
        beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);
        if (logger.isTraceEnabled()) {
          logger.trace("No '{}' bean, using [{}]", LIFECYCLE_PROCESSOR_BEAN_NAME, lifecycleProcessor.getClass().getSimpleName());
        }
      }
    }
  }

  /**
   * Return the internal LifecycleProcessor used by the context.
   *
   * @return the internal LifecycleProcessor (never {@code null})
   * @throws IllegalStateException if the context has not been initialized yet
   */
  public LifecycleProcessor getLifecycleProcessor() throws IllegalStateException {
    if (this.lifecycleProcessor == null) {
      throw new IllegalStateException(
              "LifecycleProcessor not initialized - call 'refresh' before invoking lifecycle methods via the context: " + this);
    }
    return this.lifecycleProcessor;
  }

  //---------------------------------------------------------------------
  // Implementation of ApplicationEventPublisher interface
  //---------------------------------------------------------------------

  /**
   * Return the internal ApplicationEventMulticaster used by the context.
   *
   * @return the internal ApplicationEventMulticaster (never {@code null})
   * @throws IllegalStateException if the context has not been initialized yet
   */
  public ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
    if (this.applicationEventMulticaster == null) {
      throw new IllegalStateException(
              "ApplicationEventMulticaster not initialized - call 'refresh' before multicasting events via the context: " + this);
    }
    return this.applicationEventMulticaster;
  }

  /**
   * Initialize the ApplicationEventMulticaster.
   * Uses SimpleApplicationEventMulticaster if none defined in the context.
   *
   * @see SimpleApplicationEventMulticaster
   */
  protected void initApplicationEventMulticaster() {
    ConfigurableBeanFactory beanFactory = getBeanFactory();
    if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
      this.applicationEventMulticaster =
              beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
      if (logger.isTraceEnabled()) {
        logger.trace("Using ApplicationEventMulticaster [{}]", applicationEventMulticaster);
      }
    }
    else {
      this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
      beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, applicationEventMulticaster);
      if (logger.isTraceEnabled()) {
        logger.trace("No '{}' bean, using [{}]",
                APPLICATION_EVENT_MULTICASTER_BEAN_NAME, applicationEventMulticaster.getClass().getSimpleName());
      }
    }
  }

  /**
   * Return the list of statically specified ApplicationListeners.
   */
  public Collection<ApplicationListener<?>> getApplicationListeners() {
    return this.applicationListeners;
  }

  /**
   * Publish the given event to all listeners.
   * <p>Note: Listeners get initialized after the MessageSource, to be able
   * to access it within listener implementations. Thus, MessageSource
   * implementations cannot publish events.
   *
   * @param event the event to publish (may be application-specific or a
   * standard framework event)
   */
  @Override
  public void publishEvent(ApplicationEvent event) {
    publishEvent(event, null);
  }

  /**
   * Publish the given event to all listeners.
   * <p>Note: Listeners get initialized after the MessageSource, to be able
   * to access it within listener implementations. Thus, MessageSource
   * implementations cannot publish events.
   *
   * @param event the event to publish (may be an {@link ApplicationEvent}
   * or a payload object to be turned into a {@link PayloadApplicationEvent})
   */
  @Override
  public void publishEvent(Object event) {
    publishEvent(event, null);
  }

  /**
   * Publish the given event to all listeners.
   * <p>This is the internal delegate that all other {@code publishEvent}
   * methods refer to. It is not meant to be called directly but rather serves
   * as a propagation mechanism between application contexts in a hierarchy,
   * potentially overridden in subclasses for a custom propagation arrangement.
   *
   * @param event the event to publish (may be an {@link ApplicationEvent}
   * or a payload object to be turned into a {@link PayloadApplicationEvent})
   * @param typeHint the resolved event type, if known.
   * The implementation of this method also tolerates a payload type hint for
   * a payload object to be turned into a {@link PayloadApplicationEvent}.
   * However, the recommended way is to construct an actual event object via
   * {@link PayloadApplicationEvent#PayloadApplicationEvent(Object, Object, ResolvableType)}
   * instead for such scenarios.
   * @see ApplicationEventMulticaster#multicastEvent(ApplicationEvent, ResolvableType)
   * @since 4.0
   */
  protected void publishEvent(Object event, @Nullable ResolvableType typeHint) {
    Assert.notNull(event, "Event is required");
    ResolvableType eventType = null;

    // Decorate event as an ApplicationEvent if necessary
    ApplicationEvent applicationEvent;
    if (event instanceof ApplicationEvent applEvent) {
      applicationEvent = applEvent;
      eventType = typeHint;
    }
    else {
      ResolvableType payloadType = null;
      if (typeHint != null && ApplicationEvent.class.isAssignableFrom(typeHint.toClass())) {
        eventType = typeHint;
      }
      else {
        payloadType = typeHint;
      }
      applicationEvent = new PayloadApplicationEvent<>(this, event, payloadType);
    }

    // Determine event type only once (for multicast and parent publish)
    if (eventType == null) {
      eventType = ResolvableType.forInstance(applicationEvent);
      if (typeHint == null) {
        typeHint = eventType;
      }
    }

    // Multicast right now if possible - or lazily once the multicaster is initialized
    if (this.earlyApplicationEvents != null) {
      this.earlyApplicationEvents.add(applicationEvent);
    }
    else if (this.applicationEventMulticaster != null) {
      this.applicationEventMulticaster.multicastEvent(applicationEvent, eventType);
    }

    // Publish event via parent context as well...
    if (parent != null) {
      if (parent instanceof AbstractApplicationContext parentCtx) {
        parentCtx.publishEvent(event, typeHint);
      }
      else {
        this.parent.publishEvent(event);
      }
    }
  }

  protected void registerApplicationListeners() {
    logger.debug("Registering application-listeners");

    // Register statically specified listeners first.
    ApplicationEventMulticaster eventMulticaster = getApplicationEventMulticaster();
    for (ApplicationListener<?> listener : getApplicationListeners()) {
      eventMulticaster.addApplicationListener(listener);
    }

    // Do not initialize FactoryBeans here: We need to leave all regular beans
    // uninitialized to let post-processors apply to them!
    var listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
    for (String listenerBeanName : listenerBeanNames) {
      eventMulticaster.addApplicationListenerBean(listenerBeanName);
    }

    logger.debug("Publish early application events");
    // Publish early application events now that we finally have a multicaster...
    Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
    this.earlyApplicationEvents = null;
    if (CollectionUtils.isNotEmpty(earlyEventsToProcess)) {
      for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
        eventMulticaster.multicastEvent(earlyEvent);
      }
    }
  }

  /**
   * Add a new ApplicationListener that will be notified on context events
   * such as context refresh and context shutdown.
   * <p>Note that any ApplicationListener registered here will be applied
   * on refresh if the context is not active yet, or on the fly with the
   * current event multicaster in case of a context that is already active.
   *
   * @param listener the ApplicationListener to register
   * @see ContextRefreshedEvent
   * @see ContextClosedEvent
   */
  @Override
  public void addApplicationListener(ApplicationListener<?> listener) {
    Assert.notNull(listener, "ApplicationListener is required");
    if (applicationEventMulticaster != null) {
      applicationEventMulticaster.addApplicationListener(listener);
    }
    applicationListeners.add(listener);
  }

  @Override
  public void removeApplicationListener(ApplicationListener<?> listener) {
    Assert.notNull(listener, "ApplicationListener is required");
    if (applicationEventMulticaster != null) {
      applicationEventMulticaster.removeApplicationListener(listener);
    }
    applicationListeners.remove(listener);
  }

  /**
   * Finish the initialization of this context's bean factory,
   * initializing all remaining singleton beans.
   */
  protected void finishBeanFactoryInitialization(ConfigurableBeanFactory beanFactory) {
    // Initialize bootstrap executor for this context.
    if (beanFactory.containsBean(BOOTSTRAP_EXECUTOR_BEAN_NAME) &&
            beanFactory.isTypeMatch(BOOTSTRAP_EXECUTOR_BEAN_NAME, Executor.class)) {
      beanFactory.setBootstrapExecutor(
              beanFactory.getBean(BOOTSTRAP_EXECUTOR_BEAN_NAME, Executor.class));
    }

    // Initialize conversion service for this context.
    if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME)
            && beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
      beanFactory.setConversionService(
              beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
    }

    // Register a default embedded value resolver if no BeanFactoryPostProcessor
    // (such as a PropertySourcesPlaceholderConfigurer bean) registered any before:
    // at this point, primarily for resolution in annotation attribute values.
    if (!beanFactory.hasEmbeddedValueResolver()) {
      beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolveRequiredPlaceholders(strVal));
    }

    // Call BeanFactoryInitializer beans early to allow for initializing specific other beans early.
    var initializerNames = beanFactory.getBeanNamesForType(BeanFactoryInitializer.class, false, false);
    for (String initializerName : initializerNames) {
      beanFactory.getBean(initializerName, BeanFactoryInitializer.class).initialize(beanFactory);
    }

    // Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
    var weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
    for (String weaverAwareName : weaverAwareNames) {
      try {
        beanFactory.getBean(weaverAwareName, LoadTimeWeaverAware.class);
      }
      catch (BeanNotOfRequiredTypeException ex) {
        logger.debug("Failed to initialize LoadTimeWeaverAware bean '{}' due to unexpected type mismatch: {}",
                weaverAwareName, ex.getMessage());
      }
    }

    // Stop using the temporary ClassLoader for type matching.
    beanFactory.setTempClassLoader(null);

    // Allow for caching all bean definition metadata, not expecting further changes.
    beanFactory.freezeConfiguration();

    // Instantiate all remaining (non-lazy-init) singletons.
    beanFactory.preInstantiateSingletons();
  }

  /**
   * Finish the refresh of this context, invoking the LifecycleProcessor's
   * onRefresh() method and publishing the {@link ContextRefreshedEvent}.
   */
  protected void finishRefresh() {
    // Reset common introspection caches in core infrastructure.
    resetCommonCaches();

    // Clear context-level resource caches (such as ASM metadata from scanning).
    clearResourceCaches();

    // Initialize lifecycle processor for this context.
    initLifecycleProcessor();

    // Propagate refresh to lifecycle processor first.
    getLifecycleProcessor().onRefresh();

    // Publish the final event.
    publishEvent(new ContextRefreshedEvent(this));

    applyState(State.STARTED);
    Duration duration = Duration.between(getStartupDate(), Instant.now());
    logger.info("Application context startup in {} ms", duration.toMillis());
  }

  @Override
  public void clearResourceCaches() {
    super.clearResourceCaches();
    if (patternResourceLoader instanceof PathMatchingPatternResourceLoader pmprl) {
      pmprl.clearCache();
    }
  }

  //---------------------------------------------------------------------
  // Abstract methods that must be implemented by subclasses
  //---------------------------------------------------------------------

  /**
   * Subclasses must implement this method to perform the actual configuration load.
   * The method is invoked by {@link #refresh()} before any other initialization work.
   * <p>A subclass will either create a new bean factory and hold a reference to it,
   * or return a single BeanFactory instance that it holds. In the latter case, it will
   * usually throw an IllegalStateException if refreshing the context more than once.
   *
   * @throws BeansException if initialization of the bean factory failed
   * @throws IllegalStateException if already initialized and multiple refresh
   * attempts are not supported
   * @since 4.0
   */
  protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;

  /**
   * Subclasses must implement this method to release their internal bean factory.
   * This method gets invoked by {@link #close()} after all other shutdown work.
   * <p>Should never throw an exception but rather log shutdown failures.
   *
   * @since 4.0
   */
  protected void closeBeanFactory() { }

  /**
   * Subclasses must return their internal bean factory here. They should implement the
   * lookup efficiently, so that it can be called repeatedly without a performance penalty.
   * <p>Note: Subclasses should check whether the context is still active before
   * returning the internal bean factory. The internal factory should generally be
   * considered unavailable once the context has been closed.
   *
   * @return this application context's internal bean factory (never {@code null})
   * @throws IllegalStateException if the context does not hold an internal bean factory yet
   * (usually if {@link #refresh()} has never been called) or if the context has been
   * closed already
   * @see #refreshBeanFactory()
   * @see #closeBeanFactory()
   */
  @Override
  public abstract ConfigurableBeanFactory getBeanFactory();

  // Object

  /**
   * Return information about this context.
   */
  @Override
  public String toString() {
    return "%s: state: [%s], on startup date: %s, parent: %s".formatted(
            getDisplayName(), state, startupDate, parent != null ? parent.getDisplayName() : "none");
  }

}
