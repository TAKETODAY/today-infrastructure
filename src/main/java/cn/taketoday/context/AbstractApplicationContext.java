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
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.event.BeanDefinitionLoadedEvent;
import cn.taketoday.context.event.BeanDefinitionLoadingEvent;
import cn.taketoday.context.event.ContextCloseEvent;
import cn.taketoday.context.event.ContextRefreshEvent;
import cn.taketoday.context.event.ContextStartedEvent;
import cn.taketoday.context.event.ObjectRefreshedEvent;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.factory.AbstractBeanFactory;
import cn.taketoday.context.factory.BeanPostProcessor;
import cn.taketoday.context.factory.ObjectFactory;
import cn.taketoday.context.factory.SimpleBeanDefinitionRegistry;
import cn.taketoday.context.factory.SimpleObjectFactory;
import cn.taketoday.context.listener.ApplicationListener;
import cn.taketoday.context.loader.DefaultBeanDefinitionLoader;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
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
public abstract class AbstractApplicationContext extends AbstractBeanFactory implements ApplicationContext {

	final Logger log = LoggerFactory.getLogger(getClass());
	/** application listeners **/
	final Map<Class<?>, Collection<ApplicationListener<EventObject>>> applicationListeners = new HashMap<>();

	public AbstractApplicationContext() {

		log.info("Starting Application Context at [{}].",
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

		postProcessors = new ArrayList<>();
		objectFactory = new SimpleObjectFactory();
		beanDefinitionRegistry = new SimpleBeanDefinitionRegistry();
		beanDefinitionLoader = new DefaultBeanDefinitionLoader(beanDefinitionRegistry, objectFactory);

		// put OBJECT_FACTORY
		beanDefinitionRegistry.putSingleton(Constant.OBJECT_FACTORY, objectFactory);

		loadListener();
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
	public void loadContext(@NonNull String package_) {
		this.loadContext("", package_);
	}

	public abstract void loadContext(@NonNull String path, @NonNull String package_);

	/**
	 * load all the application listeners in context.
	 * 
	 */
	private void loadListener() {

		log.debug("Loading Application Listeners.");

		ClassUtils.getImplClasses(ApplicationListener.class)//
				.parallelStream()//
				.forEach(this::forEach);
	}

	/**
	 * 
	 * @param clazz
	 */
	private void forEach(Class<?> clazz) {

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
			Object applicationListener = beanDefinitionRegistry.getSingleton(name);

			if (applicationListener == null) {
				// create bean instance
				applicationListener = objectFactory.create(clazz);
				beanDefinitionRegistry.putSingleton(name, applicationListener);
			}

			Method[] declaredMethods = clazz.getMethods();
			for (Method method : declaredMethods) {
				// onApplicationEvent
				if (method.getName().equals(Constant.ON_APPLICATION_EVENT)) {
					if (method.isBridge()) {
//						log.debug("Bridge Method : {}", method);
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
				if (class_ != EventObject.class) {
					this.registerListener(applicationListener, class_);
				}
			}
		} //
		catch (Exception e) {
			log.error("Application Listener Register Error.", e);
		}
	}

	/**
	 * 
	 * @param applicationListener
	 *            the instance of application listener
	 * @param eventType
	 *            the event class
	 */
	@SuppressWarnings("unchecked")
	private void registerListener(Object applicationListener, Class<?> eventType) {

		if (applicationListeners.containsKey(eventType)) {
			applicationListeners.get(eventType).add((ApplicationListener<EventObject>) applicationListener);
			return;
		}
		applicationListeners.put(eventType, new ArrayList<ApplicationListener<EventObject>>() {
			private static final long serialVersionUID = 1;
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
			// add bean post processor
			this.addBeanPostProcessor();

			// handle dependency
			super.handleDependency(beanDefinitionRegistry.getBeanDefinitionsMap().entrySet());

			onRefresh();
		} //
		catch (Exception e) {
			log.error("ERROR -> [{}] caused by {}", e.getMessage(), e.getCause(), e);
		}
	}

	/**
	 * load properties file with given path
	 */
	@Override
	public void loadProperties(File dir) throws IOException {

		File[] listFiles = dir
				.listFiles(file -> (file.isDirectory()) || (file.getName().endsWith(Constant.PROPERTIES_SUFFIX)));

		if (listFiles == null) {
			return;
		}
		for (File file : listFiles) {
			if (file.isDirectory()) { // recursive
				loadProperties(file);
				continue;
			}
			InputStream inputStream = null;
			try {

				inputStream = new FileInputStream(file);
				beanDefinitionRegistry.getProperties().load(inputStream);
			} finally {
				if (inputStream != null) {
					inputStream.close();
				}
			}
		}
	}

	/**
	 * 
	 * @param beans
	 * @throws IOException
	 * @throws BeanDefinitionStoreException
	 * @throws ConfigurationException
	 */
	protected void loadBeanDefinition(Set<Class<?>> beans)
			throws IOException, BeanDefinitionStoreException, ConfigurationException {
		log.debug("Start loading Properties.");

		this.loadProperties(new File(ClassUtils.getClassLoader().getResource("").getPath()));

		log.debug("Start loading BeanDefinitions.");
		// load bean from class set
		publishEvent(new BeanDefinitionLoadingEvent(this));
		beanDefinitionLoader.loadBeanDefinitions(beans);
		publishEvent(new BeanDefinitionLoadedEvent(this));
	}

	@Override
	public void addBeanPostProcessor() {

		log.debug("Start loading BeanPostProcessor.");

		Set<Entry<String, BeanDefinition>> entrySet = new HashSet<>(
				beanDefinitionRegistry.getBeanDefinitionsMap().entrySet());

		try {
			
			for (Entry<String, BeanDefinition> entry : entrySet) {
				BeanDefinition beanDefinition = entry.getValue();
				if (!BeanPostProcessor.class.isAssignableFrom(beanDefinition.getBeanClass())) {
					continue;
				}
				log.debug("Find a BeanPostProcessor: [{}]", beanDefinition.getBeanClass());
				
				postProcessors.add((BeanPostProcessor) initializingBean(//
						createBeanInstance(beanDefinition), entry.getKey(), beanDefinition)//
				);
			}
		} //
		catch (Exception e) {
			log.error("ERROR : [{}] caused by {}", e.getMessage(), e.getCause(), e);
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
	protected void loadBeanDefinition(String path, String package_)
			throws IOException, BeanDefinitionStoreException, ConfigurationException {

		log.debug("Start loading Properties.");

		this.loadProperties(new File(ClassUtils.getClassLoader().getResource(path).getPath()));

		log.debug("Start loading BeanDefinitions.");
		publishEvent(new BeanDefinitionLoadingEvent(this));
		// load bean from class set
		if (StringUtils.isEmpty(package_)) {
			beanDefinitionLoader.loadBeanDefinitions(ClassUtils.getClassCache());
		} else {
			beanDefinitionLoader.loadBeanDefinitions(ClassUtils.scanPackage(package_));
		}
		publishEvent(new BeanDefinitionLoadedEvent(this));
	}

	@Override
	public void loadSuccess() {
		ClassUtils.clearCache();
	}

	@Override
	public void onRefresh() {

		try {

			publishEvent(new ContextRefreshEvent(this));
			log.debug("Initialization of singleton.");
			Set<Entry<String, BeanDefinition>> entrySet = beanDefinitionRegistry.getBeanDefinitionsMap().entrySet();
			for (Entry<String, BeanDefinition> entry : entrySet) {
				doCreateSingleton(entry, entrySet);
			}
			log.debug("The singleton objects is initialized.");
			publishEvent(new ContextStartedEvent(this));
		} //
		catch (Exception ex) {
			log.error("Initialized ERROR -> [{}] caused by {}", ex.getMessage(), ex.getCause(), ex);
		}
	}

	@Override
	public void refresh(String name) {

		try {

			BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(name);

			beanDefinitionRegistry.putSingleton(//
					name, initializingBean(//
							createBeanInstance(beanDefinition), name, beanDefinition//
					)//
			);
			publishEvent(new ObjectRefreshedEvent(beanDefinition, this));
		} //
		catch (NoSuchBeanDefinitionException e1) {
			log.error("ERROR MSG -> [{}]", e1.getMessage(), e1);
		} //
		catch (Exception e) {
			log.error("Can't refresh a bean named -> [{}]", name, e);
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

		Collection<ApplicationListener<EventObject>> collection = applicationListeners.get(event.getClass());
		if (collection == null) {
			return;
		}
		
		for (ApplicationListener<EventObject> applicationListener : collection) {
			applicationListener.onApplicationEvent(event);
		}
	}

}
