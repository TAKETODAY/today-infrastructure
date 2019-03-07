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
package cn.taketoday.context.bean;

import java.lang.reflect.Method;
import java.util.Collection;

import cn.taketoday.context.Scope;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.exception.NoSuchPropertyException;
import cn.taketoday.context.factory.FactoryBean;

/**
 * Bean definition
 * 
 * @author Today <br>
 *         2018-06-23 11:23:45
 */
public interface BeanDefinition {

	/**
	 * Get a property
	 * 
	 * @param name
	 *            the name of property
	 * @return
	 * @throws NoSuchPropertyException
	 */
	PropertyValue getPropertyValue(String name) throws NoSuchPropertyException;

	public boolean isSingleton();

	Class<?> getBeanClass();

	/**
	 * Get init methods
	 * 
	 * @return get all the init methods
	 */
	Method[] getInitMethods();

	/**
	 * @return all the destroy methods name
	 */
	String[] getDestroyMethods();

	/**
	 * @return Bean {@link Scope}
	 */
	Scope getScope();

	/**
	 * Get bean name
	 * 
	 * @return bean name
	 */
	String getName();

	/**
	 * if bean is a {@link FactoryBean}
	 * 
	 * @return if is a {@link FactoryBean}
	 */
	boolean isFactoryBean();

	/**
	 * if a {@link Singleton} has initialized
	 * 
	 * @return if its initialized
	 */
	boolean isInitialized();

	/**
	 * if it is from abstract class
	 * 
	 * @return if it is from abstract class
	 */
	boolean isAbstract();

	/**
	 * Get all the {@link PropertyValue}s
	 * 
	 * @return all {@link PropertyValue}
	 */
	PropertyValue[] getPropertyValues();

	// ----------------- Configurable
	/**
	 * Add PropertyValue to list.
	 * 
	 * @param propertyValue
	 */
	void addPropertyValue(PropertyValue... propertyValues_);

	void addPropertyValue(Collection<PropertyValue> propertyValues);

	BeanDefinition setInitialized(boolean initialized);

	BeanDefinition setAbstract(boolean Abstract);

	BeanDefinition setName(String name);

	BeanDefinition setScope(Scope scope);

	BeanDefinition setBeanClass(Class<?> beanClass);

	BeanDefinition setInitMethods(Method[] initMethods);

	BeanDefinition setDestroyMethods(String[] destroyMethods);

	BeanDefinition setPropertyValues(PropertyValue[] propertyValues);

	BeanDefinition setFactoryBean(boolean factoryBean);

}
