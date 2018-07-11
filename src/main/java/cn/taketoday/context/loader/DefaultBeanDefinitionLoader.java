/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://yanghaijian.top
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
package cn.taketoday.context.loader;

import java.lang.reflect.Field;
import java.util.Set;

import cn.taketoday.context.annotation.ActionProcessor;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.Property;
import cn.taketoday.context.annotation.Prototype;
import cn.taketoday.context.annotation.Repository;
import cn.taketoday.context.annotation.RestProcessor;
import cn.taketoday.context.annotation.Service;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.BeanReference;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.bean.PropertyValues;
import cn.taketoday.context.core.Scope;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConversionException;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.utils.NumberUtils;
import cn.taketoday.context.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Today
 * @date 2018年6月23日 上午11:18:22
 */
@Slf4j
public final class DefaultBeanDefinitionLoader implements BeanDefinitionLoader {

	private BeanDefinitionRegistry registry;

	public DefaultBeanDefinitionLoader(BeanDefinitionRegistry registry) {
		this.registry = registry;
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

		BeanDefinition beanDefinition = new BeanDefinition();
		Class<?>[] interfaces = clazz.getInterfaces();
		if (interfaces.length > 0) {
			for (Class<?> cla : interfaces) {
				beanDefinition.setBeanClass(clazz);
				register(beanDefinition, cla);
			}
			return;
		}
		beanDefinition.setBeanClass(clazz);
		register(beanDefinition, clazz);
	}

	@Override
	public void loadBeanDefinitions(Set<Class<?>> beans) throws BeanDefinitionStoreException {
		for (Class<?> clazz : beans) {
			loadBeanDefinition(clazz);
		}
	}


	@Override
	public void loadBeanDefinition(String name, Class<?> clazz) throws BeanDefinitionStoreException {
		
		if (clazz.isInterface()) {
			throw new BeanDefinitionStoreException("class -> " + clazz.getName() + "can't be interface");
		}

		BeanDefinition beanDefinition = new BeanDefinition();

		beanDefinition.setBeanClass(clazz);
		
		register(name, beanDefinition, clazz);
	}
	
	/**
	 * register
	 * @param beanDefinition
	 * @param clazz
	 * @throws BeanDefinitionStoreException
	 */
	private void register(final BeanDefinition beanDefinition, Class<?> clazz) throws BeanDefinitionStoreException {
		String name = name(beanDefinition, clazz);
		if (name == null) {
			return;
		}
		register(name, beanDefinition, clazz);
	}
	
	/**
	 * 
	 * @param name
	 * @param beanDefinition
	 * @param clazz
	 * @throws BeanDefinitionStoreException
	 */
	private void register(String name, BeanDefinition beanDefinition, Class<?> clazz) throws BeanDefinitionStoreException {
		// setPropertyValues
		beanDefinition.setPropertyValues(processProperty(beanDefinition));
		
		registry.registerBeanDefinition(name, beanDefinition);
	}

	/**
	 * get name
	 * 
	 * @param cla
	 * @return
	 */
	private String name(BeanDefinition beanDefinition, Class<?> clazz) {
		Class<?> cla = beanDefinition.getBeanClass();
		String name = null;
		Service service = cla.getAnnotation(Service.class);
		Singleton singleton = cla.getAnnotation(Singleton.class);
		Component component = cla.getAnnotation(Component.class);
		Prototype prototype = cla.getAnnotation(Prototype.class);
		Repository repository = cla.getAnnotation(Repository.class);
		RestProcessor restProcessor = cla.getAnnotation(RestProcessor.class);
		ActionProcessor actionProcessor = cla.getAnnotation(ActionProcessor.class);
		if (service != null) {
			name = StringUtil.isEmpty(service.value()) ? clazz.getName() : service.value();
		}
		if (singleton != null) {
			name = StringUtil.isEmpty(singleton.value()) ? clazz.getName() : singleton.value();
		}
		if (prototype != null) {
			name = StringUtil.isEmpty(prototype.value()) ? clazz.getName() : prototype.value();
			beanDefinition.setScope(Scope.PROTOTYPE);
		}
		if (repository != null) {
			name = StringUtil.isEmpty(repository.value()) ? clazz.getName() : repository.value();
		}
		if (restProcessor != null) {
			name = clazz.getName();
		}
		if (actionProcessor != null) {
			name = clazz.getName();
		}
		if (component != null) {
			name = StringUtil.isEmpty(component.value()) ? clazz.getName() : component.value();
			beanDefinition.setScope(component.scope());
		}

		if (name == null) {
			return null;
		}
		return name;
	}

	/**
	 * process bean's property (field)
	 * 
	 * @param beanDefinition
	 * @return
	 */
	private PropertyValues processProperty(BeanDefinition beanDefinition) {

		Class<?> clazz = beanDefinition.getBeanClass();

		PropertyValues propertyValues = new PropertyValues();

		Field[] declaredFields = clazz.getDeclaredFields();

		for (Field field : declaredFields) {
			field.setAccessible(true);
			PropertyValue propertyValue = new PropertyValue();

			String value_ = null;
			Property property = field.getAnnotation(Property.class);
			if (property != null) {
				String value = property.value();
				if (!"".equals(value)) {
					value_ = findInPropertyFile(value);
				} else {// use field name
					value_ = registry.getProperties().getProperty(field.getName());
				}
				propertyValue.setValue(convert(value_, field.getType()));
			} else {
				Autowired autowired = field.getAnnotation(Autowired.class);
				if (autowired == null) {
					continue;
				}
				BeanReference reference = new BeanReference();
				if (!"".equals(autowired.value())) {
					reference.setName(autowired.value()); // bean name
				} else if (!(autowired.class_() == Class.class)) {
					reference.setName(autowired.class_().getName()); // class name
				} else {
					reference.setName(field.getType().getName()); // field type name
				}
				propertyValue.setValue(reference);
			}
			propertyValue.setField(field);
			propertyValues.addPropertyValue(propertyValue);
		}
		return propertyValues;
	}

	/**
	 * convert string to target type
	 * 
	 * @param value
	 * @param type
	 * @return
	 */
	private Object convert(String value, Class<?> type) {

		if (type.getSuperclass() == Number.class) {

			if (StringUtil.isEmpty(value)) {
				return 0;
			} // parse number
			try {
				return NumberUtils.parseDigit(value, type);
			} catch (ConversionException e) {
				log.error("can not convert [{}] to [{}]", value, type);
				System.exit(0);
			}
		}

		if (type == boolean.class | type == Boolean.class) {
			return Boolean.valueOf(value);
		}

		return value;
	}

	/**
	 * find in property file
	 * 
	 * @param value_
	 * @return
	 */
	private String findInPropertyFile(String value_) {
		final String key = value_;
		if (value_.startsWith("#{") && value_.endsWith("}")) {
			value_ = registry.getProperties().getProperty(value_.replaceAll("[{|#|}]+", ""));
			if (value_ == null) {
				log.error("properties file lack -> [{}] , must specify a properties value", key);
				System.exit(0);// exit
			}
		}
		return value_;
	}

}
