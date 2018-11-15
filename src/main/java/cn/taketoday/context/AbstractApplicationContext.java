/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context;

import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.ComponentImpl;
import cn.taketoday.context.annotation.ContextListener;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.aware.BeanFactoryAware;
import cn.taketoday.context.aware.BeanNameAware;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.context.aware.ObjectFactoryAware;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.env.StandardEnvironment;
import cn.taketoday.context.event.BeanDefinitionLoadedEvent;
import cn.taketoday.context.event.BeanDefinitionLoadingEvent;
import cn.taketoday.context.event.BeanPostProcessorLoadingEvent;
import cn.taketoday.context.event.ContextCloseEvent;
import cn.taketoday.context.event.ContextRefreshEvent;
import cn.taketoday.context.event.ContextStartedEvent;
import cn.taketoday.context.event.HandleDependencyEvent;
import cn.taketoday.context.event.ObjectRefreshedEvent;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.factory.AbstractBeanFactory;
import cn.taketoday.context.factory.BeanPostProcessor;
import cn.taketoday.context.factory.ObjectFactory;
import cn.taketoday.context.factory.SimpleObjectFactory;
import cn.taketoday.context.listener.ApplicationListener;
import cn.taketoday.context.loader.DefaultBeanDefinitionLoader;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.NonNull;

/**
 * @author Today <br>
 * 
 *         2018-09-09 22:02
 */
public abstract class AbstractApplicationContext extends AbstractBeanFactory implements ConfigurableApplicationContext {

	private static final Logger log = LoggerFactory.getLogger(AbstractApplicationContext.class);

	/** application listeners **/
	private final Map<Class<?>, List<ApplicationListener<EventObject>>> applicationListeners = new HashMap<>(10);

	private ConfigurableEnvironment environment = new StandardEnvironment();

	public AbstractApplicationContext(String properties) {

		log.info("Starting Application Context at [{}].",
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

		postProcessors = new ArrayList<>();
		objectFactory = new SimpleObjectFactory();
		environment.setBeanDefinitionRegistry(this);

		beanDefinitionLoader = new DefaultBeanDefinitionLoader(this, objectFactory);
		environment.setBeanDefinitionLoader(beanDefinitionLoader);

		// put OBJECT_FACTORY
		registerSingleton(Constant.OBJECT_FACTORY, objectFactory);
		loadListener();

		try {

			log.debug("Start loading Properties.");

			environment.loadProperties(new File(ClassUtils.getClassLoader().getResource(properties).getPath()));
		} //
		catch (IOException ex) {
			log.error("An Exception Occurred When Loading Properties, With Msg: [{}] caused by {}", ex.getMessage(),
					ex.getCause(), ex);
		}
		// environment
		String property = environment.getProperty(Constant.KEY_ACTIVE_PROFILES);
		if (StringUtils.isEmpty(property)) {
			return;
		}
		String[] profiles = property.split(Constant.SPLIT_REGEXP);
		if (profiles == null || profiles.length == 0) {
			profiles = new String[] { property };
		}
		environment.setActiveProfiles(profiles);
	}

	@Override
	protected void aware(Object bean, String name) {
		// aware
		if (bean instanceof BeanNameAware) {
			((BeanNameAware) bean).setBeanName(name);
		}
		if (bean instanceof ApplicationContextAware) {
			((ApplicationContextAware) bean).setApplicationContext((ApplicationContext) this);
		}
		if (bean instanceof BeanFactoryAware) {
			((BeanFactoryAware) bean).setBeanFactory(this);
		}
		if (bean instanceof ObjectFactoryAware) {
			((ObjectFactoryAware) bean).setObjectFactory(objectFactory);
		}
		if (bean instanceof EnvironmentAware) {
			((EnvironmentAware) bean).setEnvironment(environment);
		}
	}

	@Override
	public ObjectFactory getObjectFactory() {
		return objectFactory;
	}

	@Override
	public void setObjectFactory(ObjectFactory objectFactory) {
		super.objectFactory = objectFactory;
	}

	/**
	 * 
	 * load all the properties file and class in class path
	 */
	public void loadContext() {
		this.loadContext("");
	}

	/**
	 * load all the properties file and class with given package in class path
	 * 
	 * @param package_
	 *            given package
	 */
	public abstract void loadContext(@NonNull String package_);

	/**
	 * load all the application listeners in context.
	 */
	private void loadListener() {

		log.debug("Loading Application Listeners.");

		ClassUtils.getImplClasses(ApplicationListener.class)//
				.stream()//
				.forEach(this::forEach);
		// sort
		for (Entry<Class<?>, List<ApplicationListener<EventObject>>> entry : applicationListeners.entrySet()) {
			entry.getValue().sort(Comparator.comparingInt(OrderUtils::getOrder).reversed());
		}
	}

	/**
	 * 
	 * @param clazz
	 */
	private void forEach(Class<?> clazz) {

		// FIXME sometimes listener count not correct
		try {

			ContextListener contextListener = clazz.getAnnotation(ContextListener.class);
			if (contextListener == null) {
				return;
			}

			Component[] components = ClassUtils.getClassAnntation(clazz, Component.class, ComponentImpl.class);
			String name = clazz.getSimpleName();
			if (components != null && components.length != 0) {
				// bean name
				name = components[0].value()[0];
			}

			// if exist bean
			Object applicationListener = getSingleton(name);

			if (applicationListener == null) {
				// create bean instance
				applicationListener = objectFactory.create(clazz);
				registerSingleton(name, applicationListener);
			}

			Method[] declaredMethods = clazz.getDeclaredMethods();
			for (Method method : declaredMethods) {
				// onApplicationEvent
				if (method.getName().equals(Constant.ON_APPLICATION_EVENT)) {
					if (method.isBridge()) {
						continue;
					}
					// register listener
					this.registerListener(applicationListener, method.getParameterTypes()[0]);
				}
			}
			// specify type
			Class<?>[] classes = contextListener.value();
			for (Class<?> class_ : classes) {
				// register listener
				this.registerListener(applicationListener, class_);
			}
		} //
		catch (Exception ex) {
			log.error("An Exception Occurred When Register Application Listener, With Msg: [{}] caused by {}", //
					ex.getMessage(), ex.getCause(), ex);
		}
	}

	/**
	 * 
	 * @param applicationListener
	 *            the instance of application listener
	 * @param eventType
	 *            the event class
	 */
	@SuppressWarnings({ "unchecked", "serial" })
	private void registerListener(Object applicationListener, Class<?> eventType) {
		if (applicationListeners.containsKey(eventType)) {
			applicationListeners.get(eventType).add((ApplicationListener<EventObject>) applicationListener);
			return;
		}
		applicationListeners.put(eventType, new ArrayList<ApplicationListener<EventObject>>() {
			{
				add((ApplicationListener<EventObject>) applicationListener);
			}
		});
	}

	/**
	 * 
	 * @param clazz
	 */
	public void loadContext(Set<Class<?>> clazz) {

		try {
			// load bean definition
			this.loadBeanDefinition(clazz);
			// handle dependency
			publishEvent(new HandleDependencyEvent(this));
			super.handleDependency();
			// add bean post processor
			publishEvent(new BeanPostProcessorLoadingEvent(this));
			this.addBeanPostProcessor();

			onRefresh();
		} //
		catch (Exception ex) {
			log.error("An Exception Occurred When Loading Context, With Msg: [{}] caused by {}", ex.getMessage(),
					ex.getCause(), ex);
		}
	}

	/**
	 * 
	 * @param beans
	 * @throws IOException
	 * @throws BeanDefinitionStoreException
	 * @throws ConfigurationException
	 */
	protected void loadBeanDefinition(Set<Class<?>> beans)//
			throws IOException, BeanDefinitionStoreException, ConfigurationException //
	{
		log.debug("Start loading BeanDefinitions.");

		// load bean from class set
		publishEvent(new BeanDefinitionLoadingEvent(this));
		beanDefinitionLoader.loadBeanDefinitions(beans);
		publishEvent(new BeanDefinitionLoadedEvent(this));
	}

	@Override
	public void addBeanPostProcessor() {

		log.debug("Start loading BeanPostProcessor.");
		Set<Entry<String, BeanDefinition>> entrySet = getBeanDefinitionsMap().entrySet();
		try {

			for (Entry<String, BeanDefinition> entry : entrySet) {
				BeanDefinition beanDefinition = entry.getValue();
				if (!BeanPostProcessor.class.isAssignableFrom(beanDefinition.getBeanClass())) {
					continue;
				}
				log.debug("Find a BeanPostProcessor: [{}]", beanDefinition.getBeanClass());
				postProcessors.add((BeanPostProcessor) initializeSingleton(entry.getKey(), beanDefinition));
			}
			postProcessors.sort(Comparator.comparingInt(OrderUtils::getOrder).reversed());
		} //
		catch (Exception ex) {
			log.error("An Exception Occurred When Adding Post Processor To Context: [{}] With Msg: [{}] caused by {}",
					this, ex.getMessage(), ex.getCause(), ex);
		}
	}

	/**
	 * 
	 * @param path
	 *            properties file path
	 * @param package_
	 *            scan package
	 * @throws IOException
	 * @throws BeanDefinitionStoreException
	 * @throws ConfigurationException
	 */
	protected void loadBeanDefinition(String package_)//
			throws IOException, BeanDefinitionStoreException, ConfigurationException //
	{
		log.debug("Start loading BeanDefinitions.");
		publishEvent(new BeanDefinitionLoadingEvent(this));
		// load bean from class set
		beanDefinitionLoader.loadBeanDefinitions(ClassUtils.scanPackage(package_));
		publishEvent(new BeanDefinitionLoadedEvent(this));
	}

	@Override
	public void loadSuccess() {
		ClassUtils.clearCache();
	}

	@Override
	public void onRefresh() {

		try {

			log.debug("Initialization of singleton objects.");
			publishEvent(new ContextRefreshEvent(this));
			Set<Entry<String, BeanDefinition>> entrySet = getBeanDefinitionsMap().entrySet();
			for (Entry<String, BeanDefinition> entry : entrySet) {
				doCreateSingleton(entry, entrySet);
			}
			log.debug("The singleton objects is initialized.");
			publishEvent(new ContextStartedEvent(this));
		} //
		catch (Exception ex) {
			log.error("An Exception Occurred When Refresh Context: [{}] With Msg: [{}] caused by {}", this,
					ex.getMessage(), ex.getCause(), ex);
		}
	}

	@Override
	public void refresh(String name) {

		try {

			BeanDefinition beanDefinition = getBeanDefinition(name);

			registerSingleton(//
					name, initializingBean(//
							createBeanInstance(beanDefinition), name, beanDefinition//
					)//
			);
			publishEvent(new ObjectRefreshedEvent(beanDefinition, this));
		} //
		catch (NoSuchBeanDefinitionException e1) {
			log.error("ERROR MSG: [{}]", e1.getMessage(), e1);
		} //
		catch (Exception e) {
			log.error("Can't refresh a bean named: [{}]", name, e);
		}
	}

	@Override
	public Object refresh(BeanDefinition beanDefinition) {

		String name = beanDefinition.getName();

		try {

			Object initializingBean = initializingBean(createBeanInstance(beanDefinition), name, beanDefinition);

			publishEvent(new ObjectRefreshedEvent(beanDefinition, this));

			return initializingBean;
		} //
		catch (NoSuchBeanDefinitionException e1) {
			log.error("ERROR MSG -> [{}]", e1.getMessage(), e1);
		} //
		catch (Exception e) {
			log.error("Can't refresh a bean named: [{}]", name);
		}
		return null;
	}

	@Override
	public void close() {
		publishEvent(new ContextCloseEvent(this));
	}

	@Override
	public void publishEvent(EventObject event) {

		log.info("Publish event: [{}]", event.getClass().getName());

		List<ApplicationListener<EventObject>> collection = applicationListeners.get(event.getClass());
		if (collection == null) {
			return;
		}

		for (ApplicationListener<EventObject> applicationListener : collection) {
			applicationListener.onApplicationEvent(event);
		}
	}

	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	@Override
	public ConfigurableEnvironment getEnvironment() {
		return environment;
	}

}
