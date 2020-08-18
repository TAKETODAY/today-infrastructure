/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.context.annotation.ContextListener;
import cn.taketoday.context.el.ValueExpressionContext;
import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.env.StandardEnvironment;
import cn.taketoday.context.event.ApplicationEventCapable;
import cn.taketoday.context.event.BeanDefinitionLoadedEvent;
import cn.taketoday.context.event.BeanDefinitionLoadingEvent;
import cn.taketoday.context.event.ContextCloseEvent;
import cn.taketoday.context.event.ContextPreRefreshEvent;
import cn.taketoday.context.event.ContextRefreshEvent;
import cn.taketoday.context.event.ContextStartedEvent;
import cn.taketoday.context.event.DependenciesHandledEvent;
import cn.taketoday.context.event.ObjectRefreshedEvent;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.factory.AbstractBeanFactory;
import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.factory.BeanFactoryPostProcessor;
import cn.taketoday.context.factory.BeanPostProcessor;
import cn.taketoday.context.factory.BeanReference;
import cn.taketoday.context.factory.PropertyValue;
import cn.taketoday.context.listener.ApplicationListener;
import cn.taketoday.context.listener.ContextCloseListener;
import cn.taketoday.context.loader.CandidateComponentScanner;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.expression.ExpressionFactory;
import cn.taketoday.expression.ExpressionManager;
import cn.taketoday.expression.ExpressionProcessor;

import static cn.taketoday.context.exception.ConfigurationException.nonNull;

/**
 * @author TODAY <br>
 *         2018-09-09 22:02
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
     * Construct with a {@link ConfigurableEnvironment}
     * 
     * @param env
     *            {@link ConfigurableEnvironment} instance
     * @since 2.1.7
     */
    public AbstractApplicationContext(ConfigurableEnvironment env) {
        applyState(State.NONE);
        this.environment = env;
        ContextUtils.setLastStartupContext(this); // @since 2.1.6
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

    /**
     * Load all the class in class path
     */
    public void loadContext() {
        loadContext(Constant.BLANK);
    }

    @Override
    public void loadContext(Collection<Class<?>> candidates) {
        final Set<Class<?>> candidateSet = candidates instanceof Set ? (Set<Class<?>>) candidates : new HashSet<>(candidates);
        getCandidateComponentScanner().setCandidates(candidateSet);
        
        loadContext((String[]) null);
    }

    /**
     * Load class with given package locations in class path
     *
     * @param locations
     *            Given packages
     */
    @Override
    public void loadContext(String... locations) {
        this.locations = locations;

        try {
            // Prepare refresh
            prepareRefresh();
            // Prepare BeanFactory
            prepareBeanFactory();
            // Initialize other special beans in specific context subclasses.
            preRefresh();
            // Lazy loading
            if (!getEnvironment().getFlag(Constant.ENABLE_LAZY_LOADING)) {
                refresh(); // Initialize all singletons.
            }
            // Finish refresh
            finishRefresh();
        }
        catch (Throwable ex) {
            close();
            applyState(State.FAILED);
            ex = ExceptionUtils.unwrapThrowable(ex);
            throw new ContextException("An Exception Occurred When Loading Context, With Msg: [" + ex + "]", ex);
        }
    }

    /**
     * Prepare to load context
     */
    protected void prepareRefresh() {

        this.startupDate = System.currentTimeMillis();
        log.info("Starting Application Context at [{}].", //
                 new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT).format(startupDate));

        applyState(State.STARTING);

        // prepare properties
        final ConfigurableEnvironment environment = getEnvironment();
        try {
            environment.loadProperties();
        }
        catch (IOException ex) {
            throw new ContextException("An Exception Occurred When Loading Properties, With Msg: [" + ex + ']', ex);
        }
        postProcessLoadProperties(environment);

        {// @since 2.1.6
            if (environment.getFlag(Constant.ENABLE_FULL_PROTOTYPE)) {
                enableFullPrototype();
            }
            if (environment.getFlag(Constant.ENABLE_FULL_LIFECYCLE)) {
                enableFullLifecycle();
            }
        }
    }

    /**
     * Post process after load properties
     *
     * @param environment
     *            {@link Environment}
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
     *            Bean factory
     * @param beanClasses
     *            Bean classes
     */
    protected void loadBeanDefinitions(AbstractBeanFactory beanFactory, Collection<Class<?>> beanClasses) {
        // load from given class set
        beanFactory.getBeanDefinitionLoader().loadBeanDefinitions(beanClasses);
    }

    /**
     * Context start success
     */
    protected void finishRefresh() {
        // clear cache
        ClassUtils.clearCache();
        // start success publish started event
        publishEvent(new ContextStartedEvent(this));
        applyState(State.STARTED);

        log.info("Application Context Startup in {}ms", System.currentTimeMillis() - getStartupDate());
    }

    /**
     * Template method
     */
    protected void preRefresh() {
        publishEvent(new ContextPreRefreshEvent(this));
        // fix: #1 some singletons could not be initialized.
        getBeanFactory().preInitialization();
    }

    public void prepareBeanFactory() {

        final AbstractBeanFactory beanFactory = getBeanFactory();
        final ConfigurableEnvironment env = getEnvironment();
        // must not be null
        // check registry
        if (env.getBeanDefinitionRegistry() == null) {
            env.setBeanDefinitionRegistry(beanFactory);
        }
        // check bean definition loader
        if (env.getBeanDefinitionLoader() == null) {
            env.setBeanDefinitionLoader(beanFactory.getBeanDefinitionLoader());
        }
        // Expression
        if (env.getExpressionProcessor() == null) {
            final ExpressionFactory exprFactory = ExpressionFactory.getSharedInstance();
            final ValueExpressionContext elContext = new ValueExpressionContext(exprFactory, beanFactory);
            elContext.defineBean(Constant.ENV, env.getProperties()); // @since 2.1.6
            env.setExpressionProcessor(new ExpressionProcessor(new ExpressionManager(elContext, exprFactory)));
        }
        // register framework beans
        log.debug("Registering framework beans");
        registerFrameworkBeans(beanFactory.getBeanNameCreator());
        // Loading candidates components
        log.debug("Loading candidates components");
        final Set<Class<?>> candidates = getComponentCandidates();
        log.debug("There are [{}] candidates components in [{}]", candidates.size(), this);
        // register listener
        registerListener(candidates, applicationListeners);

        // start loading bean definitions ; publish loading bean definition event
        publishEvent(new BeanDefinitionLoadingEvent(this, candidates));
        loadBeanDefinitions(beanFactory, candidates);
        // bean definitions loaded
        publishEvent(new BeanDefinitionLoadedEvent(this, beanFactory.getBeanDefinitions()));
        // handle dependency : register bean dependencies definition
        beanFactory.handleDependency();
        publishEvent(new DependenciesHandledEvent(this, beanFactory.getDependencies()));

        postProcessBeanFactory(beanFactory);

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
     * Register Framework Beans
     *
     * @param beanNameCreator
     *            Bean name creator to create bean name
     */
    protected void registerFrameworkBeans(final BeanNameCreator beanNameCreator) {

        final ConfigurableEnvironment env = getEnvironment();
        final ExpressionProcessor elProcessor = env.getExpressionProcessor();

        // register ELManager @since 2.1.5
        // fix @since 2.1.6 elManager my be null
        registerSingleton(beanNameCreator.create(ExpressionManager.class), elProcessor.getManager());

        registerSingleton(beanNameCreator.create(ExpressionProcessor.class), elProcessor);
        // register Environment
        registerSingleton(beanNameCreator.create(Environment.class), env);
        // register ApplicationContext
        registerSingleton(beanNameCreator.create(ApplicationContext.class), this);
        // register BeanFactory @since 2.1.7
        registerSingleton(beanNameCreator.create(BeanFactory.class), getBeanFactory());
    }

    /**
     * Process after {@link #prepareBeanFactory}
     *
     * @param beanFactory
     *            bean factory
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
            for (PropertyValue propertyValue : beanFactory.getDependencies()) {
                final BeanReference ref = (BeanReference) propertyValue.getValue();
                final BeanDefinition def = beanFactory.getBeanDefinition(ref.getName());
                if (def != null && def.isPrototype()) {
                    ref.applyPrototype();
                }
            }
        }

    }

    /**
     * Register {@link BeanFactoryPostProcessor}s
     */
    public void registerBeanFactoryPostProcessor() {
        log.debug("Start loading BeanFactoryPostProcessor.");

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
     *            {@link ApplicationListener} cache
     */
    protected void registerListener(final Collection<Class<?>> classes,
                                    final Map<Class<?>, List<ApplicationListener<Object>>> applicationListeners) //
    {
        log.debug("Loading Application Listeners.");

        for (final Class<?> contextListener : classes) {
            if (contextListener.isAnnotationPresent(ContextListener.class)) {
                registerListener(contextListener);
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
     *            Must be {@link ApplicationListener} class
     *
     * @throws ConfigurationException
     *             If listenerClass isn't a {@link ApplicationListener}
     * @see #getEnvironment()
     */
    protected void registerListener(Class<?> listenerClass) {

        if (!ApplicationListener.class.isAssignableFrom(listenerClass)) {
            throw new ConfigurationException("ContextListener must be a 'ApplicationListener'");
        }
        try {
            final String name = getEnvironment().getBeanNameCreator().create(listenerClass);
            // if exist bean
            Object applicationListener = getSingleton(name);
            if (applicationListener == null) {
                // create bean instance
                applicationListener = ClassUtils.newInstance(listenerClass, this);
                registerSingleton(name, applicationListener);
            }
            addApplicationListener((ApplicationListener<?>) applicationListener);
        }
        catch (Throwable ex) {
            ex = ExceptionUtils.unwrapThrowable(ex);
            throw new ContextException("An Exception Occurred When Register Application Listener, With Msg: [" + ex + "]", ex);
        }
    }

    @Override
    public void addApplicationListener(final ApplicationListener<?> applicationListener) {
        nonNull(applicationListener, "applicationListener can't be null");

        final HashMap<Class<?>, List<ApplicationListener<Object>>> applicationListeners = this.applicationListeners;
        if (applicationListener instanceof ApplicationEventCapable) { // @since 2.1.7
            for (final Class<?> type : ((ApplicationEventCapable) applicationListener).getApplicationEvent()) {
                addApplicationListener(applicationListener, type, applicationListeners);
            }
        }
        else {
            for (final Method method : applicationListener.getClass().getDeclaredMethods()) {
                // onApplicationEvent
                if (!method.isBridge() && method.getName().equals(Constant.ON_APPLICATION_EVENT)) {
                    // register listener
                    addApplicationListener(applicationListener, method.getParameterTypes()[0], applicationListeners);
                    break;
                }
            }
        }
    }

    /**
     * Register to registry
     *
     * @param applicationListeners
     *            Registry
     * @param applicationListener
     *            The instance of application listener
     * @param eventType
     *            The event type
     */
    @SuppressWarnings({ "unchecked" })
    protected void addApplicationListener(Object applicationListener, Class<?> eventType,
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
     *            {@link ApplicationListener} cache
     */
    protected void postProcessRegisterListener(Map<Class<?>, List<ApplicationListener<Object>>> applicationListeners) {

        addApplicationListener(new ContextCloseListener());

        for (final Class<?> listener : loadMetaInfoListeners()) {
            registerListener(listener);
        }
    }

    /**
     * Load the META-INF/listeners
     *
     * @since 2.1.6
     */
    public Set<Class<?>> loadMetaInfoListeners() { // fixed #9 Some listener in a jar can't be load

        log.debug("Loading META-INF/listeners");
        // Load the META-INF/listeners
        // ---------------------------------------------------
        return ContextUtils.loadFromMetaInfo("META-INF/listeners");
    }

    @Override
    public void refresh() throws ContextException {
        try {
            // refresh object instance
            publishEvent(new ContextRefreshEvent(this));
            getBeanFactory().initializeSingletons();
        }
        catch (Throwable ex) {
            ex = ExceptionUtils.unwrapThrowable(ex);
            throw new ContextException("An Exception Occurred When Refresh Context: [" + this + "] With Msg: [" + ex + "]", ex);
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
    public void setEnvironment(ConfigurableEnvironment environment) {

    }

    @Override
    public ConfigurableEnvironment getEnvironment() {
        return environment;
    }

    /**
     * create {@link ConfigurableEnvironment}
     *
     * @return a default {@link ConfigurableEnvironment}
     */
    protected ConfigurableEnvironment createEnvironment() {
        return new StandardEnvironment();
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
    public void registerBean(String name, BeanDefinition beanDefinition) throws BeanDefinitionStoreException {
        getBeanFactory().registerBean(name, beanDefinition);
    }

    @Override
    public void removeBean(String name) throws BeanDefinitionStoreException {
        getBeanFactory().removeBean(name);
    }

    @Override
    public void registerBean(String name, Class<?> clazz) throws BeanDefinitionStoreException {
        getBeanFactory().registerBean(name, clazz);
    }

    @Override
    public void registerBean(Class<?> clazz) throws BeanDefinitionStoreException, ConfigurationException {
        getBeanFactory().registerBean(clazz);
    }

    @Override
    public void registerBean(Set<Class<?>> clazz) throws BeanDefinitionStoreException {
        getBeanFactory().registerBean(clazz);
    }

    @Override
    public void registerBean(Object obj) throws BeanDefinitionStoreException {
        getBeanFactory().registerBean(obj);
    }

    @Override
    public void registerBean(String name, Object obj) throws BeanDefinitionStoreException {
        getBeanFactory().registerBean(name, obj);
    }

    @Override
    public void destroyBean(String name) {
        getBeanFactory().destroyBean(name);
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
    public void initializeSingletons() throws Throwable {
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
    public Object getScopeBean(BeanDefinition def, Scope scope) {
        return getBeanFactory().getScopeBean(def, scope);
    }

    @Override
    public <T> List<T> getBeans(Class<T> requiredType) {
        return getBeanFactory().getBeans(requiredType);
    }

    @Override
    public <T extends Annotation, E> List<E> getAnnotatedBeans(Class<T> annotationType) {
        return getBeanFactory().getAnnotatedBeans(annotationType);
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> requiredType) {
        return getBeanFactory().getBeansOfType(requiredType);
    }

    @Override
    public Map<String, BeanDefinition> getBeanDefinitions() {
        return getBeanFactory().getBeanDefinitions();
    }

    @Override
    public Map<String, BeanDefinition> getBeanDefinitionsMap() {
        return getBeanFactory().getBeanDefinitionsMap();
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
    public Map<String, Object> getSingletonsMap() {
        return getBeanFactory().getSingletonsMap();
    }

    @Override
    public Object getSingleton(String name) {
        return getBeanFactory().getSingleton(name);
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

    // ----------------------

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
        final ArrayList<BeanFactoryPostProcessor> factoryPostProcessors = this.factoryPostProcessors;
        if (factoryPostProcessors == null) {
            return this.factoryPostProcessors = new ArrayList<>();
        }
        return factoryPostProcessors;
    }

    @Override
    public void registerScope(String name, Scope scope) {
        getBeanFactory().registerScope(name, scope);
    }
}
