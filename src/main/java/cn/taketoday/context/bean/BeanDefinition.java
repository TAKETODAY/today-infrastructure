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

import cn.taketoday.context.Scope;
import cn.taketoday.context.exception.NoSuchPropertyException;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * 
 * @author Today <br>
 *         2018-06-23 11:23:45
 */
public interface BeanDefinition {

	/**
	 * get a property
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
	 * @return
	 */
	Method[] getInitMethods();

	/**
	 * @return
	 */
	String[] getDestroyMethods();

	/**
	 * @return
	 */
	Scope getScope();

	String getName();

	boolean isFactoryBean();

	boolean isInitialized();

	boolean isAbstract();

	PropertyValue[] getPropertyValues();

	// ----------------- Configurable
	/**
	 * Add PropertyValue to list.
	 * 
	 * @param propertyValue
	 */
	void addPropertyValue(PropertyValue... propertyValues_);

	/**
	 * 
	 * @param propertyValues
	 */
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
