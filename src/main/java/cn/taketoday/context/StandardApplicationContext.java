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
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.context.utils.PropertiesUtils;
import cn.taketoday.context.utils.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import lombok.NonNull;

/**
 * @author Today <br>
 * 
 *         2018-09-06 13:47
 */
public class StandardApplicationContext extends DefaultApplicationContext implements ApplicationContext {

	private static final Map<String, Method> PROTOTYPES = new HashMap<>();

	/**
	 * start with given class set
	 * 
	 * @param actions
	 */
	public StandardApplicationContext(Set<Class<?>> actions) {
		super(actions);
	}

	/**
	 * start with given package
	 * 
	 * @param package_
	 */
	public StandardApplicationContext(String package_) {
		super(package_);
	}

	/**
	 * auto load and clear cache?
	 * 
	 * @param clear
	 */
	public StandardApplicationContext() {
		super();
	}

	public StandardApplicationContext(boolean clear) {
		super(clear);
	}

	@Override
	public final Object getBean(String name) throws NoSuchBeanDefinitionException {

		Object bean = beanDefinitionRegistry.getSingleton(name);

		if (bean != null) {
			return bean;
		}

		BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(name);
		
		if (beanDefinition == null) {
			log.warn("No such bean definition named : [{}].", name);
			return null;
		}
		
		try {

			Method method = PROTOTYPES.get(name);
			
			if (method != null) {
				return method.invoke(beanDefinitionRegistry.getSingleton(beanDefinition.getName()));
			}
			
			if (beanDefinition.isSingleton()) {
				return doCreateBean(beanDefinition, name);
			}
			// prototype
			return doCreatePrototype(beanDefinition, name);
		} //
		catch (Exception ex) {
			log.error("ERROR -> [{}] caused by [{}]", ex.getMessage(), ex.getCause(), ex);
		}
		return bean;
	}

	@Override
	public void loadContext(@NonNull String path, @NonNull String package_) {

		try {
			// load bean definition
			super.loadBeanDefinition(path, package_);
			// add bean post processor
			super.addBeanPostProcessor();
			Map<String, BeanDefinition> beanDefinitionsMap = beanDefinitionRegistry.getBeanDefinitionsMap();

			this.configuration(beanDefinitionsMap.entrySet());
			// handle dependency
			super.handleDependency(beanDefinitionsMap.entrySet());

			onRefresh();
		} //
		catch (Exception e) {
			log.error("ERROR -> [{}] caused by {}", e.getMessage(), e.getCause(), e);
		}
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
			super.addBeanPostProcessor();

			Map<String, BeanDefinition> beanDefinitionsMap = beanDefinitionRegistry.getBeanDefinitionsMap();

			this.configuration(beanDefinitionsMap.entrySet());
			// handle dependency
			super.handleDependency(beanDefinitionsMap.entrySet());

			onRefresh();
		} //
		catch (Exception e) {
			log.error("ERROR -> [{}] caused by {}", e.getMessage(), e.getCause(), e);
		}
	}

	/**
	 * register
	 * 
	 * @param beanDefinitions
	 * @throws Exception
	 */
	private void configuration(Set<Entry<String, BeanDefinition>> beanDefinitions) throws Exception {

		for (Entry<String, BeanDefinition> beanDefinition_ : beanDefinitions) {

			BeanDefinition beanDefinition = beanDefinition_.getValue();

			Class<? extends Object> beanClass = beanDefinition.getBeanClass();

			if (!beanClass.isAnnotationPresent(Configuration.class)) {
				continue;
			}
			// Props
			if (beanClass.isAnnotationPresent(Props.class)) {
				properties(beanDefinition, beanClass);
			}

			Method[] declaredMethods = beanClass.getDeclaredMethods();

			for (Method method : declaredMethods) {

				Component[] components = ClassUtils.getMethodAnntation(method, Component.class, ComponentImpl.class);

				if (components.length == 0) {
					continue;
				}

				create(method, components);
			}
		}
	}

	/**
	 * Properties injection
	 * 
	 * @param beanDefinition
	 * @param beanClass
	 * @throws ConfigurationException
	 */
	private void properties(BeanDefinition beanDefinition, Class<? extends Object> beanClass)
			throws ConfigurationException {

		Props props = beanClass.getAnnotation(Props.class);

		List<PropertyValue> propertyValues = new ArrayList<>();

		Properties properties = beanDefinitionRegistry.getProperties();
		String[] prefixs = props.prefix();
		Field[] declaredFields = beanClass.getDeclaredFields();
		for (String prefix : prefixs) {

			for (Field declaredField : declaredFields) {

				String key = prefix + declaredField.getName();

				String value = properties.getProperty(key);
				if (value == null) {
					continue;
				}
				declaredField.setAccessible(true);
				propertyValues.add(//
						new PropertyValue(//
								ConvertUtils.convert(//
										PropertiesUtils.findInProperties(properties, value), declaredField.getType()//
								), declaredField//
						)//
				);
			}
		}
		beanDefinition.addPropertyValue(propertyValues);
	}

	/**
	 * 
	 * @param method
	 * @param components
	 * @throws BeanDefinitionStoreException
	 */
	private void create(Method method, Component[] components) throws BeanDefinitionStoreException {
		for (Component component : components) {
			String[] names = component.value();
			Scope scope = component.scope();

			if (names.length == 0) {
				names = new String[] { method.getName() };
			}
			for (String name : names) {

				if (StringUtils.isEmpty(name)) {
					name = method.getName();
				}
				
				PROTOTYPES.put(name, method);
				
				BeanDefinition beanDefinition_ = new BeanDefinition()//
						.setScope(scope)//
						.setBeanClass(method.getReturnType())//
						.setName(method.getDeclaringClass().getSimpleName());
				beanDefinitionLoader.register(name, beanDefinition_);
			}
		}
	}

	@Override
	protected void doCreateSingleton(Entry<String, BeanDefinition> entry, Set<Entry<String, BeanDefinition>> entrySet)
			throws Exception {

		String name = entry.getKey();
		BeanDefinition beanDefinition = entry.getValue();

		// remove singleton
		if (PROTOTYPES.containsKey(name)) {
			Method method = PROTOTYPES.get(name);

			try {
				
				beanDefinitionRegistry.putSingleton(name, //
						initializingBean(//
								method.invoke(//
										createDeclaringObject(name, beanDefinition.getName())//
								), name, beanDefinition//
						)//
				);

			} catch (InvocationTargetException e) {
				log.error("Error with creating bean named -> [{}]", name, e);
			}
			PROTOTYPES.remove(name);
			return;
		}
		super.doCreateSingleton(entry, entrySet);
	}

	/**
	 * 
	 * @param name
	 * @param beanDefinition
	 * @param declaringName
	 * @return
	 * @throws Exception
	 */
	private Object createDeclaringObject(String name, String declaringName) throws Exception {

		if (beanDefinitionRegistry.containsSingleton(declaringName)) {

			return beanDefinitionRegistry.getSingleton(declaringName);
		}
		BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(declaringName);
		// declaring bean not initialized
		Object declaringSingleton = super.initializingBean(//
				createBeanInstance(//
						beanDefinition//
				), //
				name, //
				beanDefinition);
		// put declaring object
		beanDefinitionRegistry.putSingleton(name, declaringSingleton);

		return declaringSingleton;
	}

}
