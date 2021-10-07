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
import java.util.Collection;
import java.util.HashSet;
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
import cn.taketoday.beans.factory.Scope;
import cn.taketoday.beans.factory.ValueExpressionContext;
import cn.taketoday.beans.support.BeanFactoryAwareBeanInstantiator;
import cn.taketoday.context.event.ApplicationEventPublisher;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.context.event.BeanDefinitionLoadedEvent;
import cn.taketoday.context.event.BeanDefinitionLoadingEvent;
import cn.taketoday.context.event.ContextCloseEvent;
import cn.taketoday.context.event.ContextCloseListener;
import cn.taketoday.context.event.ContextPreRefreshEvent;
import cn.taketoday.context.event.ContextRefreshEvent;
import cn.taketoday.context.event.ContextStartedEvent;
import cn.taketoday.context.event.DefaultApplicationEventPublisher;
import cn.taketoday.context.event.DependenciesHandledEvent;
import cn.taketoday.context.event.EventListener;
import cn.taketoday.context.loader.CandidateComponentScanner;
import cn.taketoday.core.Assert;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.Constant;
import cn.taketoday.core.MultiValueMap;
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
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractApplicationContext implements ConfigurableApplicationContext {
  private static final Logger log = LoggerFactory.getLogger(AbstractApplicationContext.class);

  private long startupDate;

  private ConfigurableEnvironment environment;

  // @since 2.1.5
  private State state = State.NONE;

  private String[] locations;
  /** @since 2.1.7 Scan candidates */
  private CandidateComponentScanner candidateComponentScanner;

  private ArrayList<BeanFactoryPostProcessor> factoryPostProcessors;

  /** Unique id for this context, if any. @since 4.0 */
  private String id = ObjectUtils.identityToString(this);

  /** Parent context. @since 4.0 */
  @Nullable
  private ApplicationContext parent;

  /** Display name. */
  private String displayName = ObjectUtils.identityToString(this);

  /** @since 4.0 */
  private ApplicationEventPublisher eventPublisher = new DefaultApplicationEventPublisher();

  /** @since 4.0 */
  private BeanFactoryAwareBeanInstantiator beanInstantiator;
  /** @since 4.0 */
  private final PathMatchingPatternResourceLoader patternResourceLoader = new PathMatchingPatternResourceLoader();

  public AbstractApplicationContext() {
    ContextUtils.setLastStartupContext(this); // @since 2.1.6
  }

  /**
   * Construct with a {@link ConfigurableEnvironment}
   *
   * @param env
   *         {@link ConfigurableEnvironment} instance
   *
   * @since 2.1.7
   */
  public AbstractApplicationContext(ConfigurableEnvironment env) {
    this();
    this.environment = env;
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
   * Load all the class in class path
   */
  public void scan() {
    scan(Constant.BLANK);
  }

  @Override
  public void scan(Collection<Class<?>> candidates) {
    Set<Class<?>> candidateSet;
    if (candidates instanceof Set) {
      candidateSet = (Set<Class<?>>) candidates;
    }
    else {
      candidateSet = new HashSet<>(candidates);
    }

    getCandidateComponentScanner().setCandidates(candidateSet);

    scan((String[]) null);
  }

  @Override
  public void scan(String... locations) {
    this.locations = locations;

    try {
      // Prepare refresh
      prepareRefresh();
      // Prepare BeanFactory
      prepareBeanFactory();
      // Initialization singletons that has already in context
      // Initialize other special beans in specific context subclasses.
      // for example a Web Server
      preRefresh();

      // Refresh factory, Initialize all singletons.
      refresh();

      // Finish refresh
      finishRefresh();
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

    postProcessLoadProperties();

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
   * post-process after load properties
   */
  protected void postProcessLoadProperties() {
    ConfigurableEnvironment environment = getEnvironment();
    // @since 2.1.6
    if (environment.getFlag(ENABLE_FULL_PROTOTYPE)) {
      getBeanFactory().setFullPrototype(true);
    }
    if (environment.getFlag(ENABLE_FULL_LIFECYCLE)) {
      getBeanFactory().setFullLifecycle(true);
    }
  }

  /**
   * Load bean definitions
   *
   * @param beanFactory
   *         Bean factory
   * @param candidates
   *         candidates bean classes
   */
  protected void loadBeanDefinitions(AbstractBeanFactory beanFactory, Collection<Class<?>> candidates) {
  }

  /**
   * Context start success
   */
  protected void finishRefresh() {
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
    AbstractBeanFactory beanFactory = getBeanFactory();
    // must not be null
    ConfigurableEnvironment env = getEnvironment();

    checkEnvironment(env);
    // register framework beans
    log.info("Registering framework beans");
    registerFrameworkComponents(env, beanFactory);
    // Loading candidates components
    log.info("Loading candidates components");
    Set<Class<?>> candidates = getComponentCandidates();
    log.info("There are [{}] candidates components in [{}]", candidates.size(), this);

    // start loading bean definitions ; publish loading bean definition event
    publishEvent(new BeanDefinitionLoadingEvent(this, candidates)); // first event
    loadBeanDefinitions(beanFactory, candidates);

    // register listener @since 4.0 after loadBeanDefinitions
    registerListener(candidates, applicationListeners);

    // bean definitions loaded
    publishEvent(new BeanDefinitionLoadedEvent(this, beanFactory.getBeanDefinitions()));
    // handle dependency : register bean dependencies definition
    beanFactory.handleDependency();
    publishEvent(new DependenciesHandledEvent(this, beanFactory.getDependencies()));

    postProcessBeanFactory(beanFactory);
  }

  /**
   * check {@link ConfigurableEnvironment}
   */
  protected void checkEnvironment(ConfigurableEnvironment env) {
    // Expression
  }

  protected Set<Class<?>> getComponentCandidates() {
    CandidateComponentScanner scanner = getCandidateComponentScanner();
    if (ObjectUtils.isEmpty(locations)) {
      // Candidates have not been set or scanned
      if (scanner.getCandidates() == null) {
        return scanner.scan();// scan all class path
      }
      return scanner.getScanningCandidates();
    }
    return scanner.scan(locations);
  }

  /**
   * register Framework Beans
   */
  public void registerFrameworkComponents() {
    AbstractBeanFactory beanFactory = getBeanFactory();
    registerFrameworkComponents(getEnvironment(), beanFactory);
  }

  /**
   * Register Framework Beans
   */
  protected void registerFrameworkComponents(
          ConfigurableEnvironment env, AbstractBeanFactory beanFactory) {

    ExpressionProcessor elProcessor = beanFactory.getBean(ExpressionProcessor.class);
    if (elProcessor == null) {
      // create shared elProcessor to singletons
      ExpressionFactory exprFactory = ExpressionFactory.getSharedInstance();
      ValueExpressionContext elContext = new ValueExpressionContext(exprFactory, getBeanFactory());
      elContext.defineBean(ExpressionEvaluator.ENV, env); // @since 2.1.6

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
    beanFactory.registerSingleton(createBeanName(Environment.class), env);
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
   *
   * @param beanFactory
   *         bean factory
   */
  protected void postProcessBeanFactory(AbstractBeanFactory beanFactory) {
    registerBeanFactoryPostProcessor();

    if (CollectionUtils.isNotEmpty(factoryPostProcessors)) {
      for (BeanFactoryPostProcessor postProcessor : factoryPostProcessors) {
        postProcessor.postProcessBeanFactory(beanFactory);
      }
    }

    // register bean post processors
    beanFactory.registerBeanPostProcessors();

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
    publishEvent(new ContextRefreshEvent(this));

    try {
      // Prepare refresh
      prepareRefresh();
      // Prepare BeanFactory
      prepareBeanFactory();
      // Initialization singletons that has already in context
      // Initialize other special beans in specific context subclasses.
      // for example a Web Server
      preRefresh();

      // Refresh factory, Initialize all singletons.
      onRefresh();

      // Finish refresh
      finishRefresh();
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

  // ------------------- BeanFactory

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

  // ArgumentsResolverProvider

  @NonNull
  @Override
  public ArgumentsResolver getArgumentsResolver() {
    return getBeanFactory().getArgumentsResolver();
  }

  // ----------------------------

  public void setPropertiesLocation(String propertiesLocation) {
    if (StringUtils.isNotEmpty(propertiesLocation)) {
//      getEnvironment().setPropertiesLocation(propertiesLocation);
    }
  }

  // @since 2.1.7
  // ---------------------------

  @Override
  public CandidateComponentScanner getCandidateComponentScanner() {
    CandidateComponentScanner ret = this.candidateComponentScanner;
    if (ret == null) {
      return this.candidateComponentScanner = createCandidateComponentScanner();
    }
    return ret;
  }

  protected CandidateComponentScanner createCandidateComponentScanner() {
    return CandidateComponentScanner.getSharedInstance();
  }

  @Override
  public void setCandidateComponentScanner(CandidateComponentScanner candidateComponentScanner) {
    this.candidateComponentScanner = candidateComponentScanner;
  }

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

  /**
   * Load all the application listeners in context and register it.
   */
  protected void registerListener(Collection<Class<?>> candidates) {
    log.info("Loading Application Listeners.");

    for (Class<?> candidateListener : candidates) {
      if (AnnotationUtils.isPresent(candidateListener, EventListener.class)) {
        registerListener(candidateListener);
      }
    }

    postProcessRegisterListener();
  }

  /**
   * Register {@link ApplicationListener} to {@link ApplicationEventPublisher}
   * <p>
   * If there isn't a bean create it and register bean to singleton cache
   *
   * @param listenerClass
   *         Must be {@link ApplicationListener} class
   *
   * @throws IllegalArgumentException
   *         If listenerClass isn't a {@link ApplicationListener}
   * @see #getEnvironment()
   */
  protected void registerListener(Class<?> listenerClass) {
    Assert.isAssignable(
            ApplicationListener.class,
            listenerClass, "@EventListener must be a 'ApplicationListener'");
    try {
      // if exist bean
      Object applicationListener = getBeanFactory().getSingleton(listenerClass);
      if (applicationListener == null) {
        // create bean instance
        applicationListener = beanInstantiator.instantiate(listenerClass);
        getBeanFactory().registerSingleton(applicationListener);
      }
      addApplicationListener((ApplicationListener<?>) applicationListener);
    }
    catch (NoSuchBeanDefinitionException e) {
      throw new ConfigurationException("It is best not to use constructor-injection when instantiating the listener", e);
    }
  }

  @Override
  public void addApplicationListener(ApplicationListener<?> listener) {
    getEventPublisher().addApplicationListener(listener);
  }

  @Override
  public void addApplicationListener(Class<?> listener) {
    registerListener(listener);
  }

  @Override
  public void removeAllListeners() {
    getEventPublisher().removeAllListeners();
  }

  /**
   * Process after {@link #registerListener(Collection, MultiValueMap)}
   */
  protected void postProcessRegisterListener() {
    addApplicationListener(new ContextCloseListener());

    Set<Class<?>> listeners = loadMetaInfoListeners();
    // load from strategy files
    log.info("Loading listeners from strategies files");
    TodayStrategies todayStrategies = TodayStrategies.getDetector();
    listeners.addAll(todayStrategies.getTypes(ApplicationListener.class));

    for (Class<?> listener : listeners) {
      registerListener(listener);
    }
  }

  /**
   * Load the META-INF/listeners
   *
   * @see Constant#META_INFO_listeners
   * @since 2.1.6
   */
  public Set<Class<?>> loadMetaInfoListeners() {
    // fixed #9 Some listener in a jar can't be load
    log.info("Loading META-INF/listeners");
    // Load the META-INF/listeners
    // ---------------------------------------------------
    return ContextUtils.loadFromMetaInfo(Constant.META_INFO_listeners);
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
