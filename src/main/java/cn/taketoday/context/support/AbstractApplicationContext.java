/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context.support;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.BeanPostProcessor;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.ObjectSupplier;
import cn.taketoday.beans.factory.support.AbstractBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.DependencyInjector;
import cn.taketoday.beans.support.ResourceEditorRegistrar;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.HierarchicalMessageSource;
import cn.taketoday.context.Lifecycle;
import cn.taketoday.context.LifecycleProcessor;
import cn.taketoday.context.MessageSource;
import cn.taketoday.context.MessageSourceAware;
import cn.taketoday.context.MessageSourceResolvable;
import cn.taketoday.context.NoSuchMessageException;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.aware.ApplicationContextAwareProcessor;
import cn.taketoday.context.aware.ApplicationEventPublisherAware;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.context.aware.ResourceLoaderAware;
import cn.taketoday.context.event.ApplicationEvent;
import cn.taketoday.context.event.ApplicationEventMulticaster;
import cn.taketoday.context.event.ApplicationEventPublisher;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.context.event.ApplicationListenerDetector;
import cn.taketoday.context.event.ContextClosedEvent;
import cn.taketoday.context.event.ContextRefreshedEvent;
import cn.taketoday.context.event.ContextStartedEvent;
import cn.taketoday.context.event.ContextStoppedEvent;
import cn.taketoday.context.event.SimpleApplicationEventMulticaster;
import cn.taketoday.context.expression.EmbeddedValueResolverAware;
import cn.taketoday.context.expression.ExpressionEvaluator;
import cn.taketoday.context.expression.StandardBeanExpressionResolver;
import cn.taketoday.context.weaving.LoadTimeWeaverAware;
import cn.taketoday.context.weaving.LoadTimeWeaverAwareProcessor;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceConsumer;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Abstract implementation of the {@link ApplicationContext}
 * interface. Doesn't mandate the type of storage used for configuration; simply
 * implements common context functionality. Uses the Template Method design pattern,
 * requiring concrete subclasses to implement abstract methods.
 *
 * <p>In contrast to a plain BeanFactory, an ApplicationContext is supposed
 * to detect special beans defined in its internal bean factory:
 * Therefore, this class automatically registers
 * {@link BeanFactoryPostProcessor BeanFactoryPostProcessors},
 * {@link BeanPostProcessor BeanPostProcessors}, and
 * {@link ApplicationListener ApplicationListeners} which are defined as beans in the context.
 *
 * <p>Implements resource loading by extending {@link PathMatchingPatternResourceLoader}.
 * Consequently, treats non-URL resource paths as class path resources
 * (supporting full class path resource names that include the package path,
 * e.g. "mypackage/myresource.dat")
 *
 * @author TODAY 2018-09-09 22:02
 */
@SuppressWarnings({ "unchecked" })
public abstract class AbstractApplicationContext
        extends DefaultResourceLoader implements ConfigurableApplicationContext, Lifecycle {
  protected final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Name of the ApplicationEventMulticaster bean in the factory.
   * If none is supplied, a default SimpleApplicationEventMulticaster is used.
   *
   * @see ApplicationEventMulticaster
   * @see SimpleApplicationEventMulticaster
   */
  public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";

  /**
   * Name of the MessageSource bean in the factory.
   * If none is supplied, message resolution is delegated to the parent.
   *
   * @see MessageSource
   */
  public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

  /**
   * Name of the LifecycleProcessor bean in the factory.
   * If none is supplied, a DefaultLifecycleProcessor is used.
   *
   * @see LifecycleProcessor
   * @see DefaultLifecycleProcessor
   */
  public static final String LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor";

  private long startupDate;

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

  /** Display name. */
  private String applicationName = ObjectUtils.identityToString(this);

  /** @since 4.0 */
  private final PathMatchingPatternResourceLoader patternResourceLoader
          = new PathMatchingPatternResourceLoader(this);

  /** @since 4.0 */
  private boolean refreshable;

  /** @since 4.0 */
  private ExpressionEvaluator expressionEvaluator;

  /** Flag that indicates whether this context has been closed already. @since 4.0 */
  private final AtomicBoolean closed = new AtomicBoolean();

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
  private Set<Object> earlyApplicationEvents;

  /** MessageSource we delegate our implementation of this interface to. @since 4.0 */
  @Nullable
  private MessageSource messageSource;

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
   * Set a friendly name for this context.
   * Typically, done during initialization of concrete context implementations.
   * <p>Default is the object id of the context instance.
   */
  public void setApplicationName(String applicationName) {
    Assert.hasLength(applicationName, "Application name must not be empty");
    ApplicationContextHolder.remove(this);
    this.applicationName = applicationName;
    ApplicationContextHolder.register(this); // @since 4.0
  }

  /**
   * Return this application name for this context.
   *
   * @return a display name for this context (never {@code null})
   */
  @Override
  public String getApplicationName() {
    return applicationName;
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
    return unwrapFactory(AutowireCapableBeanFactory.class);
  }

  @Override
  public ExpressionEvaluator getExpressionEvaluator() {
    if (expressionEvaluator == null) {
      expressionEvaluator = new ExpressionEvaluator(this);
      expressionEvaluator.setBeanFactory(getBeanFactory());
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
    return (getParent() instanceof ConfigurableApplicationContext ?
            ((ConfigurableApplicationContext) getParent()).getBeanFactory() : getParent());
  }

  //---------------------------------------------------------------------
  // Implementation of MessageSource interface
  //---------------------------------------------------------------------

  @Override
  public String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
    return getMessageSource().getMessage(code, args, defaultMessage, locale);
  }

  @Override
  public String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
    return getMessageSource().getMessage(code, args, locale);
  }

  @Override
  public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
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
      throw new IllegalStateException("MessageSource not initialized - " +
              "call 'refresh' before accessing messages via the context: " + this);
    }
    return this.messageSource;
  }

  /**
   * Return the internal message source of the parent context if it is an
   * AbstractApplicationContext too; else, return the parent context itself.
   */
  @Nullable
  protected MessageSource getInternalParentMessageSource() {
    return getParent() instanceof AbstractApplicationContext ?
           ((AbstractApplicationContext) getParent()).messageSource : getParent();
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
    if (this.shutdownHook == null) {
      // No shutdown hook registered yet.
      this.shutdownHook = new Thread(this::close, SHUTDOWN_HOOK_THREAD_NAME);
      Runtime.getRuntime().addShutdownHook(this.shutdownHook);
    }
  }

  //---------------------------------------------------------------------
  // Implementation of ApplicationContext interface
  //---------------------------------------------------------------------

  @Override
  public void refresh() throws IllegalStateException {
    synchronized(this) {
      assertRefreshable();
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
      catch (Exception ex) {
        log.warn("Exception encountered during context initialization - cancelling refresh attempt: {}",
                ex.toString());

        cancelRefresh(ex);
        applyState(State.FAILED);
        throw new ApplicationContextException("context refresh failed, cause: " + ex.getMessage(), ex);
      }
      finally {
        resetCommonCaches();
      }
    }
  }

  private void assertRefreshable() {
    if (!refreshable &&
            (state == State.STARTED || state == State.STARTING || state == State.CLOSING)) {
      throw new IllegalStateException("this context not supports refresh again");
    }
  }

  /**
   * Prepare to load context
   */
  protected void prepareRefresh() {
    this.startupDate = System.currentTimeMillis();
    this.closed.set(false);
    applyState(State.STARTING);
    ApplicationContextHolder.register(this); // @since 4.0

    log.info("Starting application context at '{}'", formatStartupDate());

    ConfigurableEnvironment environment = getEnvironment();

    // Initialize any placeholder property sources in the context environment.
    initPropertySources();

    environment.validateRequiredProperties();

    // @since 4.0
    String appName = environment.getProperty(APPLICATION_NAME);
    if (StringUtils.hasText(appName)) {
      setApplicationName(appName);
    }

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

    if (log.isDebugEnabled()) {
      if (log.isTraceEnabled()) {
        log.trace("Refreshing " + this);
      }
      else {
        log.debug("Refreshing " + getDisplayName());
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
    log.debug("Registering framework components");

    // @since 4.0 ArgumentsResolver
    beanFactory.registerSingleton(getInjector());

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

    ExpressionEvaluator.register(beanFactory, getEnvironment());
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
  public void prepareBeanFactory(ConfigurableBeanFactory beanFactory) {
    log.debug("Preparing bean-factory: {}", beanFactory);
    // Tell the internal bean factory to use the context's class loader etc.
    beanFactory.setBeanClassLoader(getClassLoader());
    beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver());
    beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

    // Configure the bean factory with context callbacks.
    beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
    // Register early post-processor for detecting inner beans as ApplicationListeners.
    beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

    // Detect a LoadTimeWeaver and prepare for weaving, if found.
    if (beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
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
    beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);

    // BeanFactory interface not registered as resolvable type in a plain factory.
    // MessageSource registered (and found for autowiring) as a bean.
    beanFactory.registerDependency(BeanFactory.class, beanFactory);
    beanFactory.registerDependency(ResourceLoader.class, this);
    beanFactory.registerDependency(ApplicationEventPublisher.class, this);
    beanFactory.registerDependency(ApplicationContext.class, this);
  }

  // post-processor

  /**
   * Modify the application context's internal bean factory after its standard
   * initialization. All bean definitions will have been loaded, but no beans
   * will have been instantiated yet. This allows for registering special
   * BeanPostProcessors etc in certain ApplicationContext implementations.
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
    log.debug("Invoking bean-factory-post-processors");
    PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, factoryPostProcessors);

    // Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime
    // (e.g. through an @Bean method registered by ConfigurationClassPostProcessor)
    if (beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
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
    log.debug("Registering bean-post-processors");
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
      if (log.isTraceEnabled()) {
        log.trace("Using MessageSource [{}]", messageSource);
      }
    }
    else {
      // Use empty MessageSource to be able to accept getMessage calls.
      DelegatingMessageSource dms = new DelegatingMessageSource();
      dms.setParentMessageSource(getInternalParentMessageSource());
      this.messageSource = dms;
      beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
      if (log.isTraceEnabled()) {
        log.trace("No '{}' bean, using [{}]", MESSAGE_SOURCE_BEAN_NAME, messageSource);
      }
    }
  }

  /**
   * Cancel this context's refresh attempt, after an exception got thrown.
   *
   * @param ex the exception that led to the cancellation
   */
  protected void cancelRefresh(Exception ex) {
    close();
  }

  @Override
  public void close() {
    synchronized(this) {
      applyState(State.CLOSING);
      doClose();
      // If we registered a JVM shutdown hook, we don't need it anymore now:
      // We've already explicitly closed the context.
      if (this.shutdownHook != null) {
        try {
          Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
        }
        catch (IllegalStateException ex) {
          // ignore - VM is already shutting down
        }
      }
      applyState(State.CLOSED);
      ApplicationContextHolder.remove(this);
    }
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
    if (this.closed.compareAndSet(false, true)) {
      log.info("Closing: [{}] at [{}]", this,
              new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT).format(System.currentTimeMillis()));

      try {
        // Publish shutdown event.
        publishEvent(new ContextClosedEvent(this));
      }
      catch (Throwable ex) {
        log.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", ex);
      }
      // Stop all Lifecycle beans, to avoid delays during individual destruction.
      if (this.lifecycleProcessor != null) {
        try {
          this.lifecycleProcessor.onClose();
        }
        catch (Throwable ex) {
          log.warn("Exception thrown from LifecycleProcessor on context close", ex);
        }
      }
      // Destroy all cached singletons in the context's BeanFactory.
      destroyBeans();

      // Close the state of this context itself.
      closeBeanFactory();

      // Let subclasses do some final clean-up if they wish...
      onClose();

      // Reset local application listeners to pre-refresh state.
      if (this.earlyApplicationListeners != null) {
        this.applicationListeners.clear();
        this.applicationListeners.addAll(this.earlyApplicationListeners);
      }

    }
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

  @NonNull
  @Override
  @SuppressWarnings("unchecked")
  public <T> T unwrapFactory(Class<T> requiredType) {
    ConfigurableBeanFactory beanFactory = getBeanFactory();
    if (requiredType.isInstance(beanFactory)) {
      return (T) beanFactory;
    }
    throw new IllegalArgumentException("bean factory must be a " + requiredType);
  }

  @NonNull
  @Override
  public <T> T unwrap(Class<T> requiredType) {
    if (requiredType.isInstance(this)) {
      return (T) this;
    }
    throw new IllegalArgumentException("This BeanFactory '" + this + "' is not a " + requiredType);
  }

  @Override
  public boolean hasStarted() {
    return state == State.STARTED;
  }

  @Override
  public State getState() {
    return state;
  }

  protected void applyState(State state) {
    this.state = state;
  }

  @Override
  public long getStartupDate() {
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
    if (environment == null) {
      environment = createEnvironment();
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
  public void setEnvironment(ConfigurableEnvironment environment) {
    this.environment = environment;
  }

  @Override
  public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
    Assert.notNull(postProcessor, "BeanFactoryPostProcessor must not be null");

    factoryPostProcessors.add(postProcessor);
  }

  @Override
  public void setRefreshable(boolean refreshable) {
    this.refreshable = refreshable;
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
    if (!refreshable && !isActive()) {
      if (this.closed.get()) {
        throw new IllegalStateException(getDisplayName() + " has been closed already");
      }
      else {
        throw new IllegalStateException(getDisplayName() + " has not been refreshed yet");
      }
    }
  }

  @Override
  public Object getBean(String name) {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(name);
  }

  @Override
  public Object getBean(String name, Object... args) throws BeansException {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(name, args);
  }

  @Override
  @Nullable
  public <T> T getBean(Class<T> requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(requiredType);
  }

  @Override
  @Nullable
  public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(requiredType, args);
  }

  @Override
  public <T> T getBean(String name, Class<T> requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(name, requiredType);
  }

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

  @Nullable
  @Override
  public <A extends Annotation> MergedAnnotation<A> findAnnotationOnBean(
          String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
          throws NoSuchBeanDefinitionException {
    assertBeanFactoryActive();
    return getBeanFactory().findAnnotationOnBean(beanName, annotationType, allowFactoryBeanInit);
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
  public Map<String, BeanDefinition> getBeanDefinitions() {
    return getBeanFactory().getBeanDefinitions();
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
  public Set<String> getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
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
  public <T> ObjectSupplier<T> getObjectSupplier(Class<T> requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getObjectSupplier(requiredType);
  }

  @Override
  public <T> ObjectSupplier<T> getObjectSupplier(ResolvableType requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getObjectSupplier(requiredType);
  }

  @Override
  public <T> ObjectSupplier<T> getObjectSupplier(Class<T> requiredType, boolean allowEagerInit) {
    assertBeanFactoryActive();
    return getBeanFactory().getObjectSupplier(requiredType, allowEagerInit);
  }

  @Override
  public <T> ObjectSupplier<T> getObjectSupplier(ResolvableType requiredType, boolean allowEagerInit) {
    assertBeanFactoryActive();
    return getBeanFactory().getObjectSupplier(requiredType, allowEagerInit);
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
  public Set<String> getBeanNamesForType(Class<?> requiredType, boolean includeNonSingletons) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanNamesForType(requiredType, includeNonSingletons);
  }

  @Override
  public Set<String> getBeanNamesForType(
          Class<?> requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanNamesForType(requiredType, includeNonSingletons, allowEagerInit);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(Class<T> requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeansOfType(requiredType);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(
          Class<T> requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeansOfType(requiredType, includeNonSingletons, allowEagerInit);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(
          ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeansOfType(requiredType, includeNonSingletons, allowEagerInit);
  }

  @Override
  public Set<String> getBeanNamesForType(ResolvableType requiredType) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanNamesForType(requiredType);
  }

  @Override
  public Set<String> getBeanNamesForType(
          ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    assertBeanFactoryActive();
    return getBeanFactory().getBeanNamesForType(requiredType, includeNonSingletons, allowEagerInit);
  }

  // ArgumentsResolverProvider

  @NonNull
  @Override
  public DependencyInjector getInjector() {
    return getBeanFactory().getInjector();
  }

  // @since 2.1.7
  // ---------------------------

  public List<BeanFactoryPostProcessor> getFactoryPostProcessors() {
    return factoryPostProcessors;
  }

  // since 4.0
  public void addFactoryPostProcessors(BeanFactoryPostProcessor... postProcessors) {
    CollectionUtils.addAll(factoryPostProcessors, postProcessors);
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
  public boolean isRunning() {
    return this.lifecycleProcessor != null && this.lifecycleProcessor.isRunning();
  }

  // lifecycleProcessor

  // @since 4.0
  public void setLifecycleProcessor(@Nullable LifecycleProcessor lifecycleProcessor) {
    this.lifecycleProcessor = lifecycleProcessor;
  }

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
        if (log.isTraceEnabled()) {
          log.trace("Using LifecycleProcessor [{}]", lifecycleProcessor);
        }
      }
      else {
        DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
        defaultProcessor.setBeanFactory(beanFactory);
        this.lifecycleProcessor = defaultProcessor;
        beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);
        if (log.isTraceEnabled()) {
          log.trace("No '{}' bean, using [{}]", LIFECYCLE_PROCESSOR_BEAN_NAME, lifecycleProcessor.getClass().getSimpleName());
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
              "LifecycleProcessor not initialized - " +
                      "call 'refresh' before invoking lifecycle methods via the context: " + this);
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
      throw new IllegalStateException("ApplicationEventMulticaster not initialized - " +
              "call 'refresh' before multicasting events via the context: " + this);
    }
    return this.applicationEventMulticaster;
  }

  /**
   * Initialize the ApplicationEventMulticaster.
   * Uses SimpleApplicationEventMulticaster if none defined in the context.
   *
   * @see cn.taketoday.context.event.SimpleApplicationEventMulticaster
   */
  protected void initApplicationEventMulticaster() {
    ConfigurableBeanFactory beanFactory = getBeanFactory();
    if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
      this.applicationEventMulticaster =
              beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
      if (log.isTraceEnabled()) {
        log.trace("Using ApplicationEventMulticaster [{}]", applicationEventMulticaster);
      }
    }
    else {
      this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
      beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
      if (log.isTraceEnabled()) {
        log.trace("No '{}' bean, using [{}]",
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

  @Override
  public void publishEvent(Object event) {
    publishEvent(event, null);
  }

  /**
   * Publish the given event to all listeners.
   *
   * @param event the event to publish (may be an {@link ApplicationEvent}
   * @param eventType the resolved event type, if known
   * @since 4.0
   */
  protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
    Assert.notNull(event, "Event must not be null");

    // Multicast right now if possible - or lazily once the multicaster is initialized
    if (this.earlyApplicationEvents != null) {
      this.earlyApplicationEvents.add(event);
    }
    else {
      getApplicationEventMulticaster().multicastEvent(event, eventType);
    }

    // Publish event via parent context as well...
    if (parent != null) {
      if (parent instanceof AbstractApplicationContext parent) {
        parent.publishEvent(event, eventType);
      }
      else {
        parent.publishEvent(event);
      }
    }

  }

  protected void registerApplicationListeners() {
    log.debug("Registering application-listeners");

    // Register statically specified listeners first.
    for (ApplicationListener<?> listener : getApplicationListeners()) {
      getApplicationEventMulticaster().addApplicationListener(listener);
    }

    // Do not initialize FactoryBeans here: We need to leave all regular beans
    // uninitialized to let post-processors apply to them!
    Set<String> listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
    for (String listenerBeanName : listenerBeanNames) {
      getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
    }

    log.debug("Publish early application events");
    // Publish early application events now that we finally have a multicaster...
    Set<Object> earlyEventsToProcess = this.earlyApplicationEvents;
    this.earlyApplicationEvents = null;
    if (CollectionUtils.isNotEmpty(earlyEventsToProcess)) {
      for (Object earlyEvent : earlyEventsToProcess) {
        getApplicationEventMulticaster().multicastEvent(earlyEvent);
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
   * @see cn.taketoday.context.event.ContextRefreshedEvent
   * @see cn.taketoday.context.event.ContextClosedEvent
   */
  @Override
  public void addApplicationListener(ApplicationListener<?> listener) {
    Assert.notNull(listener, "ApplicationListener is required");
    if (this.applicationEventMulticaster != null) {
      this.applicationEventMulticaster.addApplicationListener(listener);
    }
    this.applicationListeners.add(listener);
  }

  /**
   * Finish the initialization of this context's bean factory,
   * initializing all remaining singleton beans.
   */
  protected void finishBeanFactoryInitialization(ConfigurableBeanFactory beanFactory) {
    // Initialize conversion service for this context.
    if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
            beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
      beanFactory.setConversionService(
              beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
    }

    // Register a default embedded value resolver if no BeanFactoryPostProcessor
    // (such as a PropertySourcesPlaceholderConfigurer bean) registered any before:
    // at this point, primarily for resolution in annotation attribute values.
    if (!beanFactory.hasEmbeddedValueResolver()) {
      beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolveRequiredPlaceholders(strVal));
    }

    // Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
    Set<String> weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
    for (String weaverAwareName : weaverAwareNames) {
      getBean(weaverAwareName);
    }

    // Stop using the temporary ClassLoader for type matching.
    beanFactory.setTempClassLoader(null);

    // Allow for caching all bean definition metadata, not expecting further changes.
    beanFactory.freezeConfiguration();

    // Instantiate all remaining (non-lazy-init) singletons.
    beanFactory.preInstantiateSingletons();
  }

  /**
   * Context start success
   */
  protected void finishRefresh() {
    // Clear context-level resource caches (such as ASM metadata from scanning).
    clearResourceCaches();

    // Initialize lifecycle processor for this context.
    initLifecycleProcessor();

    // Propagate refresh to lifecycle processor first.
    getLifecycleProcessor().onRefresh();

    // Publish the final event.
    publishEvent(new ContextRefreshedEvent(this));

    applyState(State.STARTED);
    log.info("Application context startup in {}ms", System.currentTimeMillis() - getStartupDate());
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
  public abstract AbstractBeanFactory getBeanFactory();

  // Object

  /**
   * Return information about this context.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getDisplayName());
    sb.append(": state: [")
            .append(state)
            .append("], on startup date: ")
            .append(formatStartupDate());
    ApplicationContext parent = getParent();
    if (parent != null) {
      sb.append(", parent: ").append(parent.getDisplayName());
    }
    return sb.toString();
  }

  public String formatStartupDate() {
    return new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT).format(startupDate);
  }

}
