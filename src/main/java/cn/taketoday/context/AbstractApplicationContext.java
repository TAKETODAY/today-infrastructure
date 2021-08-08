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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import cn.taketoday.context.event.ApplicationEventCapable;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.context.event.BeanDefinitionLoadedEvent;
import cn.taketoday.context.event.BeanDefinitionLoadingEvent;
import cn.taketoday.context.event.ContextCloseEvent;
import cn.taketoday.context.event.ContextCloseListener;
import cn.taketoday.context.event.ContextPreRefreshEvent;
import cn.taketoday.context.event.ContextRefreshEvent;
import cn.taketoday.context.event.ContextStartedEvent;
import cn.taketoday.context.event.DependenciesHandledEvent;
import cn.taketoday.context.event.EventListener;
import cn.taketoday.context.event.ObjectRefreshedEvent;
import cn.taketoday.context.factory.AbstractBeanFactory;
import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.BeanDefinitionStoreException;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.factory.BeanFactoryPostProcessor;
import cn.taketoday.context.factory.BeanInitializingException;
import cn.taketoday.context.factory.BeanPostProcessor;
import cn.taketoday.context.factory.BeanReferencePropertySetter;
import cn.taketoday.context.factory.NoSuchBeanDefinitionException;
import cn.taketoday.context.factory.ObjectSupplier;
import cn.taketoday.context.factory.ValueExpressionContext;
import cn.taketoday.context.loader.CandidateComponentScanner;
import cn.taketoday.context.loader.StrategiesDetector;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.AnnotationUtils;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.GenericTypeResolver;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.expression.ExpressionFactory;
import cn.taketoday.expression.ExpressionManager;
import cn.taketoday.expression.ExpressionProcessor;

/**
 * @author TODAY <br>
 * 2018-09-09 22:02
 */
public abstract class AbstractApplicationContext implements ConfigurableApplicationContext {
  private static final Logger log = LoggerFactory.getLogger(AbstractApplicationContext.class);

  private long startupDate;
  private final ConfigurableEnvironment environment;

  // @since 2.1.5
  private State state;
  /** application listeners **/
  private final HashMap<Class<?>, List<ApplicationListener<Object>>> applicationListeners = new HashMap<>(32);
  private String[] locations;
  /** @since 2.1.7 Scan candidates */
  private CandidateComponentScanner candidateComponentScanner;

  private ArrayList<BeanFactoryPostProcessor> factoryPostProcessors;
  /**
   * @since 3.1
   */
  private StrategiesDetector strategiesDetector;

  /**
   * Construct with a {@link ConfigurableEnvironment}
   *
   * @param env
   *         {@link ConfigurableEnvironment} instance
   *
   * @since 2.1.7
   */
  public AbstractApplicationContext(ConfigurableEnvironment env) {
    applyState(State.NONE);
    this.environment = env;
    ContextUtils.setLastStartupContext(this); // @since 2.1.6
    checkEnvironment(env);
  }

  /**
   * Load all the class in class path
   */
  public void load() {
    load(Constant.BLANK);
  }

  @Override
  public void load(Collection<Class<?>> candidates) {
    final Set<Class<?>> candidateSet;
    if (candidates instanceof Set) {
      candidateSet = (Set<Class<?>>) candidates;
    }
    else {
      candidateSet = new HashSet<>(candidates);
    }

    getCandidateComponentScanner().setCandidates(candidateSet);

    load((String[]) null);
  }

  @Override
  public void load(String... locations) {
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
  }

  /**
   * Prepare to load context
   */
  protected void prepareRefresh() {

    this.startupDate = System.currentTimeMillis();
    log.info("Starting Application Context at [{}].", formatStartupDate());

    applyState(State.STARTING);

    // prepare properties
    final ConfigurableEnvironment env = getEnvironment();
    try {
      env.loadProperties();
    }
    catch (IOException ex) {
      throw new ApplicationContextException("An Exception Occurred When Loading Properties", ex);
    }
    postProcessLoadProperties(env);

    {// @since 2.1.6
      if (env.getFlag(Constant.ENABLE_FULL_PROTOTYPE)) {
        enableFullPrototype();
      }
      if (env.getFlag(Constant.ENABLE_FULL_LIFECYCLE)) {
        enableFullLifecycle();
      }
    }
  }

  /**
   * Post process after load properties
   *
   * @param environment
   *         {@link ConfigurableEnvironment}
   */
  protected void postProcessLoadProperties(ConfigurableEnvironment environment) {
    // @since 3.0 enable check params types
    ClassUtils.setEnableParamNameTypeChecking(
            environment.getFlag("ClassUtils.enableParamNameTypeChecking", false)
    );
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
    // load from given class set
    beanFactory.getBeanDefinitionLoader().load(candidates);
  }

  /**
   * Context start success
   */
  protected void finishRefresh() {
    // clear cache
    // ClassUtils.clearCache();
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

    final AbstractBeanFactory beanFactory = getBeanFactory();
    // must not be null
    final ConfigurableEnvironment env = getEnvironment();

    checkEnvironment(env);
    // register framework beans
    log.info("Registering framework beans");
    registerFrameworkBeans(env, beanFactory.getBeanNameCreator());
    // Loading candidates components
    log.info("Loading candidates components");
    final Set<Class<?>> candidates = getComponentCandidates();
    log.info("There are [{}] candidates components in [{}]", candidates.size(), this);
    // register listener
    registerListener(candidates, applicationListeners);

    // start loading bean definitions ; publish loading bean definition event
    publishEvent(new BeanDefinitionLoadingEvent(this, candidates)); // first event
    loadBeanDefinitions(beanFactory, candidates);
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
    // check registry
    if (env.getBeanDefinitionRegistry() == null) {
      env.setBeanDefinitionRegistry(getBeanFactory());
    }
    // check bean definition loader
    if (env.getBeanDefinitionLoader() == null) {
      env.setBeanDefinitionLoader(getBeanFactory().getBeanDefinitionLoader());
    }
    // Expression
    if (env.getExpressionProcessor() == null) {
      final ExpressionFactory exprFactory = ExpressionFactory.getSharedInstance();
      final ValueExpressionContext elContext = new ValueExpressionContext(exprFactory, getBeanFactory());
      elContext.defineBean(Constant.ENV, env.getProperties()); // @since 2.1.6
      env.setExpressionProcessor(new ExpressionProcessor(new ExpressionManager(elContext, exprFactory)));
    }
  }

  protected Set<Class<?>> getComponentCandidates() {
    final CandidateComponentScanner scanner = getCandidateComponentScanner();
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
  public void registerFrameworkBeans() {
    registerFrameworkBeans(getEnvironment(), getBeanFactory().getBeanNameCreator());
  }

  /**
   * Register Framework Beans
   */
  protected void registerFrameworkBeans(ConfigurableEnvironment env, final BeanNameCreator nameCreator) {
    final ExpressionProcessor elProcessor = env.getExpressionProcessor();

    // register ELManager @since 2.1.5
    // fix @since 2.1.6 elManager my be null
    registerSingleton(nameCreator.create(ExpressionManager.class), elProcessor.getManager());

    registerSingleton(nameCreator.create(ExpressionProcessor.class), elProcessor);
    // register Environment
    registerSingleton(nameCreator.create(Environment.class), env);
    // register ApplicationContext
    registerSingleton(nameCreator.create(ApplicationContext.class), this);
    // register BeanFactory @since 2.1.7
    registerSingleton(nameCreator.create(BeanFactory.class), getBeanFactory());
    // @since 4.0 StrategiesLoader
    registerSingleton(nameCreator.create(StrategiesDetector.class), getStrategiesDetector());
  }

  /**
   * Process after {@link #prepareBeanFactory}
   *
   * @param beanFactory
   *         bean factory
   */
  protected void postProcessBeanFactory(AbstractBeanFactory beanFactory) {
    registerBeanFactoryPostProcessor();

    if (!CollectionUtils.isEmpty(factoryPostProcessors)) {
      for (final BeanFactoryPostProcessor postProcessor : factoryPostProcessors) {
        postProcessor.postProcessBeanFactory(beanFactory);
      }
    }

    // register bean post processors
    beanFactory.registerBeanPostProcessors();

    if (beanFactory.isFullPrototype()) {
      for (BeanReferencePropertySetter reference : beanFactory.getDependencies()) {
        final BeanDefinition def = beanFactory.getBeanDefinition(reference.getReferenceName());
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

    final List<BeanFactoryPostProcessor> postProcessors = getBeans(BeanFactoryPostProcessor.class);
    if (!postProcessors.isEmpty()) {
      getFactoryPostProcessors().addAll(postProcessors);
      OrderUtils.reversedSort(factoryPostProcessors);
    }
  }

  /**
   * Load all the application listeners in context and register it.
   *
   * @param applicationListeners
   *         {@link ApplicationListener} cache
   */
  void registerListener(final Collection<Class<?>> candidates,
                        final Map<Class<?>, List<ApplicationListener<Object>>> applicationListeners) //
  {
    log.info("Loading Application Listeners.");

    for (final Class<?> candidateListener : candidates) {
      if (AnnotationUtils.isPresent(candidateListener, EventListener.class)) {
        registerListener(candidateListener);
      }
    }

    postProcessRegisterListener(applicationListeners);
  }

  /**
   * Register {@link ApplicationListener} to {@link #applicationListeners}
   * <p>
   * If there isn't a bean create it and register bean to singleton cache
   *
   * @param listenerClass
   *         Must be {@link ApplicationListener} class
   *
   * @throws ConfigurationException
   *         If listenerClass isn't a {@link ApplicationListener}
   * @see #getEnvironment()
   */
  protected void registerListener(final Class<?> listenerClass) {
    if (!ApplicationListener.class.isAssignableFrom(listenerClass)) {
      throw new ConfigurationException("@EventListener must be a 'ApplicationListener'");
    }

    try {
      // if exist bean
      Object applicationListener = getSingleton(listenerClass);
      if (applicationListener == null) {
        // create bean instance
        applicationListener = ClassUtils.newInstance(listenerClass, this);
        registerSingleton(applicationListener);
      }
      addApplicationListener((ApplicationListener<?>) applicationListener);
    }
    catch (NoSuchBeanDefinitionException e) {
      throw new ConfigurationException("It is best not to use constructor-injection when instantiating the listener", e);
    }
    catch (Throwable ex) {
      ex = ExceptionUtils.unwrapThrowable(ex);
      throw new ApplicationContextException("An Exception Occurred When Register Application Listener", ex);
    }
  }

  @Override
  public void addApplicationListener(final ApplicationListener<?> listener) {
    Assert.notNull(listener, "listener can't be null");

    final HashMap<Class<?>, List<ApplicationListener<Object>>> listeners = this.applicationListeners;
    if (listener instanceof ApplicationEventCapable) { // @since 2.1.7
      for (final Class<?> type : ((ApplicationEventCapable) listener).getApplicationEvent()) {
        addApplicationListener(listener, type, listeners);
      }
    }
    else {
      final Class<?> eventType = GenericTypeResolver.resolveTypeArgument(listener.getClass(), ApplicationListener.class);
      addApplicationListener(listener, eventType, listeners);
    }
  }

  /**
   * Register to registry
   *
   * @param applicationListeners
   *         Registry
   * @param applicationListener
   *         The instance of application listener
   * @param eventType
   *         The event type
   */
  @SuppressWarnings({ "unchecked" })
  void addApplicationListener(ApplicationListener<?> applicationListener, Class<?> eventType,
                              Map<Class<?>, List<ApplicationListener<Object>>> applicationListeners) //
  {
    List<ApplicationListener<Object>> listeners = applicationListeners.get(eventType);
    if (listeners == null) {
      applicationListeners.put(eventType, listeners = new ArrayList<>(2));
      listeners.add((ApplicationListener<Object>) applicationListener);
    }
    else if (!listeners.contains(applicationListener)) {
      listeners.add((ApplicationListener<Object>) applicationListener);
      if (!listeners.isEmpty()) {
        OrderUtils.reversedSort(listeners);
      }
    }
  }

  /**
   * Process after {@link #registerListener(Collection, Map)}
   *
   * @param applicationListeners
   *         {@link ApplicationListener} cache
   */
  protected void postProcessRegisterListener(Map<Class<?>, List<ApplicationListener<Object>>> applicationListeners) {
    addApplicationListener(new ContextCloseListener());

    final Set<Class<?>> listeners = loadMetaInfoListeners();
    // load from strategy files
    log.info("Loading listeners from strategies files");
    final StrategiesDetector strategiesDetector = getStrategiesDetector();
    listeners.addAll(strategiesDetector.getTypes(ApplicationListener.class));

    for (final Class<?> listener : listeners) {
      registerListener(listener);
    }
  }

  /**
   * Load the META-INF/listeners
   *
   * @see Constant#META_INFO_listeners
   * @since 2.1.6
   */
  public Set<Class<?>> loadMetaInfoListeners() { // fixed #9 Some listener in a jar can't be load
    log.info("Loading META-INF/listeners");
    // Load the META-INF/listeners
    // ---------------------------------------------------
    return ContextUtils.loadFromMetaInfo(Constant.META_INFO_listeners);
  }

  @Override
  public void refresh() {
    try {
      // refresh object instance
      publishEvent(new ContextRefreshEvent(this));
      initializeSingletons();
    }
    catch (Throwable ex) {
      ex = ExceptionUtils.unwrapThrowable(ex);
      throw new ApplicationContextException("An Exception Occurred When Refresh Context: [" + this + "]", ex);
    }
  }

  @Override
  public void close() {
    applyState(State.CLOSING);
    publishEvent(new ContextCloseEvent(this));
    applyState(State.CLOSED);
  }

  // --------ApplicationEventPublisher

  @Override
  public void publishEvent(final Object event) {
    if (log.isDebugEnabled()) {
      log.debug("Publish event: [{}]", event);
    }
    final List<ApplicationListener<Object>> listeners = applicationListeners.get(event.getClass());
    if (CollectionUtils.isEmpty(listeners)) {
      return;
    }
    for (final ApplicationListener<Object> applicationListener : listeners) {
      applicationListener.onApplicationEvent(event);
    }
  }

  @Override
  public abstract AbstractBeanFactory getBeanFactory();

  @Override
  public ConfigurableEnvironment getEnvironment() {
    return environment;
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

  // ---------------------ConfigurableBeanFactory

  @Override
  public void registerBean(String name, BeanDefinition beanDefinition) {
    getBeanFactory().registerBean(name, beanDefinition);
  }

  @Override
  public void removeBean(String name) {
    getBeanFactory().removeBean(name);
  }

  @Override
  public void removeBean(Class<?> beanClass) {
    getBeanFactory().removeBean(beanClass);
  }

  @Override
  public void registerBean(String name, Class<?> clazz) {
    getBeanFactory().registerBean(name, clazz);
  }

  @Override
  public void registerBean(Class<?> clazz) {
    getBeanFactory().registerBean(clazz);
  }

  @Override
  public void registerBean(Set<Class<?>> clazz) {
    getBeanFactory().registerBean(clazz);
  }

  @Override
  public void registerBean(Object obj) {
    getBeanFactory().registerBean(obj);
  }

  @Override
  public void registerBean(String name, Object obj) {
    getBeanFactory().registerBean(name, obj);
  }

  @Override
  public <T> void registerBean(Class<T> clazz, Supplier<T> supplier, boolean prototype)
          throws BeanDefinitionStoreException {
    getBeanFactory().registerBean(clazz, supplier, prototype);
  }

  @Override
  public <T> void registerBean(Class<T> clazz, Supplier<T> supplier, boolean prototype, boolean ignoreAnnotation)
          throws BeanDefinitionStoreException {
    getBeanFactory().registerBean(clazz, supplier, prototype, ignoreAnnotation);
  }

  @Override
  public <T> void registerBean(String name, Supplier<T> supplier) throws BeanDefinitionStoreException {
    getBeanFactory().registerBean(name, supplier);
  }

  @Override
  public void destroyBean(String name) {
    getBeanFactory().destroyBean(name);
  }

  @Override
  public void destroyBean(String beanName, Object beanInstance) {
    getBeanFactory().destroyBean(beanName, beanInstance);
  }

  @Override
  public void destroyScopedBean(String beanName) {
    getBeanFactory().destroyScopedBean(beanName);
  }

  @Override
  public void refresh(String name) {
    getBeanFactory().refresh(name);
    // object refreshed
    publishEvent(new ObjectRefreshedEvent(getBeanDefinition(name), this));
  }

  @Override
  public Object refresh(BeanDefinition def) {
    final Object initializingBean = getBeanFactory().refresh(def);
    // object refreshed
    publishEvent(new ObjectRefreshedEvent(def, this));
    return initializingBean;
  }

  @Override
  public void initializeSingletons() {
    getBeanFactory().initializeSingletons();
  }

  @Override
  public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
    getBeanFactory().addBeanPostProcessor(beanPostProcessor);
  }

  @Override
  public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
    Assert.notNull(postProcessor, "BeanFactoryPostProcessor must not be null");

    getFactoryPostProcessors().add(postProcessor);
  }

  @Override
  public void removeBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
    getBeanFactory().removeBeanPostProcessor(beanPostProcessor);
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
  public <T> ObjectSupplier<T> getBeanSupplier(BeanDefinition def) {
    return getBeanFactory().getBeanSupplier(def);
  }

  @Override
  public <T> ObjectSupplier<T> getBeanSupplier(Class<T> requiredType) {
    return getBeanFactory().getBeanSupplier(requiredType);
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
  public void registerSingleton(String name, Object bean) {
    getBeanFactory().registerSingleton(name, bean);
  }

  @Override
  public void registerSingleton(Object bean) {
    getBeanFactory().registerSingleton(bean);
  }

  @Override
  public Map<String, Object> getSingletons() {
    return getBeanFactory().getSingletons();
  }

  @Override
  public Object getSingleton(String name) {
    return getBeanFactory().getSingleton(name);
  }

  @Override
  public <T> T getSingleton(Class<T> requiredType) {
    return getBeanFactory().getSingleton(requiredType);
  }

  @Override
  public void removeSingleton(String name) {
    getBeanFactory().removeSingleton(name);
  }

  @Override
  public boolean containsSingleton(String name) {
    return getBeanFactory().containsSingleton(name);
  }

  @Override
  public void registerBeanDefinition(String name, BeanDefinition beanDefinition) {
    getBeanFactory().registerBeanDefinition(name, beanDefinition);
  }

  @Override
  public void removeBeanDefinition(String beanName) {
    getBeanFactory().removeBeanDefinition(beanName);
  }

  @Override
  public BeanDefinition getBeanDefinition(String beanName) {
    return getBeanFactory().getBeanDefinition(beanName);
  }

  @Override
  public BeanDefinition getBeanDefinition(Class<?> beanClass) {
    return getBeanFactory().getBeanDefinition(beanClass);
  }

  @Override
  public boolean containsBeanDefinition(String beanName) {
    return getBeanFactory().containsBeanDefinition(beanName);
  }

  @Override
  public boolean containsBeanDefinition(Class<?> type) {
    return getBeanFactory().containsBeanDefinition(type);
  }

  @Override
  public boolean containsBeanDefinition(Class<?> type, boolean equals) {
    return getBeanFactory().containsBeanDefinition(type, equals);
  }

  @Override
  public Set<String> getBeanDefinitionNames() {
    return getBeanFactory().getBeanDefinitionNames();
  }

  @Override
  public int getBeanDefinitionCount() {
    return getBeanFactory().getBeanDefinitionCount();
  }

  @Override
  public void enableFullPrototype() {
    getBeanFactory().enableFullPrototype();
  }

  @Override
  public void enableFullLifecycle() {
    getBeanFactory().enableFullLifecycle();
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
  public void setFullLifecycle(boolean fullLifecycle) {
    getBeanFactory().setFullLifecycle(fullLifecycle);
  }

  @Override
  public void setFullPrototype(boolean fullPrototype) {
    getBeanFactory().setFullPrototype(fullPrototype);
  }

  // AutowireCapableBeanFactory
  // ----------------------------

  @Override
  public <T> T createBean(final Class<T> beanClass, final boolean cacheBeanDef) {
    return getBeanFactory().createBean(beanClass, cacheBeanDef);
  }

  @Override
  public void autowireBean(final Object existingBean) {
    getBeanFactory().autowireBean(existingBean);
  }

  @Override
  public void autowireBeanProperties(final Object existingBean) {
    getBeanFactory().autowireBeanProperties(existingBean);
  }

  @Override
  public Object initializeBean(Object existingBean) throws BeanInitializingException {
    return getBeanFactory().initializeBean(existingBean);
  }

  @Override
  public Object initializeBean(final Object existingBean, final String beanName) {
    return getBeanFactory().initializeBean(existingBean, beanName);
  }

  @Override
  public Object initializeBean(final Object existingBean, final BeanDefinition def) {
    return getBeanFactory().initializeBean(existingBean, def);
  }

  @Override
  public Object applyBeanPostProcessorsAfterInitialization(final Object existingBean, final String beanName) {
    return getBeanFactory().applyBeanPostProcessorsAfterInitialization(existingBean, beanName);
  }

  @Override
  public Object applyBeanPostProcessorsBeforeInitialization(final Object existingBean, final String beanName) {
    return getBeanFactory().applyBeanPostProcessorsBeforeInitialization(existingBean, beanName);
  }

  @Override
  public void destroyBean(final Object existingBean) {
    getBeanFactory().destroyBean(existingBean);
  }

  @Override
  public void destroyBean(Object beanInstance, BeanDefinition def) {
    getBeanFactory().destroyBean(beanInstance, def);
  }

  @Override
  public void importBeans(final Class<?>... beans) {
    getBeanFactory().importBeans(beans);
  }

  @Override
  public void importAnnotated(final BeanDefinition annotated) {
    getBeanFactory().importAnnotated(annotated);
  }

  @Override
  public void importBeans(final Set<BeanDefinition> defs) {
    getBeanFactory().importBeans(defs);
  }

  // ----------------------------

  public void setPropertiesLocation(String propertiesLocation) {
    if (StringUtils.isNotEmpty(propertiesLocation)) {
      getEnvironment().setPropertiesLocation(propertiesLocation);
    }
  }

  // @since 2.1.7
  // ---------------------------

  @Override
  public CandidateComponentScanner getCandidateComponentScanner() {
    final CandidateComponentScanner ret = this.candidateComponentScanner;
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

  public final List<BeanFactoryPostProcessor> getFactoryPostProcessors() {
    final ArrayList<BeanFactoryPostProcessor> processors = this.factoryPostProcessors;
    if (processors == null) {
      return this.factoryPostProcessors = new ArrayList<>();
    }
    return processors;
  }

  @Override
  public void registerScope(String name, Scope scope) {
    getBeanFactory().registerScope(name, scope);
  }

  @Override
  public StrategiesDetector getStrategiesDetector() {
    StrategiesDetector strategiesDetector = this.strategiesDetector;
    if (strategiesDetector == null) {
      strategiesDetector = createStrategiesLoader(this);
      this.strategiesDetector = strategiesDetector;
    }
    return strategiesDetector;
  }

  protected StrategiesDetector createStrategiesLoader(ConfigurableApplicationContext context) {
    final StrategiesDetector detector = StrategiesDetector.getSharedInstance();
    detector.setBeanFactory(context);
    return detector;
  }

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
