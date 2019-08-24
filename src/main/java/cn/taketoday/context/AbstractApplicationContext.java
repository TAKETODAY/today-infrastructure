/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.el.ELManager;
import javax.el.ELProcessor;
import javax.el.ExpressionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.context.annotation.ContextListener;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.BeanReference;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.el.ValueELContext;
import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.env.DefaultBeanNameCreator;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.env.StandardEnvironment;
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
import cn.taketoday.context.factory.BeanPostProcessor;
import cn.taketoday.context.listener.ApplicationListener;
import cn.taketoday.context.listener.ContextCloseListener;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.loader.DefaultBeanDefinitionLoader;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.OrderUtils;

/**
 * @author TODAY <br>
 *         2018-09-09 22:02
 */
public abstract class AbstractApplicationContext implements ConfigurableApplicationContext {

    private static final Logger log = LoggerFactory.getLogger(AbstractApplicationContext.class);

    private long startupDate;

    private ConfigurableEnvironment environment;

    // @since 2.1.5
    private State state;
    /** application listeners **/
    private final Map<Class<?>, List<ApplicationListener<EventObject>>> applicationListeners = new HashMap<>(32, 1.0f);

    public AbstractApplicationContext() {
        applyState(State.NONE);
        ContextUtils.applicationContext = this; // @since 2.1.6
    }

    /**
     * Load all the class in class path
     */
    public void loadContext() {
        this.loadContext(Constant.BLANK);
    }

    /**
     * Load class with given package locations in class path
     *
     * @param locations
     *            Given packages
     */
    @Override
    public void loadContext(String... locations) {
        this.loadContext(ClassUtils.scan(locations));
    }

    @Override
    public void loadContext(Collection<Class<?>> classes) {

        try {
            // Prepare refresh
            prepareRefresh();
            // Prepare BeanFactory
            prepareBeanFactory(classes);
            // Initialize other special beans in specific context subclasses.
            onRefresh();
            // Lazy loading
            if (!getEnvironment().getProperty(Constant.ENABLE_LAZY_LOADING, Boolean::parseBoolean, false)) {
                refresh(); // Initialize all singletons.
            }
            // Finish refresh
            finishRefresh();
        }
        catch (Throwable ex) {
            applyState(State.FAILED);
            ex = ExceptionUtils.unwrapThrowable(ex);
            log.error("An Exception Occurred When Loading Context, With Msg: [{}]", ex.toString(), ex);
            throw ExceptionUtils.newContextException(ex);
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
            log.error("An Exception Occurred When Loading Properties, With Msg: [{}]", ex.toString(), ex);
            throw new ContextException(ex);
        }
        postProcessLoadProperties(environment);

        {// @since 2.1.6
            if (environment.getProperty(Constant.ENABLE_FULL_PROTOTYPE, boolean.class, false)) {
                enableFullPrototype();
            }
            if (environment.getProperty(Constant.ENABLE_FULL_LIFECYCLE, boolean.class, false)) {
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
     * 
     * @throws Throwable
     */
    protected void onRefresh() throws Throwable {
        publishEvent(new ContextPreRefreshEvent(this));
        // fix: #1 some singletons could not be initialized.
        getBeanFactory().preInitialization();
    }

    /**
     * Prepare a bean factory
     * 
     * @param classes
     *            class set
     */
    public void prepareBeanFactory(Collection<Class<?>> classes) throws Throwable {

        final AbstractBeanFactory beanFactory = getBeanFactory();

        final ConfigurableEnvironment environment = getEnvironment();
        BeanNameCreator beanNameCreator = environment.getBeanNameCreator();
        // check name creator
        if (beanNameCreator == null) {
            beanNameCreator = createBeanNameCreator();
            environment.setBeanNameCreator(beanNameCreator);
        }
        // must not be null
        beanFactory.setBeanNameCreator(beanNameCreator);
        // check registry
        if (environment.getBeanDefinitionRegistry() == null) {
            environment.setBeanDefinitionRegistry(beanFactory);
        }
        // check bean definition loader
        BeanDefinitionLoader beanDefinitionLoader = environment.getBeanDefinitionLoader();
        if (beanDefinitionLoader == null) {
            beanDefinitionLoader = new DefaultBeanDefinitionLoader(this);
            environment.setBeanDefinitionLoader(beanDefinitionLoader);
        }
        beanFactory.setBeanDefinitionLoader(beanDefinitionLoader);

        {// fix: ensure ExpressionFactory's instance consistent @since 2.1.6
            Field declaredField = ClassUtils.forName("javax.el.ELUtil").getDeclaredField("exprFactory");
            ClassUtils.makeAccessible(declaredField)//
                    .set(null, ExpressionFactory.newInstance(environment.getProperties()));
        }
        ELProcessor elProcessor = environment.getELProcessor();
        if (elProcessor == null) {
            // setting el manager @since 2.1.5
            final ELManager elManager = new ELManager();
            elManager.setELContext(new ValueELContext(this));

            elProcessor = new ELProcessor(elManager);
            environment.setELProcessor(elProcessor);
        }

        // register ELManager @since 2.1.5
        // fix @since 2.1.6 elManager my be null
        registerSingleton(beanNameCreator.create(ELManager.class), elProcessor.getELManager());

        registerSingleton(beanNameCreator.create(ELProcessor.class), elProcessor);
        // register Environment
        registerSingleton(beanNameCreator.create(Environment.class), environment);
        // register ApplicationContext
        registerSingleton(beanNameCreator.create(ApplicationContext.class), this);

        // register listener
        registerListener(applicationListeners);

        // start loading bean definitions ; publish loading bean definition event
        publishEvent(new BeanDefinitionLoadingEvent(this));
        loadBeanDefinitions(beanFactory, classes);
        // bean definitions loaded
        publishEvent(new BeanDefinitionLoadedEvent(this, beanFactory.getBeanDefinitions()));
        // handle dependency : register bean dependencies definition
        beanFactory.handleDependency();
        publishEvent(new DependenciesHandledEvent(this, beanFactory.getDependencies()));

        postProcessBeanFactory(beanFactory);
    }

    /**
     * Process after {@link #prepareBeanFactory(Collection)}
     * 
     * @param beanFactory
     *            bean factory
     */
    protected void postProcessBeanFactory(AbstractBeanFactory beanFactory) {

        // register bean post processors
        beanFactory.registerBeanPostProcessors();

        if (beanFactory.isFullPrototype()) {
            for (PropertyValue propertyValue : beanFactory.getDependencies()) {
                final BeanReference beanReference = (BeanReference) propertyValue.getValue();
                final BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanReference.getName());
                if (beanDefinition != null && !beanDefinition.isSingleton()) {
                    beanReference.applyPrototype();
                }
            }
        }
    }

    /**
     * Load all the application listeners in context and register it.
     * 
     * @param applicationListeners
     *            {@link ApplicationListener} cache
     */
    protected void registerListener(Map<Class<?>, List<ApplicationListener<EventObject>>> applicationListeners) {

        log.debug("Loading Application Listeners.");

        for (final Class<?> contextListener : ClassUtils.getAnnotatedClasses(ContextListener.class)) {
            registerListener(contextListener);
        }

        postProcessRegisterListener(applicationListeners);

        // sort
        for (Entry<Class<?>, List<ApplicationListener<EventObject>>> entry : applicationListeners.entrySet()) {
            OrderUtils.reversedSort(entry.getValue());
        }
    }

    protected void registerListener(Class<?> listenerClass) {

        if (!ApplicationListener.class.isAssignableFrom(listenerClass)) {
            throw ExceptionUtils.newConfigurationException(null, "ContextListener must be a 'ApplicationListener'");
        }

        try {

            final String name = getEnvironment().getBeanNameCreator().create(listenerClass);
            // if exist bean
            Object applicationListener = getSingleton(name);
            if (applicationListener == null) {
                // create bean instance
                applicationListener = ClassUtils.newInstance(listenerClass);
                registerSingleton(name, applicationListener);
            }

            addApplicationListener((ApplicationListener<?>) applicationListener);
        }
        catch (Throwable ex) {
            ex = ExceptionUtils.unwrapThrowable(ex);
            log.error("An Exception Occurred When Register Application Listener, With Msg: [{}]", ex.toString(), ex);
            throw ExceptionUtils.newContextException(ex);
        }
    }

    @Override
    public void addApplicationListener(final ApplicationListener<?> applicationListener) {

        for (final Method method : applicationListener.getClass().getDeclaredMethods()) {
            // onApplicationEvent
            if (!method.isBridge() && method.getName().equals(Constant.ON_APPLICATION_EVENT)) {
                // register listener
                doRegisterListener(this.applicationListeners, applicationListener, method.getParameterTypes()[0]);
            }
        }
    }

    /**
	 * Register to registry
	 * 
	 * @param applicationListeners 
	 * 				Registry
	 * @param applicationListener
	 *            The instance of application listener
	 * @param eventType
	 *            The event type
	 * @off
	 */
	@SuppressWarnings({ "unchecked" })
	private	static void doRegisterListener(Map<Class<?>, List<ApplicationListener<EventObject>>> applicationListeners, //
			Object applicationListener, Class<?> eventType) //
	{
		if (applicationListeners.containsKey(eventType)) {
			applicationListeners.get(eventType).add((ApplicationListener<EventObject>) applicationListener);
			return;
		}
		final ArrayList<ApplicationListener<EventObject>> listeners = new ArrayList<>();
		listeners.add((ApplicationListener<EventObject>) applicationListener);
		applicationListeners.put(eventType, listeners);
	}
	//@on

    /**
     * Process after {@link #registerListener()}
     * 
     * @param applicationListeners
     *            {@link ApplicationListener} cache
     */
    protected void postProcessRegisterListener(Map<Class<?>, List<ApplicationListener<EventObject>>> applicationListeners) {

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

        // Load the META-INF/listeners
        // ---------------------------------------------------
        final Set<Class<?>> listeners = new HashSet<>();

        try {

            final ClassLoader classLoader = ClassUtils.getClassLoader();
            final Enumeration<URL> resources = classLoader.getResources("META-INF/listeners");
            final Charset charset = Constant.DEFAULT_CHARSET;

            while (resources.hasMoreElements()) {
                try (final BufferedReader reader = new BufferedReader(//
                        new InputStreamReader(resources.nextElement().openStream(), charset))) { // fix

                    String str;
                    while ((str = reader.readLine()) != null) {
                        listeners.add(classLoader.loadClass(str));
                    }
                }
            }
        }
        catch (IOException | ClassNotFoundException e) {
            LoggerFactory.getLogger(getClass()).error("Exception occurred when load 'META-INF/listeners'", e);
            throw ExceptionUtils.newContextException(e);
        }
        return listeners;
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
            log.error("An Exception Occurred When Refresh Context: [{}] With Msg: [{}]", this, ex.toString(), ex);
            throw ExceptionUtils.newContextException(ex);
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
    public void publishEvent(EventObject event) {

        if (log.isDebugEnabled()) {
            log.debug("Publish event: [{}]", event.getClass().getName());
        }

        final List<ApplicationListener<EventObject>> listeners = applicationListeners.get(event.getClass());
        if (listeners == null || listeners.isEmpty()) {
            return;
        }

        for (final ApplicationListener<EventObject> applicationListener : listeners) {
            applicationListener.onApplicationEvent(event);
        }
    }

    public abstract AbstractBeanFactory getBeanFactory();

    @Override
    public void setEnvironment(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public ConfigurableEnvironment getEnvironment() {
        if (this.environment == null) {
            this.environment = createEnvironment();
        }
        return this.environment;
    }

    /**
     * create {@link BeanNameCreator}
     * 
     * @return a default {@link BeanNameCreator}
     */
    protected BeanNameCreator createBeanNameCreator() {
        return new DefaultBeanNameCreator(getEnvironment());
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
    public void destroyBean(String name) {
        getBeanFactory().destroyBean(name);
    }

    @Override
    public void refresh(String name) {
        try {
            getBeanFactory().refresh(name);
            // object refreshed
            publishEvent(new ObjectRefreshedEvent(getBeanDefinition(name), this));
        }
        catch (Throwable ex) {
            ex = ExceptionUtils.unwrapThrowable(ex);
            log.error("Can't refresh a bean named: [{}], With Msg: [{}]", name, ex.getMessage(), ex);
            throw ExceptionUtils.newContextException(ex);
        }
    }

    @Override
    public Object refresh(BeanDefinition beanDefinition) {
        try {

            final Object initializingBean = getBeanFactory().refresh(beanDefinition);
            // object refreshed
            publishEvent(new ObjectRefreshedEvent(beanDefinition, this));
            return initializingBean;
        }
        catch (Throwable ex) {
            ex = ExceptionUtils.unwrapThrowable(ex);
            log.error("Can't refresh a bean named: [{}], With Msg: [{}]", beanDefinition.getName(), ex.getMessage(), ex);
            throw ExceptionUtils.newContextException(ex);
        }
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
    public void removeBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        getBeanFactory().removeBeanPostProcessor(beanPostProcessor);
    }

    // ------------------- BeanFactory

    @Override
    public Object getBean(String name) {
        return getBeanFactory().getBean(name);
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
        getEnvironment().setPropertiesLocation(propertiesLocation);
    }

}
