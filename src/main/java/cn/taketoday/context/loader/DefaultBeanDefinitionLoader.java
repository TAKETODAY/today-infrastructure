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
package cn.taketoday.context.loader;

import cn.taketoday.context.Condition;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.ComponentImpl;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.ConditionalImpl;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.exception.AnnotationException;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.factory.FactoryBean;
import cn.taketoday.context.factory.ObjectFactory;
import cn.taketoday.context.utils.ClassUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * Default Bean Definition Loader implements
 * 
 * @author Today <br>
 *         2018-06-23 11:18:22
 */
@Slf4j
public final class DefaultBeanDefinitionLoader implements BeanDefinitionLoader {

	private ObjectFactory objectFactory;
	private BeanDefinitionRegistry registry;
	/** load property */
	private PropertyValuesLoader propertyValuesLoader;
	/** bean definition registry */
	private ConfigurableApplicationContext applicationContext;

	public DefaultBeanDefinitionLoader(ConfigurableApplicationContext applicationContext, ObjectFactory objectFactory) {
		this.applicationContext = applicationContext;
		this.objectFactory = objectFactory;
		registry = applicationContext.getEnvironment().getBeanDefinitionRegistry();
		propertyValuesLoader = new PropertyValuesLoader(applicationContext, objectFactory);
		propertyValuesLoader.init();
	}

	@Override
	public BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	@Override
	public void loadBeanDefinition(Class<?> clazz) throws BeanDefinitionStoreException {

		if (clazz.isInterface()) {
			return;
		}
		try {

			if (conditional(clazz)) {
				register(clazz);
			}
		} catch (Exception e) {
			new BeanDefinitionStoreException(e.getMessage(), e);
		}
	}

	/**
	 * If matched
	 * 
	 * @param clazz
	 *            target class
	 * @return
	 * @throws Exception
	 */
	private boolean conditional(Class<?> clazz) throws Exception {
		Conditional[] conditionals = ClassUtils.getClassAnntation(clazz, Conditional.class, ConditionalImpl.class);
		if (conditionals == null || conditionals.length == 0) {
			return true;
		}
		for (Conditional conditional : conditionals) {
			for (Class<? extends Condition> conditionClass : conditional.value()) {
				Condition condition = objectFactory.create(conditionClass);
				if (!condition.matches(applicationContext, clazz)) {
					return false; // can't match
				}
			}
		}
		return true;
	}

	@Override
	public void loadBeanDefinitions(Collection<Class<?>> beans) throws BeanDefinitionStoreException {
		for (Class<?> clazz : beans) {
			loadBeanDefinition(clazz);
		}
	}

	@Override
	public void loadBeanDefinition(String name, Class<?> clazz) throws BeanDefinitionStoreException {
		// register
		register(name, new BeanDefinition(name, clazz));
	}

	/**
	 * register with given class
	 * 
	 * @param clazz
	 *            bean class
	 * @throws BeanDefinitionStoreException
	 * @throws ConfigurationException
	 */
	@Override
	public void register(Class<?> clazz) //
			throws BeanDefinitionStoreException, ConfigurationException //
	{
		Map<String, BeanDefinition> beanDefinitions = new HashMap<>();

		build(beanDefinitions, clazz);

		Iterator<Entry<String, BeanDefinition>> iterator = beanDefinitions.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, BeanDefinition> entry = iterator.next();
			register(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * register bean definition with given name, and resolve property values
	 * 
	 * @param name
	 *            bean name
	 * @param beanDefinition
	 *            definition
	 * @throws BeanDefinitionStoreException
	 */
	@Override
	public void register(String name, BeanDefinition beanDefinition) throws BeanDefinitionStoreException {

		try {

			Class<? extends Object> beanClass = beanDefinition.getBeanClass();
			// find same property.
			Collection<BeanDefinition> values = registry.getBeanDefinitionsMap().values();
			for (BeanDefinition registedDefinition : values) {
				if (registedDefinition.getBeanClass() == beanClass) {
					// have same property value.
					beanDefinition.setPropertyValues(registedDefinition.getPropertyValues());
					name = registerFactoryBean(name, beanDefinition, beanClass);
					registry.registerBeanDefinition(name, beanDefinition);
					return;
				}
			}
			// process property
			beanDefinition.setPropertyValues(processProperty(beanDefinition));
			// FactoryBean
			name = registerFactoryBean(name, beanDefinition, beanClass);
			registry.registerBeanDefinition(name, beanDefinition);
		} catch (Throwable e) {
			log.error("Process property error.", e);
		}
	}

	/**
	 * 
	 * @param factoryBeanName
	 * @param beanDefinition
	 * @param beanClass
	 * @return
	 * @throws ConfigurationException
	 */
	private String registerFactoryBean(String factoryBeanName, //
			BeanDefinition beanDefinition, Class<? extends Object> beanClass) throws ConfigurationException //
	{
		if (FactoryBean.class.isAssignableFrom(beanClass)) {
			// beanClass must be a singleton.
			if (!beanDefinition.isSingleton()) {
				throw new ConfigurationException("FactoryBean : [${}] must be a Singleton.", factoryBeanName);
			}

			String $beanName = BeanFactory.FACTORY_BEAN_PREFIX + factoryBeanName;// $name
			Object $factoryBean = applicationContext.getSingleton($beanName);
			if ($factoryBean == null) {// If not exist instance

				FactoryBean<?> $factoryBean_ = (FactoryBean<?>) objectFactory.create(beanClass);
				factoryBeanName = $factoryBean_.getBeanName();
				applicationContext.registerSingleton(BeanFactory.FACTORY_BEAN_PREFIX + factoryBeanName, $factoryBean_);// not
				// initialized
			}

			beanDefinition.setFactoryBean(true)//
					.setName(factoryBeanName);

		}
		return factoryBeanName;
	}

	/**
	 * build definition map with given class
	 * 
	 * @param map
	 *            bean definition map
	 * @param clazz
	 *            bean class
	 * @throws ConfigurationException
	 */
	private void build(Map<String, BeanDefinition> map, Class<?> clazz) throws ConfigurationException {

		try {

			Component[] components = ClassUtils.getClassAnntation(clazz, Component.class, ComponentImpl.class);
			for (Component component : components) {

				String[] names = findNames(clazz, component.value());

				for (String name : names) {

					if (!applicationContext.containsSingleton(name)) {
						map.put(name, new BeanDefinition(name, clazz, component.scope()));
					}
				}
			}
			return;
		} catch (Exception e) {
			throw new ConfigurationException("Auto load definition error");
		}
	}

	/**
	 * find names
	 * 
	 * @param clazz
	 *            bean class
	 * @param names
	 *            annotation values
	 * @return
	 */
	private String[] findNames(Class<?> clazz, String... names) {
		if (names.length == 0) {
			return new String[] { clazz.getSimpleName() };
		}
		return names;
	}

	/**
	 * process bean's property (field)
	 * 
	 * @param beanDefinition
	 *            bean definition
	 * @return property value list
	 * @throws Exception
	 */
	private PropertyValue[] processProperty(BeanDefinition beanDefinition) throws Throwable {

		Class<?> clazz = beanDefinition.getBeanClass();
		Set<PropertyValue> propertyValues = new HashSet<>();
		Field[] declaredFields = clazz.getDeclaredFields();

		// self
		try {

			for (Field field : declaredFields) {
				// supports?
				if (!propertyValuesLoader.supportsProperty(field)) {
					continue;
				}
				field.setAccessible(true);
				PropertyValue create = propertyValuesLoader.create(field);
				if (create != null) {
					propertyValues.add(create);
				}
			}
		} //
		catch (AnnotationException ex) {
			log.error("ERROR -> [{}] caused by [{}]", ex.getMessage(), ex.getCause(), ex);
		}

		// superclass
		Class<?> superclass = clazz.getSuperclass();
		if (superclass == null) {
			return propertyValues.toArray(new PropertyValue[0]);
		}

		Field[] declaredFields_ = superclass.getDeclaredFields();

		for (Field field : declaredFields_) {
			if (!propertyValuesLoader.supportsProperty(field)) {
				continue;
			}

			try {

				field.setAccessible(true);
				PropertyValue create = propertyValuesLoader.create(field);
				// not required
				if (create == null) {
					continue;
				}
				propertyValues.add(create);
			} catch (AnnotationException ex) {
				log.error("ERROR -> [{}] caused by [{}]", ex.getMessage(), ex.getCause(), ex);
			}
		}
		return propertyValues.toArray(new PropertyValue[0]);
	}

	@Override
	public BeanDefinition createBeanDefinition(Class<?> clazz) {

		BeanDefinition beanDefinition = new BeanDefinition(clazz.getSimpleName(), clazz);

		try {

			// process property
			return beanDefinition.setPropertyValues(processProperty(beanDefinition));
		} //
		catch (Throwable e) {
			log.error("process property error.", e);
		}
		return null;
	}

}
