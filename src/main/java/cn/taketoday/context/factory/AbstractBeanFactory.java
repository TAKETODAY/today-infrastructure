/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
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
package cn.taketoday.context.factory;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.aware.BeanClassLoaderAware;
import cn.taketoday.context.aware.BeanFactoryAware;
import cn.taketoday.context.aware.BeanNameAware;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.BeanReference;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.utils.ClassUtils;
import lombok.NonNull;

/**
 * 
 * @author Today <br>
 * 
 *         2018-06-23 11:20:58
 */
public abstract class AbstractBeanFactory implements BeanFactory {

	private final Logger				log	= LoggerFactory.getLogger(AbstractBeanFactory.class);

	/** storage BeanDefinition */
	protected BeanDefinitionRegistry	beanDefinitionRegistry;
	/** resolve beanDefinition which It is marked annotation */
	protected BeanDefinitionLoader		beanDefinitionLoader;
	/** Bean Post Processors */
	protected List<BeanPostProcessor>	postProcessors;

	@Override
	public Object getBean(String name) throws NoSuchBeanDefinitionException {

		Object bean = beanDefinitionRegistry.getSingleton(name);

		if (bean != null) {
			return bean;
		}

		try {

			return doCreateBean(beanDefinitionRegistry.getBeanDefinition(name), name);
		} //
		catch (Exception ex) {
			log.error("ERROR -> [{}] caused by [{}]", ex.getMessage(), ex.getCause(), ex);
		}
		return bean;
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws NoSuchBeanDefinitionException {
		return requiredType.cast(getBean(requiredType.getSimpleName()));
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws NoSuchBeanDefinitionException {
		return requiredType.cast(getBean(name));
	}

	/**
	 * create bean use default constructor
	 * 
	 * @param beanDefinition
	 *                       bean definition
	 * @return bean instance
	 * @throws Exception
	 */
	protected Object createBeanInstance(BeanDefinition beanDefinition) throws Exception {

		return beanDefinition.getBeanClass().getConstructor().newInstance();
	}

	/**
	 * apply property values.
	 * 
	 * @param bean
	 *                       bean instance
	 * @param propertyValues
	 *                       property list
	 * @throws Exception
	 */
	protected void applyPropertyValues(Object bean, PropertyValue[] propertyValues) throws Exception {

		for (PropertyValue propertyValue : propertyValues) {
			Field field = propertyValue.getField();
			Object value = propertyValue.getValue();
			if (value instanceof BeanReference) {
				value = this.getBean(((BeanReference) value).getName());
			}
			field.set(bean, value);
		}
	}

	/**
	 * create bean use default constructor
	 * 
	 * @param beanDefinition
	 *                       bean definition
	 * @return
	 * @throws Exception
	 */
	protected Object doCreateBean(BeanDefinition beanDefinition, String name) throws Exception {

		// bean definition's class is factory bean's class
		if (FactoryBean.class.isAssignableFrom(beanDefinition.getBeanClass())) {
			// bean definition's name is factory bean's name
			return ((FactoryBean<?>) beanDefinitionRegistry.getSingleton(beanDefinition.getName())).getBean();
		}

		// init
		Object bean = initializingBean(createBeanInstance(beanDefinition), name, beanDefinition);

		if (beanDefinition.isSingleton()) {
			log.debug("Singleton bean is being stored in the name of [{}]", name);
			beanDefinitionRegistry.putSingleton(name, bean);
		}
		return bean;
	}

	/**
	 * 
	 * set singleton bean
	 */
	protected void doCreateSingleton() throws Exception {

		log.debug("Initialization of singleton.");

		Set<String> names = beanDefinitionRegistry.getBeanDefinitionsMap().keySet();
		for (String name : names) {

			BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinitionsMap().get(name);

			if (!beanDefinition.isSingleton()) {
				continue;
			}

			// interface
			Class<?>[] interfaces = beanDefinition.getBeanClass().getInterfaces();
			for (Class<?> clazz : interfaces) {

				if (beanDefinitionRegistry.containsInstance(clazz.getSimpleName())) {
					beanDefinitionRegistry.putSingleton(name,
							beanDefinitionRegistry.getSingleton(clazz.getSimpleName()));
					continue;
				}
			}

			if (beanDefinitionRegistry.containsInstance(name)) {

				continue;// initialized
			}

			// initializing singleton bean
			Object bean = initializingBean(createBeanInstance(beanDefinition), name, beanDefinition);

			beanDefinitionRegistry.putSingleton(name, bean);
		}
		log.debug("The singleton objects is initialized.");
	}

	/**
	 * add BeanPostProcessor to pool
	 * 
	 */
	public abstract void addBeanPostProcessor();

	/**
	 * register FactoryBean.
	 * 
	 * <p>
	 * If this factory bean is prototype, register FactoryBean Definition. else
	 * register target <b>T</b> definition.
	 * </p>
	 * 
	 * @param beanDefinition
	 *                       FactoryBean definition
	 * @param name
	 *                       FactoryBean names
	 * @throws BeanDefinitionStoreException
	 * @throws Exception
	 */
	protected abstract void doRegisterFactoryBean(BeanDefinition beanDefinition, String name)
			throws BeanDefinitionStoreException, Exception;

	/**
	 * 
	 * Handle interface from dependency
	 * 
	 * @param entrySet
	 * @throws BeanDefinitionStoreException
	 */
	protected void handleDependency(Set<Entry<String, BeanDefinition>> entrySet) throws BeanDefinitionStoreException {

		Iterator<PropertyValue> iterator = beanDefinitionRegistry.getDependency().iterator();// all dependency

		while (iterator.hasNext()) {

			PropertyValue propertyValue = iterator.next();
			Class<?> propertyType = propertyValue.getField().getType();
			if (beanDefinitionRegistry.containsBeanDefinition(propertyType.getName())) {
				continue;
			}
			// handle dependency which is interface
			for (Entry<String, BeanDefinition> entry : entrySet) {
				BeanDefinition beanDefinition = entry.getValue();

				if (propertyType.isInterface() && propertyType.isAssignableFrom(beanDefinition.getBeanClass())) {
					// register bean definition

					String beanName = propertyType.getSimpleName();
					beanDefinitionRegistry.registerBeanDefinition(//
							beanName, //
							new BeanDefinition(//
									beanName, //
									beanDefinition.getBeanClass(), //
									beanDefinition.getScope(), //
									beanDefinition.getPropertyValues()//
							)//
					);
				}
			}
		}
	}

	/**
	 * initializing bean.
	 * 
	 * @param bean
	 *                       bean instance
	 * @param name
	 *                       bean name
	 * @param propertyValues
	 *                       property value
	 * @throws Exception
	 */
	protected Object initializingBean(Object bean, String name, BeanDefinition beanDefinition) throws Exception {

		log.debug("initializing bean named -> [{}].", name);

		aware(bean, name);

		// before properties
		for (BeanPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeforeInitialization(bean, beanDefinition);
		}

		// apply properties
		applyPropertyValues(bean, beanDefinition.getPropertyValues());

		if (bean instanceof InitializingBean) {
			((InitializingBean) bean).afterPropertiesSet();
		}

		// after properties
		for (BeanPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessAfterInitialization(bean, name);
		}
		return bean;
	}

	/**
	 * aware
	 * 
	 * @param bean
	 *             bean instance
	 * @param name
	 *             bean name
	 */
	protected void aware(Object bean, String name) {
		// aware
		if (bean instanceof BeanNameAware) {
			((BeanNameAware) bean).setBeanName(name);
		}
		if (bean instanceof ApplicationContextAware) {
			((ApplicationContextAware) bean).setApplicationContext((ApplicationContext) this);
		}
		if (bean instanceof BeanClassLoaderAware) {
			((BeanClassLoaderAware) bean).setBeanClassLoader(ClassUtils.getClassLoader());
		}
		if (bean instanceof BeanFactoryAware) {
			((BeanFactoryAware) bean).setBeanFactory(this);
		}
	}

	@Override
	public void removeBean(String name) throws NoSuchBeanDefinitionException {
		beanDefinitionRegistry.removeBeanDefinition(name);
	}

	@Override
	public boolean containsBean(String name) {
		return beanDefinitionRegistry.containsBeanDefinition(name);
	}

	@Override
	public boolean containsBean(Class<?> type) {
		return containsBean(type.getSimpleName());
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		return !isPrototype(name);
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(name);

		if (beanDefinition == null) {

			log.error("no such bean named -> [{}]", name);
			throw new NoSuchBeanDefinitionException("no such bean exception");
		}
		return !beanDefinition.isSingleton();
	}

	@Override
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {

		BeanDefinition type = beanDefinitionRegistry.getBeanDefinition(name);

		if (type == null) {
			log.error("no such bean exception");
			throw new NoSuchBeanDefinitionException("no such bean exception");
		}
		return type.getBeanClass();
	}

	@Override
	public Set<String> getAliases(Class<?> type) {

		Set<String> names = new HashSet<>();

		Iterator<Entry<String, BeanDefinition>> iterator = beanDefinitionRegistry.getBeanDefinitionsMap().entrySet()
				.iterator();

		while (iterator.hasNext()) {
			Map.Entry<String, BeanDefinition> entry = (Map.Entry<String, BeanDefinition>) iterator.next();
			if (entry.getValue().getBeanClass() == type) {
				names.add(entry.getKey());
			}
		}
		return names;
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
	public void registerBean(Class<?> clazz) throws BeanDefinitionStoreException, ConfigurationException {
		beanDefinitionLoader.loadBeanDefinition(clazz);
	}

	@Override
	public void registerBean(Set<Class<?>> clazz) throws BeanDefinitionStoreException, ConfigurationException {
		beanDefinitionLoader.loadBeanDefinitions(clazz);
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
	public void registerBean(String name, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException, ConfigurationException {
		beanDefinitionLoader.register(name, beanDefinition);

	}

	@Override
	public void registerSingleton(String name, @NonNull Object bean) {
		beanDefinitionRegistry.putSingleton(name, bean);
	}

}
