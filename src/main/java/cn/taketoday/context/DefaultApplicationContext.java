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
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.core.Scope;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.AbstractBeanFactory;
import cn.taketoday.context.factory.BeanPostProcessor;
import cn.taketoday.context.factory.DisposableBean;
import cn.taketoday.context.factory.FactoryBean;
import cn.taketoday.context.factory.SimpleBeanDefinitionRegistry;
import cn.taketoday.context.loader.DefaultBeanDefinitionLoader;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.StringUtils;
import lombok.NonNull;

/**
 * 
 * @author Today <br>
 *         2018-07-03 22:05:21
 */
public class DefaultApplicationContext extends AbstractBeanFactory implements ApplicationContext {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * start with given class set
	 * 
	 * @param actions
	 */
	public DefaultApplicationContext(Set<Class<?>> actions) {
		this();
		loadContext(actions);
	}

	/**
	 * start with given package
	 * 
	 * @param package_
	 */
	public DefaultApplicationContext(String package_) {
		this();
		loadContext();
	}

	/**
	 * auto load and clear cache?
	 * 
	 * @param clear
	 */
	public DefaultApplicationContext(boolean clear) {
		this();
		loadContext();
		if (clear) {
			loadSuccess();
		}
	}

	/**
	 * default constructor
	 */
	public DefaultApplicationContext() {
		log.info("Starting Application Context at [{}].",
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		
		postProcessors = new ArrayList<>();
		beanDefinitionRegistry = new SimpleBeanDefinitionRegistry();
		beanDefinitionLoader = new DefaultBeanDefinitionLoader(beanDefinitionRegistry);
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
	 *                 given package
	 */
	public void loadContext(@NonNull String package_) {
		this.loadContext("", package_);
	}

	/**
	 * 
	 * @param clazz
	 */
	public void loadContext(Set<Class<?>> clazz) {
		try {
			
			System.err.println(clazz);
			// load bean definition
			this.loadBeanDefinition(clazz);
			// add bean post processor
			this.addBeanPostProcessor();
			// handle dependency
			this.handleDependency(this.registerFactoryBean());
			System.err.println(1213);
			onRefresh();
		} //
		catch (Exception e) {
			log.error("ERROR -> [{}] caused by {}", e.getMessage(), e.getCause(), e);
		}
	}

	@Override
	public void loadContext(@NonNull String path, @NonNull String package_) {

		try {
			// load bean definition
			this.loadBeanDefinition(path, package_);
			// add bean post processor
			this.addBeanPostProcessor();
			// handle dependency
			this.handleDependency(this.registerFactoryBean());

			onRefresh();
		} //
		catch (Exception e) {
			log.error("ERROR -> [{}] caused by {}", e.getMessage(), e.getCause(), e);
		}
	}

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	protected Set<Entry<String, BeanDefinition>> registerFactoryBean() throws Exception {
		log.debug("Start register FactoryBean.");
		Set<Entry<String, BeanDefinition>> entrySet = new HashSet<>(
				beanDefinitionRegistry.getBeanDefinitionsMap().entrySet());

		for (Entry<String, BeanDefinition> entry : entrySet) {
			this.doRegisterFactoryBean(entry.getValue(), entry.getKey());
		}
		return entrySet;
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
		
		// load bean from class set
		beanDefinitionLoader.loadBeanDefinitions(beans);
	}

	/**
	 * 
	 * @param path
	 *                 properties file path
	 * @param package_
	 *                 scan package
	 * @throws IOException
	 * @throws BeanDefinitionStoreException
	 * @throws ConfigurationException
	 */
	protected void loadBeanDefinition(String path, String package_)
			throws IOException, BeanDefinitionStoreException, ConfigurationException {
		log.debug("Start loading Properties.");
		
		this.loadProperties(new File(ClassUtils.getClassLoader().getResource(path).getPath()));

		// load bean from class set
		if (StringUtils.isEmpty(package_)) {
			beanDefinitionLoader.loadBeanDefinitions(ClassUtils.getClassCache());
			return;
		}
		beanDefinitionLoader.loadBeanDefinitions(ClassUtils.scanPackage(package_));
	}

	@Override
	protected void doRegisterFactoryBean(BeanDefinition beanDefinition, String name) throws Exception {

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
			beanDefinitionRegistry.putSingleton(beanName, factoryBean.getBean());
		}

		// FactoryBean name, put factory bean instance to registry
		beanDefinitionRegistry.putSingleton(name, factoryBean_);

		// register
		beanDefinitionRegistry.registerBeanDefinition(beanName, beanDefinition_);
	}

	/**
	 * load properties file with given path
	 */
	@Override
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
				Object bean = beanDefinitionRegistry.getSingleton(name);
				if (bean instanceof DisposableBean) {
					((DisposableBean) bean).destroy();
				}
				bean = null;
			}
		} //
		catch (Exception ex) {
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
	public void onRefresh() {
		try {

			doCreateSingleton();
		} //
		catch (Exception ex) {
			log.error("Initialized ERROR -> [{}] caused by {}", ex.getMessage(), ex.getCause(), ex);
		}
	}

	@Override
	public void refresh(String name) {
		BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinitionsMap().get(name);

		try {

			beanDefinitionRegistry.putSingleton(name,
					initializingBean(createBeanInstance(beanDefinition), name, beanDefinition));
		} //
		catch (Exception e) {
			log.error("Can't refresh a bean named -> [{}]", name, e);
		}
	}

}
