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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.core.Scope;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.AbstractBeanFactory;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.factory.BeanPostProcessor;
import cn.taketoday.context.factory.DisposableBean;
import cn.taketoday.context.factory.FactoryBean;
import cn.taketoday.context.factory.SimpleBeanDefinitionRegistry;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.loader.DefaultBeanDefinitionLoader;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.StringUtils;

/**
 * @author Today
 * @date 2018年7月3日 下午10:05:21
 */
public class ClassPathApplicationContext extends AbstractBeanFactory implements ApplicationContext {

	protected final Logger log = LoggerFactory.getLogger(ClassPathApplicationContext.class);

	/**
	 * start with given class set
	 * 
	 * @param actions
	 */
	public ClassPathApplicationContext(Set<Class<?>> actions) {
		this();
		loadContext();
	}

	/**
	 * start with given package
	 * 
	 * @param package_
	 */
	public ClassPathApplicationContext(String package_) {
		this();
		loadContext();
	}

	/**
	 * auto load and clear cache?
	 * 
	 * @param clear
	 */
	public ClassPathApplicationContext(boolean clear) {
		this();
		loadContext();
		if (clear) {
			loadSuccess();
		}
	}

	/**
	 * default constructor
	 */
	public ClassPathApplicationContext() {
		log.info("Starting Application Context at [{}].",
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

		postProcessors = new ArrayList<>();
		beanDefinitionRegistry = new SimpleBeanDefinitionRegistry();
		beanDefinitionLoader = new DefaultBeanDefinitionLoader(beanDefinitionRegistry);
	}

	/**
	 * all class in application class path
	 * 
	 * @return
	 */
	private Set<Class<?>> getAllClass() {
		return ClassUtils.getClassCache();
	}

	/**
	 * 
	 * load all the properties file and class in class path
	 */
	public void loadContext() {
		this.loadContext("");
	}

	/**
	 * load all the properties file and given package in class path
	 * 
	 * @param package_
	 *            given package
	 */
	public void loadContext(String package_) {
		this.loadContext("", package_);
	}

	/**
	 * 
	 * load properties file with given path
	 */
	@Override
	public void loadContext(String path, String package_) {

		try {

			log.debug("Start loading Properties.");

			this.loadProperties(new File(ClassUtils.getClassLoader().getResource(path).getPath()));
			// load bean from class set
			if (StringUtils.isEmpty(package_)) {
				beanDefinitionLoader.loadBeanDefinitions(getAllClass());
			} else {
				Set<Class<?>> scanPackage = ClassUtils.scanPackage(package_);
				beanDefinitionLoader.loadBeanDefinitions(scanPackage);
			}

			addBeanPostProcessor();

			log.debug("Start register FactoryBean.");

			Set<Entry<String, BeanDefinition>> entrySet = new HashSet<>(
					beanDefinitionRegistry.getBeanDefinitionsMap().entrySet());

			for (Entry<String, BeanDefinition> entry : entrySet) {
				this.registerFactoryBean(entry.getValue(), entry.getKey());
			}
			// handle dependency
			Set<PropertyValue> dependency = beanDefinitionRegistry.getDependency();
			Iterator<PropertyValue> iterator = dependency.iterator();
			while (iterator.hasNext()) {
				PropertyValue propertyValue = iterator.next();
				Class<?> propertyType = propertyValue.getField().getType();
				if (beanDefinitionRegistry.containsBeanDefinition(propertyType.getName())) {
					continue;
				}
				for (Entry<String, BeanDefinition> entry : entrySet) {
					BeanDefinition beanDefinition = entry.getValue();
					if (propertyType.isAssignableFrom(beanDefinition.getBeanClass())) {
						beanDefinitionRegistry.registerBeanDefinition(propertyType.getName(), beanDefinition);
					}
				}
			}
			log.debug("Start init singleton.");
			// init singleton bean
			doCreateSingleton();
			log.debug("init singleton success.");
		} catch (Exception e) {
			log.error("ERROR -> [{}] caused by {}", e.getMessage(), e.getCause(), e);
		}
	}

	@Override
	protected void registerFactoryBean(BeanDefinition beanDefinition, String name) throws Exception {

		Class<? extends Object> beanClass = beanDefinition.getBeanClass();
		if (!FactoryBean.class.isAssignableFrom(beanClass)) {
			return;
		}

		if (!beanDefinition.isSingleton()) {
			throw new ConfigurationException("FactoryBean -> [{}] must be a Singleton.", name);
		}

		log.debug("register FactoryBean -> [{}].", beanClass);
		// initialize once, Singleton
		Object factoryBean_ = initializingBean(createBeanInstance(beanDefinition), name, beanDefinition);

		FactoryBean<?> factoryBean = (FactoryBean<?>) factoryBean_;

		BeanDefinition beanDefinition_ = new BeanDefinition();
		beanDefinition_.setBeanClass(beanClass);// FactoryBean class
		beanDefinition_.setName(name);// FactoryBean name, when get instance use this name

		String beanName = factoryBean.getBeanName(); // bean's real name

		if (!factoryBean.isSingleton()) { // PROTOTYPE
			beanDefinition_.setScope(Scope.PROTOTYPE);
			log.debug("FactoryBean -> [{}] is PROTOTYPE.", beanName);
		} else { // initialized
			beanDefinitionRegistry.putInstance(beanName, factoryBean.getBean());
		}

		beanDefinitionRegistry.putInstance(name, factoryBean_);// FactoryBean name

		// register
		beanDefinitionRegistry.registerBeanDefinition(beanName, beanDefinition_);
	}

	/**
	 * load properties file with given path
	 */
	public void loadProperties(File dir) throws IOException {

		File[] listFiles = dir.listFiles(file -> (file.isDirectory()) || (file.getName().endsWith(".properties")));

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
				log.debug("Find a BeanPostProcessor -> [{}]", beanDefinition.getBeanClass());

				postProcessors.add((BeanPostProcessor) initializingBean(createBeanInstance(beanDefinition),
						entry.getKey(), beanDefinition));

			}
		} catch (Exception e) {
			log.error("ERROR -> [{}] caused by {}", e.getMessage(), e.getCause(), e);
		}
	}

	@Override
	public void close() {

		log.info("Closing -> [{}] at [{}].", this, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

		Set<String> names = beanDefinitionRegistry.getBeanDefinitionNames();

		try {
			for (String name : names) {
				Object bean = beanDefinitionRegistry.getInstance(name);
				if (bean instanceof DisposableBean) {
					((DisposableBean) bean).destroy();
				}
				bean = null;
			}
		} catch (Exception ex) {
			log.error("Initialized ERROR -> [{}] caused by {}", ex.getMessage(), ex.getCause(), ex);
		}
		beanDefinitionRegistry.getProperties().clear();
		beanDefinitionRegistry.getBeanDefinitionsMap().clear();
	}

	
	@Override
	public void loadSuccess() {
		ClassUtils.clearCache();
	}

	@Override
	public void setBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) {
		this.beanDefinitionRegistry.getBeanDefinitionsMap().putAll(beanDefinitionRegistry.getBeanDefinitionsMap());
	}

	@Override
	public BeanDefinitionRegistry getBeanDefinitionRegistry() {
		return beanDefinitionRegistry;
	}

	@Override
	public void registerBean(Class<?> clazz) throws BeanDefinitionStoreException {
		beanDefinitionLoader.loadBeanDefinition(clazz);
		// setBeanDefinitionRegistry(beanDefinitionLoader.getRegistry());
	}

	@Override
	public void registerBean(Set<Class<?>> clazz) throws BeanDefinitionStoreException {
		beanDefinitionLoader.loadBeanDefinitions(clazz);
		// setBeanDefinitionRegistry(beanDefinitionLoader.getRegistry());
	}

	@Override
	public BeanDefinitionLoader getBeanDefinitionLoader() {
		return beanDefinitionLoader;
	}

	@Override
	public void registerBean(String name, Class<?> clazz) throws BeanDefinitionStoreException {
		beanDefinitionLoader.loadBeanDefinition(name, clazz);
	}

	@Override
	public void onRefresh() {
		try {
			super.doCreateSingleton();
		} catch (Exception ex) {
			log.error("Initialized ERROR -> [{}] caused by {}", ex.getMessage(), ex.getCause(), ex);
		}
	}
	
}
