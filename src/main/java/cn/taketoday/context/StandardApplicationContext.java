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
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.ConditionalImpl;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.event.BeanPostProcessorLoadingEvent;
import cn.taketoday.context.event.HandleDependencyEvent;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.NonNull;

/**
 * @author Today <br>
 * 
 *         2018-09-06 13:47
 */
public class StandardApplicationContext extends AbstractApplicationContext implements ApplicationContext {

	private static final Logger log = LoggerFactory.getLogger(StandardApplicationContext.class);

	private final Map<String, Method> prototypes = new HashMap<>(8);

	/**
	 * start with given class set
	 * 
	 * @param actions
	 */
	public StandardApplicationContext(Set<Class<?>> actions) {
		this();
		loadContext(actions);
	}

	public StandardApplicationContext(String path) {
		super(path);
	}

	public StandardApplicationContext(String path, String package_) {
		super(path);
		loadContext(package_);
	}

	/**
	 * auto load and clear cache?
	 * 
	 * @param clear
	 */
	public StandardApplicationContext() {
		super("");
	}

	public StandardApplicationContext(boolean clear) {
		this();
		loadContext();
		if (clear) {
			loadSuccess();
		}
	}

	@Override
	public final Object getBean(String name) throws NoSuchBeanDefinitionException {

		Object bean = getSingleton(name);

		if (bean != null) {
			return bean;
		}

		BeanDefinition beanDefinition = getBeanDefinition(name);

		if (beanDefinition == null) {
			log.warn("No such bean definition named: [{}].", name);
			return null;
		}

		try {

			if (beanDefinition.isSingleton()) {
				return doCreateBean(beanDefinition, name);
			}

			Method method = prototypes.get(name);

			if (method != null) {
				return method.invoke(getSingleton(beanDefinition.getName()));
			}

			// prototype
			return doCreatePrototype(beanDefinition, name);
		} //
		catch (Exception ex) {
			log.error("An Exception Occurred When Getting A Bean Named: [{}], With Msg: [{}] caused by {}", //
					name, ex.getMessage(), ex.getCause(), ex);
		}
		return bean;
	}

	@Override
	public void loadContext(@NonNull String package_) {

		try {
			// load bean definition
			super.loadBeanDefinition(package_);

			this.configuration();
			// handle dependency
			publishEvent(new HandleDependencyEvent(this));
			super.handleDependency();
			// add bean post processor
			publishEvent(new BeanPostProcessorLoadingEvent(this));
			super.addBeanPostProcessor();
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
			this.configuration();
			// handle dependency
			publishEvent(new HandleDependencyEvent(this));
			super.handleDependency();
			// add bean post processor
			publishEvent(new BeanPostProcessorLoadingEvent(this));
			super.addBeanPostProcessor();
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
	private void configuration() throws Exception {

		Map<String, BeanDefinition> beanDefinitionsMap = getBeanDefinitionsMap();

		for (Entry<String, BeanDefinition> beanDefinition_ : beanDefinitionsMap.entrySet()) {

			BeanDefinition beanDefinition = beanDefinition_.getValue();

			Class<? extends Object> beanClass = beanDefinition.getBeanClass();
			//
			if (!beanClass.isAnnotationPresent(Configuration.class)) {
				continue;
			}

			// Props
			if (beanClass.isAnnotationPresent(Props.class)) {
				properties(beanDefinition, beanClass);
			}

			Method[] declaredMethods = beanClass.getDeclaredMethods();

			for (Method method : declaredMethods) {
				if (!conditional(method)) {
					continue;
				}
				Component[] components = ClassUtils.getMethodAnntation(method, Component.class, ComponentImpl.class);

				if (components.length == 0) {
					continue;
				}
				create(method, components);
			}
		}
	}

	/**
	 * 
	 * @param method
	 * @return
	 * @throws Exception
	 */
	private boolean conditional(Method method) throws Exception {
		Conditional[] conditionals = ClassUtils.getMethodAnntation(method, Conditional.class, ConditionalImpl.class);
		if (conditionals == null || conditionals.length == 0) {
			return true;
		}
		for (Conditional conditional : conditionals) {
			for (Class<? extends Condition> conditionClass : conditional.value()) {
				Condition condition = objectFactory.create(conditionClass);
				if (!condition.matches(this, method)) {
					return false; // can't match
				}
			}
		}
		return true;
	}

	/**
	 * Properties injection
	 * 
	 * @param beanDefinition
	 * @param beanClass
	 * @throws ConfigurationException
	 */
	private void properties(BeanDefinition beanDefinition, //
			Class<? extends Object> beanClass) throws ConfigurationException //
	{
		log.debug("Loading Properties For: [{}].", beanClass.getName());

		Props props = beanClass.getAnnotation(Props.class);

		List<PropertyValue> propertyValues = new ArrayList<>();

		Properties properties = getEnvironment().getProperties();
		String[] prefixs = props.prefix();
		Field[] declaredFields = beanClass.getDeclaredFields();
		for (String prefix : prefixs) {

			for (Field declaredField : declaredFields) {

				String key = prefix + declaredField.getName();

				String value = properties.getProperty(key);
				if (value == null) {
					continue;
				}
				log.debug("Found Properties key: [{}].", key);

				declaredField.setAccessible(true);
				String findInProperties = PropertiesUtils.findInProperties(//
						properties, PropertiesUtils.findInProperties(properties, value)//
				);

				propertyValues.add(//
						new PropertyValue(//
								ConvertUtils.convert(//
										findInProperties, declaredField.getType()//
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
				names = new String[] { method.getReturnType().getSimpleName() };// use simple name
			}

			for (String name : names) {

				if (StringUtils.isEmpty(name)) {
					name = method.getReturnType().getSimpleName();
				}

				prototypes.put(name, method);

				BeanDefinition beanDefinition_ = new BeanDefinition()//
						.setScope(scope)//
						.setBeanClass(method.getReturnType())//
						.setName(method.getDeclaringClass().getSimpleName()); // use declaring class name
				// register
				beanDefinitionLoader.register(name, beanDefinition_);
			}
		}
	}

	@Override
	protected void doCreateSingleton(Entry<String, BeanDefinition> entry, //
			Set<Entry<String, BeanDefinition>> entrySet) throws Exception //
	{
		String name = entry.getKey();
		BeanDefinition beanDefinition = entry.getValue();

		// remove singleton
		Method method = prototypes.get(name);
		if (method != null) {
			try {
				registerSingleton(name, //
						initializingBean(//
								method.invoke(//
										createDeclaringObject(name, beanDefinition.getName())//
								), name, beanDefinition//
						)//
				);
				beanDefinition.setInitialized(true);
				prototypes.remove(name);
			} //
			catch (InvocationTargetException e) {
				beanDefinition.setInitialized(false);
				log.error("Error with creating bean named: [{}]", name, e.getTargetException());
			}
			return;
		}
		super.doCreateSingleton(entry, entrySet);
	}

	/**
	 * 
	 * @param name
	 * @param declaringName
	 * @return
	 * @throws Exception
	 */
	private Object createDeclaringObject(String name, String declaringName) throws Exception {

		Object declaringSingleton = getSingleton(declaringName);
		BeanDefinition beanDefinition = getBeanDefinition(declaringName);

		if (beanDefinition.isInitialized() && declaringSingleton != null) {
			return declaringSingleton;
		}
		// declaring bean not initialized
		declaringSingleton = super.initializingBean(//
				createBeanInstance(//
						beanDefinition//
				), //
				name, //
				beanDefinition);
		// put declaring object
		registerSingleton(name, declaringSingleton);
		beanDefinition.setInitialized(true);
		return declaringSingleton;
	}

}
