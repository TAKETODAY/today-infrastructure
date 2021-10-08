/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.context;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.ArgumentsResolver;
import cn.taketoday.beans.factory.AbstractBeanFactory;
import cn.taketoday.beans.factory.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.BeanPostProcessor;
import cn.taketoday.beans.factory.BeanReferencePropertySetter;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.ObjectSupplier;
import cn.taketoday.beans.factory.Prototypes;
import cn.taketoday.beans.factory.Scope;
import cn.taketoday.beans.factory.ValueExpressionContext;
import cn.taketoday.beans.support.BeanFactoryAwareBeanInstantiator;
import cn.taketoday.context.aware.ApplicationContextAwareProcessor;
import cn.taketoday.context.event.ApplicationEventPublisher;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.context.event.ContextCloseEvent;
import cn.taketoday.context.event.ContextCloseListener;
import cn.taketoday.context.event.ContextPreRefreshEvent;
import cn.taketoday.context.event.ContextStartedEvent;
import cn.taketoday.context.event.DefaultApplicationEventPublisher;
import cn.taketoday.context.event.EventListener;
import cn.taketoday.core.Assert;
import cn.taketoday.core.Constant;
import cn.taketoday.core.NonNull;
import cn.taketoday.core.Nullable;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TodayStrategies;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.expression.ExpressionFactory;
import cn.taketoday.expression.ExpressionManager;
import cn.taketoday.expression.ExpressionProcessor;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

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
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractApplicationContext implements ConfigurableApplicationContext {
  private static final Logger log = LoggerFactory.getLogger(AbstractApplicationContext.class);

  private long startupDate;

  private ConfigurableEnvironment environment;

  // @since 2.1.5
  private State state = State.NONE;

  private ArrayList<BeanFactoryPostProcessor> factoryPostProcessors;

  /** Unique id for this context, if any. @since 4.0 */
  private String id = ObjectUtils.identityToString(this);

  /** Parent context. @since 4.0 */
  @Nullable
  private ApplicationContext parent;

  /** Display name. */
  private String displayName = ObjectUtils.identityToString(this);

  /** @since 4.0 */
  private ApplicationEventPublisher eventPublisher = new DefaultApplicationEventPublisher(getBeanFactory());

  /** @since 4.0 */
  private BeanFactoryAwareBeanInstantiator beanInstantiator;
  /** @since 4.0 */
  private final PathMatchingPatternResourceLoader patternResourceLoader = new PathMatchingPatternResourceLoader();

  public AbstractApplicationContext() {
    ContextUtils.setLastStartupContext(this); // @since 2.1.6
  }

  /**
   * Create a new AbstractApplicationContext with the given parent context.
   *
   * @param parent
   *         the parent context
   */
  public AbstractApplicationContext(@Nullable ApplicationContext parent) {
    this();
    setParent(parent);
  }

  //---------------------------------------------------------------------
  // Implementation of PatternResourceLoader interface
  //---------------------------------------------------------------------

  @NonNull
  @Override
  public Resource getResource(String location) {
    return patternResourceLoader.getResource(location);
  }

  @Override
  public Resource[] getResources(String locationPattern) throws IOException {
    return patternResourceLoader.getResources(locationPattern);
  }

  @Nullable
  @Override
  public ClassLoader getClassLoader() {
    return patternResourceLoader.getClassLoader();
  }

  //---------------------------------------------------------------------
  // Implementation of ApplicationContext interface
  //---------------------------------------------------------------------

  /**
   * Set the unique id of this application context.
   * <p>Default is the object id of the context instance, or the name
   * of the context bean if the context is itself defined as a bean.
   *
   * @param id
   *         the unique id of the context
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
  public void setDisplayName(String displayName) {
    Assert.hasLength(displayName, "Display name must not be empty");
    this.displayName = displayName;
  }

  /**
   * Return a friendly name for this context.
   *
   * @return a display name for this context (never {@code null})
   */
  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String getApplicationName() {
    return null; // TODO application.name property
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
  public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
    return getBeanFactory();
  }

  @Override
  public boolean containsBean(String name) {
    return getBeanFactory().containsBean(name);
  }

  @Override
  public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
    return getBeanFactory().isTypeMatch(name, typeToMatch);
  }

  @Override
  public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
    return getBeanFactory().isTypeMatch(name, typeToMatch);
  }

  @Override
  public <T> ObjectSupplier<T> getObjectSupplier(ResolvableType requiredType) {
    return getBeanFactory().getObjectSupplier(requiredType);
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
   * Prepare to load context
   */
  protected void prepareRefresh() {
    this.startupDate = System.currentTimeMillis();
    log.info("Starting Application Context at [{}].", formatStartupDate());

    applyState(State.STARTING);

    // Initialize any placeholder property sources in the context environment.
    initPropertySources();

    ConfigurableEnvironment environment = getEnvironment();
    AbstractBeanFactory beanFactory = getBeanFactory();

    // @since 2.1.6
    if (environment.getFlag(ENABLE_FULL_PROTOTYPE)) {
      beanFactory.setFullPrototype(true);
    }
    if (environment.getFlag(ENABLE_FULL_LIFECYCLE)) {
      beanFactory.setFullLifecycle(true);
    }

    // register framework beans
    registerFrameworkComponents(beanFactory);
  }

  /**
   * <p>Replace any stub property sources with actual instances.
   *
   * @see PropertySource.StubPropertySource
   */
  protected void initPropertySources() {
    // For subclasses: do nothing by default.

    // prepare properties
    ConfigurableEnvironment env = getEnvironment();
    try {
      env.loadProperties();
    }
    catch (IOException ex) {
      throw new ApplicationContextException("An Exception Occurred When Loading Properties", ex);
    }
  }

  /**
   * Context start success
   */
  protected void postRefresh() {
    getBeanFactory().initializeSingletons();

    // start success publish started event
    publishEvent(new ContextStartedEvent(this));
    applyState(State.STARTED);

    log.info("Application Context Startup in {}ms", System.currentTimeMillis() - getStartupDate());
  }

  /**
   * Initialization singletons that has already in context
   */
  protected void preRefresh() {
    publishEvent(new ContextPreRefreshEvent(this));
    // fix: #1 some singletons could not be initialized.
    getBeanFactory().preInitialization();
  }

  public void prepareBeanFactory() {
    log.info("Preparing internal bean-factory");
  }

  /**
   * register Framework Beans
   */
  public void registerFrameworkComponents() {
    registerFrameworkComponents(getBeanFactory());
  }

  /**
   * Register Framework Beans
   */
  protected void registerFrameworkComponents(AbstractBeanFactory beanFactory) {
    log.info("Registering framework beans");
    ExpressionProcessor elProcessor = beanFactory.getBean(ExpressionProcessor.class);
    if (elProcessor == null) {
      // create shared elProcessor to singletons
      ExpressionFactory exprFactory = ExpressionFactory.getSharedInstance();
      ValueExpressionContext elContext = new ValueExpressionContext(exprFactory, getBeanFactory());
      elContext.defineBean(ExpressionEvaluator.ENV, getEnvironment()); // @since 2.1.6

      ExpressionManager elManager = new ExpressionManager(elContext, exprFactory);
      elProcessor = new ExpressionProcessor(elManager);

      // register ELManager @since 2.1.5
      // fix @since 2.1.6 elManager my be null
      beanFactory.registerSingleton(elManager);

      beanFactory.registerSingleton(elContext);
      beanFactory.registerSingleton(exprFactory);
      beanFactory.registerSingleton(elProcessor);
    }

    // register Environment
    beanFactory.registerSingleton(createBeanName(Environment.class), getEnvironment());
    // register ApplicationContext
    beanFactory.registerSingleton(createBeanName(ApplicationContext.class), this);
    // register BeanFactory @since 2.1.7
    beanFactory.registerSingleton(createBeanName(BeanFactory.class), beanFactory);
    // @since 4.0 ArgumentsResolver
    beanFactory.registerSingleton(getArgumentsResolver());

  }

  public String createBeanName(Class<?> clazz) {
    return ClassUtils.getShortName(clazz);
  }

  /**
   * Process after {@link #prepareBeanFactory}
   */
  protected void postProcessBeanFactory() {
    registerBeanFactoryPostProcessor();
    AbstractBeanFactory beanFactory = getBeanFactory();

    if (CollectionUtils.isNotEmpty(factoryPostProcessors)) {
      for (BeanFactoryPostProcessor postProcessor : factoryPostProcessors) {
        postProcessor.postProcessBeanFactory(beanFactory);
      }
    }

    // register bean post processors
    beanFactory.registerBeanPostProcessors();
    beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));

    if (beanFactory.isFullPrototype()) {
      for (BeanReferencePropertySetter reference : beanFactory.getDependencies()) {
        BeanDefinition def = beanFactory.getBeanDefinition(reference.getReferenceName());
        if (def != null && def.isPrototype()) {
          reference.applyPrototype();
        }
      }
    }

  }

  /**
   * Register {@link BeanFactoryPostProcessor}s
   */
  public void registerBeanFactoryPostProcessor() {
    log.info("Loading BeanFactoryPostProcessor.");

    List<BeanFactoryPostProcessor> postProcessors = getBeans(BeanFactoryPostProcessor.class);
    if (!postProcessors.isEmpty()) {
      getFactoryPostProcessors().addAll(postProcessors);
      AnnotationAwareOrderComparator.sort(factoryPostProcessors);
    }
  }

  //---------------------------------------------------------------------
  // Implementation of ApplicationContext interface
  //---------------------------------------------------------------------

  @Override
  public void refresh() {
    try {
      // Prepare refresh
      prepareRefresh();

      // Prepare BeanFactory
      prepareBeanFactory();

      // handle dependency : register bean dependencies definition
      handleDependency();

      registerApplicationListeners();

      postProcessBeanFactory();

      // Initialization singletons that has already in context
      // Initialize other special beans in specific context subclasses.
      // for example a Web Server
      preRefresh();

      // Refresh factory, Initialize all singletons.
      onRefresh();

      // Finish refresh
      postRefresh();
    }
    catch (Throwable ex) {
      close();
      applyState(State.FAILED);
      ex = ExceptionUtils.unwrapThrowable(ex);
      throw new ApplicationContextException("An Exception Occurred When Loading Context", ex);
    }
    finally {
      resetCommonCaches();
    }

  }

  protected void handleDependency() {
    // handle dependency : register bean dependencies definition
    getBeanFactory().handleDependency();
  }

  protected void onRefresh() {

  }

  @Override
  public abstract AbstractBeanFactory getBeanFactory();

  @Override
  public void close() {
    applyState(State.CLOSING);
    publishEvent(new ContextCloseEvent(this));
    applyState(State.CLOSED);
  }

  @NonNull
  @Override
  @SuppressWarnings("unchecked")
  public <T> T unwrapFactory(Class<T> requiredType) {
    AbstractBeanFactory beanFactory = getBeanFactory();
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
    throw new IllegalArgumentException("bean factory must be a " + requiredType);
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

    getFactoryPostProcessors().add(postProcessor);
  }

  //---------------------------------------------------------------------
  // Implementation of BeanFactory interface
  //---------------------------------------------------------------------

  @Override
  public Object getBean(String name) {
    return getBeanFactory().getBean(name);
  }

  @Override
  public Object getBean(BeanDefinition def) {
    return getBeanFactory().getBean(def);
  }

  @Override
  public <T> T getBean(Class<T> requiredType) {
    return getBeanFactory().getBean(requiredType);
  }

  @Override
  public <T> T getBean(String name, Class<T> requiredType) {
    return getBeanFactory().getBean(name, requiredType);
  }

  @Override
  public <T> ObjectSupplier<T> getObjectSupplier(BeanDefinition def) {
    return getBeanFactory().getObjectSupplier(def);
  }

  @Override
  public <T> ObjectSupplier<T> getObjectSupplier(Class<T> requiredType) {
    return getBeanFactory().getObjectSupplier(requiredType);
  }

  @Override
  public Object getScopeBean(BeanDefinition def, Scope scope) {
    return getBeanFactory().getScopeBean(def, scope);
  }

  @Override
  public <T> List<T> getBeans(Class<T> requiredType) {
    return getBeanFactory().getBeans(requiredType);
  }

  @Override
  public <A extends Annotation> A getAnnotationOnBean(String beanName, Class<A> annotationType) {
    return getBeanFactory().getAnnotationOnBean(beanName, annotationType);
  }

  @Override
  public <T> List<T> getAnnotatedBeans(Class<? extends Annotation> annotationType) {
    return getBeanFactory().getAnnotatedBeans(annotationType);
  }

  @Override
  public Set<String> getBeanNamesOfType(Class<?> requiredType, boolean includeNonSingletons) {
    return getBeanFactory().getBeanNamesOfType(requiredType, includeNonSingletons);
  }

  @Override
  public Set<String> getBeanNamesOfType(
          Class<?> requiredType, boolean includeNoneRegistered, boolean includeNonSingletons) {
    return getBeanFactory().getBeanNamesOfType(requiredType, includeNoneRegistered, includeNonSingletons);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(Class<T> requiredType) {
    return getBeanFactory().getBeansOfType(requiredType);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(Class<T> requiredType, boolean includeNonSingletons) {
    return getBeanFactory().getBeansOfType(requiredType, includeNonSingletons);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(
          Class<T> requiredType, boolean includeNoneRegistered, boolean includeNonSingletons) {
    return getBeanFactory().getBeansOfType(requiredType, includeNoneRegistered, includeNonSingletons);
  }

  @Override
  public Map<String, Object> getBeansOfAnnotation(Class<? extends Annotation> annotationType) {
    return getBeanFactory().getBeansOfAnnotation(annotationType);
  }

  @Override
  public Map<String, Object> getBeansOfAnnotation(Class<? extends Annotation> annotationType, boolean includeNonSingletons) {
    return getBeanFactory().getBeansOfAnnotation(annotationType, includeNonSingletons);
  }

  @Override
  public Map<String, BeanDefinition> getBeanDefinitions() {
    return getBeanFactory().getBeanDefinitions();
  }

  @Override
  public boolean isSingleton(String name) {
    return getBeanFactory().isSingleton(name);
  }

  @Override
  public boolean isPrototype(String name) {
    return getBeanFactory().isPrototype(name);
  }

  @Override
  public Class<?> getType(String name) {
    return getBeanFactory().getType(name);
  }

  @Override
  public Set<String> getAliases(Class<?> type) {
    return getBeanFactory().getAliases(type);
  }

  @Override
  public String getBeanName(Class<?> targetClass) {
    return getBeanFactory().getBeanName(targetClass);
  }

  @Override
  public boolean isFullLifecycle() {
    return getBeanFactory().isFullLifecycle();
  }

  @Override
  public boolean isFullPrototype() {
    return getBeanFactory().isFullPrototype();
  }

  @Override
  public <T> Map<String, T> getBeansOfType(
          ResolvableType requiredType, boolean includeNoneRegistered, boolean includeNonSingletons) {
    return getBeanFactory().getBeansOfType(requiredType, includeNoneRegistered, includeNonSingletons);
  }

  @Override
  public Set<String> getBeanNamesOfType(
          ResolvableType requiredType, boolean includeNoneRegistered, boolean includeNonSingletons) {
    return getBeanFactory().getBeanNamesOfType(requiredType, includeNoneRegistered, includeNonSingletons);
  }

  @Override
  public Set<String> getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
    return getBeanFactory().getBeanNamesForAnnotation(annotationType);
  }

  // ArgumentsResolverProvider

  @NonNull
  @Override
  public ArgumentsResolver getArgumentsResolver() {
    return getBeanFactory().getArgumentsResolver();
  }

  // @since 2.1.7
  // ---------------------------

  public List<BeanFactoryPostProcessor> getFactoryPostProcessors() {
    ArrayList<BeanFactoryPostProcessor> processors = this.factoryPostProcessors;
    if (processors == null) {
      return this.factoryPostProcessors = new ArrayList<>();
    }
    return processors;
  }

  //---------------------------------------------------------------------
  // Implementation of ApplicationEventPublisher interface
  //---------------------------------------------------------------------

  @Override
  public void publishEvent(Object event) {
    getEventPublisher().publishEvent(event);
  }

  protected void registerApplicationListeners() {
    log.info("Loading Application Listeners.");
    addApplicationListener(new ContextCloseListener());

    for (BeanDefinition definition : getBeanFactory()) {
      if (AnnotationUtils.isPresent(definition, EventListener.class)) {
        Object listener;
        if (definition.isSingleton()) {
          listener = getBean(definition);
        }
        else {
          listener = Prototypes.newProxyInstance(definition, getBeanFactory());
        }

        Assert.isInstanceOf(
                ApplicationListener.class, listener, "@EventListener bean must be a 'ApplicationListener'");
        addApplicationListener((ApplicationListener<?>) listener);
      }
    }
    // fixed #9 Some listener in a jar can't be load
    log.info("Loading META-INF/listeners");

    // Load the META-INF/listeners
    // ---------------------------------------------------
    Set<Class<?>> listeners = ContextUtils.loadFromMetaInfo(Constant.META_INFO_listeners);
    for (Class<?> listener : listeners) {
      ApplicationListener applicationListener = (ApplicationListener) beanInstantiator.instantiate(listener);
      addApplicationListener(applicationListener);
    }

    // load from strategy files
    TodayStrategies detector = TodayStrategies.getDetector();
    log.info("Loading listeners from strategies files: {}", detector.getStrategiesLocation());
    for (ApplicationListener listener : detector.getStrategies(ApplicationListener.class, this)) {
      addApplicationListener(listener);
    }

  }

  @Override
  public void addApplicationListener(ApplicationListener<?> listener) {
    getEventPublisher().addApplicationListener(listener);
  }

  @Override
  public void addApplicationListener(String listenerBeanName) {
    getEventPublisher().addApplicationListener(listenerBeanName);
  }

  @Override
  public void removeApplicationListener(String listenerBeanName) {
    getEventPublisher().removeApplicationListener(listenerBeanName);

  }

  @Override
  public void removeApplicationListener(ApplicationListener<?> listener) {
    getEventPublisher().removeApplicationListener(listener);
  }

  @Override
  public void removeAllListeners() {
    getEventPublisher().removeAllListeners();
  }

  /** @since 4.0 */
  public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
    Assert.notNull(eventPublisher, "event-publisher must not be nul");
    this.eventPublisher = eventPublisher;
  }

  /** @since 4.0 */
  public ApplicationEventPublisher getEventPublisher() {
    return eventPublisher;
  }

  // Object

  @Override
  public String toString() {
    return new StringBuilder(ObjectUtils.toHexString(this))
            .append(": state: [")
            .append(state)
            .append("], on startup date: ")
            .append(formatStartupDate())
            .toString();
  }

  public String formatStartupDate() {
    return new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT).format(startupDate);
  }

}
