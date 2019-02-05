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

import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.ComponentImpl;
import cn.taketoday.context.annotation.ContextListener;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.env.DefaultBeanNameCreator;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.env.StandardEnvironment;
import cn.taketoday.context.event.BeanDefinitionLoadedEvent;
import cn.taketoday.context.event.BeanDefinitionLoadingEvent;
import cn.taketoday.context.event.ContextCloseEvent;
import cn.taketoday.context.event.ContextRefreshEvent;
import cn.taketoday.context.event.ContextStartedEvent;
import cn.taketoday.context.event.HandleDependencyEvent;
import cn.taketoday.context.event.ObjectRefreshedEvent;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.factory.AbstractBeanFactory;
import cn.taketoday.context.factory.BeanPostProcessor;
import cn.taketoday.context.listener.ApplicationListener;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.loader.DefaultBeanDefinitionLoader;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.OrderUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Today <br>
 *         <p>
 *         2018-09-09 22:02
 */
public abstract class AbstractApplicationContext implements ConfigurableApplicationContext {

	private static final Logger log = LoggerFactory.getLogger(AbstractApplicationContext.class);

	private final long startupDate;
	private String propertiesLocation = ""; // default ""
	private BeanNameCreator beanNameCreator;
	private ConfigurableEnvironment environment;
	private final AtomicBoolean started = new AtomicBoolean(false);

	/** application listeners **/
	private final Map<Class<?>, List<ApplicationListener<EventObject>>> applicationListeners = new HashMap<>(10, 1.0f);

	public AbstractApplicationContext() {
		this.startupDate = System.currentTimeMillis();
		log.info("Starting Application Context at [{}].", //
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(startupDate)));
	}

	/**
	 * Load all the class in class path
	 */
	public void loadContext() {
		this.loadContext("");
	}

	/**
	 * Load class with given package locations in class path
	 *
	 * @param locations
	 *            given packages
	 */
	public void loadContext(String... locations) {
		this.loadContext(ClassUtils.scan(locations));
	}

	/**
	 * @param clazz
	 *            class set
	 */
	@Override
	public void loadContext(Collection<Class<?>> classes) {
		try {
			// Prepare refresh
			prepareRefresh();
			// Prepare BeanFactory
			prepareBeanFactory(classes);
			// Initialize other special beans in specific context subclasses.
			onRefresh();
			// Initialize all singletons.
			refresh();
			// finish refresh
			finishRefresh();
		}
		catch (Throwable ex) {
			ex = ExceptionUtils.unwrapThrowable(ex);
			log.error("An Exception Occurred When Loading Context, With Msg: [{}]", ex.getMessage(), ex);
		}
	}

	/**
	 * Prepare to load context
	 * 
	 * @throws IOException
	 */
	protected void prepareRefresh() throws IOException {
		// prepare properties
		getEnvironment().loadProperties(propertiesLocation);
	}

	/**
	 * @param beanFactory
	 *            bean factory
	 * @param beanClasses
	 *            bean classes
	 * @throws Throwable
	 */
	protected abstract void doLoadBeanDefinitions(AbstractBeanFactory beanFactory, Collection<Class<?>> beanClasses) //
			throws Throwable;

	/**
	 * Context start success
	 */
	private void finishRefresh() {
		// clear cache
		ClassUtils.clearCache();
		// start success publish started event
		publishEvent(new ContextStartedEvent(this));
		started.set(true);
	}

	/**
	 * Template method
	 */
	protected void onRefresh() throws ContextException {

	}

	/**
	 * @param classes
	 * @throws Throwable
	 */
	public void prepareBeanFactory(Collection<Class<?>> classes) throws Throwable {

		final AbstractBeanFactory beanFactory = getBeanFactory();

		//
		postProcessBeanFactory(beanFactory);

		final ConfigurableEnvironment environment = getEnvironment();

		BeanNameCreator beanNameCreator = environment.getBeanNameCreator();
		// check name creator
		if (beanNameCreator == null) {
			beanNameCreator = new DefaultBeanNameCreator(environment);
			environment.setBeanNameCreator(beanNameCreator);
		}
		// check registry
		if (environment.getBeanDefinitionRegistry() == null) {
			environment.setBeanDefinitionRegistry(this);
		}
		// must not be null
		beanFactory.setBeanNameCreator(beanNameCreator);
		//
		// check bean definition loader
		BeanDefinitionLoader beanDefinitionLoader = environment.getBeanDefinitionLoader();
		if (beanDefinitionLoader == null) {
			beanDefinitionLoader = new DefaultBeanDefinitionLoader(this);
			environment.setBeanDefinitionLoader(beanDefinitionLoader);
		}
		beanFactory.setBeanDefinitionLoader(beanDefinitionLoader);
		// register Environment
		registerSingleton(beanNameCreator.create(Environment.class), environment);
		// register listener
		registerListener();
		// start loading bean definitions ; publish loading bean definition event
		publishEvent(new BeanDefinitionLoadingEvent(this));
		doLoadBeanDefinitions(beanFactory, classes);
		// bean definitions loaded
		publishEvent(new BeanDefinitionLoadedEvent(this));
		// handle dependency : register bean dependencies definition
		publishEvent(new HandleDependencyEvent(this));
		beanFactory.handleDependency();

		// register bean post processors
		beanFactory.registerBeanPostProcessors();
	}

	/**
	 * @param beanFactory
	 */
	protected void postProcessBeanFactory(AbstractBeanFactory beanFactory) {

	}

	/**
	 * Load all the application listeners in context and register it.
	 */
	void registerListener() {

		log.debug("Loading Application Listeners.");

		for (Class<?> listenerClass : ClassUtils.getImplClasses(ApplicationListener.class)) {
			forEach(listenerClass);
		}
		// sort
		for (Entry<Class<?>, List<ApplicationListener<EventObject>>> entry : this.applicationListeners.entrySet()) {
			OrderUtils.reversedSort(entry.getValue());
		}
	}

	/**
	 * @param listenerClass
	 */
	private void forEach(Class<?> listenerClass) {

		try {

			ContextListener contextListener = listenerClass.getAnnotation(ContextListener.class);
			if (contextListener == null) {
				return;
			}

			if (!ApplicationListener.class.isAssignableFrom(listenerClass)) {
				throw new ConfigurationException("[{}] must be a [{}]", //
						listenerClass.getName(), ApplicationListener.class.getClass().getName());
			}

			Component[] components = ClassUtils.getAnnotationArray(listenerClass, Component.class, ComponentImpl.class);
			String name = null;
			if (components != null && components.length != 0) {
				// bean name
				name = components[0].value()[0];
			}
			else {
				name = getBeanNameCreator().create(listenerClass);
			}

			// if exist bean
			Object applicationListener = getSingleton(name);

			if (applicationListener == null) {
				// create bean instance
				applicationListener = ClassUtils.newInstance(listenerClass);
				registerSingleton(name, applicationListener);
			}

			for (Method method : listenerClass.getDeclaredMethods()) {
				// onApplicationEvent
				if (method.getName().equals(Constant.ON_APPLICATION_EVENT)) {
					if (method.isBridge()) {
						continue;
					}
					// register listener
					doRegisterListener(applicationListener, method.getParameterTypes()[0]);
				}
			}
			// specify type
			Class<?>[] classes = contextListener.value();
			for (Class<?> class_ : classes) {
				// register listener
				doRegisterListener(applicationListener, class_);
			}
		}
		catch (Throwable ex) {
			ex = ExceptionUtils.unwrapThrowable(ex);
			log.error("An Exception Occurred When Register Application Listener, With Msg: [{}]", ex.getMessage(), ex);
		}
	}

	/**
	 * @param applicationListener
	 *            the instance of application listener
	 * @param eventType
	 *            the event type
	 * @off
	 */
	@SuppressWarnings({ "unchecked", "serial" })
	private void doRegisterListener(Object applicationListener, Class<?> eventType) {
		if (applicationListeners.containsKey(eventType)) {
			applicationListeners.get(eventType).add((ApplicationListener<EventObject>) applicationListener);
			return;
		}
		applicationListeners.put(eventType, new ArrayList<ApplicationListener<EventObject>>() {{
			add((ApplicationListener<EventObject>) applicationListener);
		}});
	}
	//@on

	@Override
	public void refresh() throws ContextException {

		try {

			// refresh object instance
			publishEvent(new ContextRefreshEvent(this));

			getBeanFactory().preInstantiateSingletons();
		}
		catch (Throwable ex) {
			ex = ExceptionUtils.unwrapThrowable(ex);
			log.error("An Exception Occurred When Refresh Context: [{}] With Msg: [{}]", this, ex.getMessage(), ex);
			throw new ContextException(ex);
		}
	}

	@Override
	public void close() {
		started.set(false);
		publishEvent(new ContextCloseEvent(this));
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

	/**
	 * get AbstractBeanFactory
	 * 
	 * @return
	 * @throws IllegalStateException
	 */
	public abstract AbstractBeanFactory getBeanFactory() throws IllegalStateException;

	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	public BeanNameCreator getBeanNameCreator() {
		if (this.beanNameCreator == null) {
			this.beanNameCreator = createBeanNameCreator();
		}
		return this.beanNameCreator;
	}

	@Override
	public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * @return
	 */
	protected BeanNameCreator createBeanNameCreator() {
		return new DefaultBeanNameCreator(getEnvironment());
	}

	/**
	 * @return
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardEnvironment();
	}

	@Override
	public boolean hasStarted() {
		return started.get();
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
			throw new ContextException(ex);
		}
	}

	@Override
	public Object refresh(BeanDefinition beanDefinition) {

		try {

			Object initializingBean = getBeanFactory().refresh(beanDefinition);
			// object refreshed
			publishEvent(new ObjectRefreshedEvent(beanDefinition, this));
			return initializingBean;
		}
		catch (Throwable ex) {
			ex = ExceptionUtils.unwrapThrowable(ex);
			log.error("Can't refresh a bean named: [{}], With Msg: [{}]", beanDefinition.getName(), ex.getMessage(), ex);
			throw new ContextException(ex);
		}
	}

	@Override
	public void preInstantiateSingletons() throws Throwable {
		getBeanFactory().preInstantiateSingletons();
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
	public Object getBean(String name) throws NoSuchBeanDefinitionException {
		return getBeanFactory().getBean(name);
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws NoSuchBeanDefinitionException {
		return getBeanFactory().getBean(requiredType);
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws NoSuchBeanDefinitionException {
		return getBeanFactory().getBean(name, requiredType);
	}

	@Override
	public <T> List<T> getBeans(Class<T> requiredType) {
		return getBeanFactory().getBeans(requiredType);
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		return getBeanFactory().isSingleton(name);
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		return getBeanFactory().isPrototype(name);
	}

	@Override
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
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
	public Map<String, BeanDefinition> getBeanDefinitionsMap() {
		return getBeanFactory().getBeanDefinitionsMap();
	}

	@Override
	public void registerBeanDefinition(String name, BeanDefinition beanDefinition) //
			throws BeanDefinitionStoreException, ConfigurationException //
	{
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

	// ----------------------

	public void setPropertiesLocation(String propertiesLocation) {
		this.propertiesLocation = propertiesLocation;
	}

}
