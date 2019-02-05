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
package cn.taketoday.context.factory;

import cn.taketoday.context.exception.NoSuchBeanDefinitionException;

import java.util.List;
import java.util.Set;

/**
 * bean factory
 * 
 * @author Today <br>
 * 
 *         2018-06-23 11:22:26
 */
public interface BeanFactory {

	String FACTORY_BEAN_PREFIX = "$";

	/**
	 * find the bean with the given type, throw an NoSuchBeanDefinitionException if
	 * it doesn't exist
	 * 
	 * @param name
	 *            bean name
	 * @return get bean instance
	 * @throws NoSuchBeanDefinitionException
	 */
	Object getBean(String name) throws NoSuchBeanDefinitionException;

	/**
	 * Find the bean with the given type, throw an NoSuchBeanDefinitionException if
	 * it doesn't exist
	 * 
	 * @param requiredType
	 *            bean type
	 * @return get casted bean instance
	 * @throws NoSuchBeanDefinitionException
	 */
	<T> T getBean(Class<T> requiredType) throws NoSuchBeanDefinitionException;

	/**
	 * find the bean with the given name and cast to required type, throw an
	 * NoSuchBeanDefinitionException if it doesn't exist.
	 * 
	 * @param name
	 *            bean name
	 * @param requiredType
	 *            cast to required type
	 * @return get casted bean instance
	 * @throws NoSuchBeanDefinitionException
	 */
	<T> T getBean(String name, Class<T> requiredType) throws NoSuchBeanDefinitionException;

	/**
	 * is Singleton ?
	 * 
	 * @param name
	 * @return if this bean is a singleton
	 * @throws NoSuchBeanDefinitionException
	 */
	boolean isSingleton(String name) throws NoSuchBeanDefinitionException;

	/**
	 * is Prototype ?
	 * 
	 * @param name
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 */
	boolean isPrototype(String name) throws NoSuchBeanDefinitionException;

	/**
	 * get bean type
	 * 
	 * @param name
	 *            bean name
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 */
	Class<?> getType(String name) throws NoSuchBeanDefinitionException;

	/**
	 * get all bean name
	 * 
	 * @param type
	 *            bean type
	 * @return a set of names with given type
	 */
	Set<String> getAliases(Class<?> type);

	/**
	 * Get the target class's name
	 * 
	 * @param requiredType
	 * @return
	 * @since 2.1.2
	 */
	String getBeanName(Class<?> requiredType);

	/**
	 * Get a list of beans with given type
	 * 
	 * @param requiredType
	 *            given bean type
	 * @return
	 * @since 2.1.2
	 */
	<T> List<T> getBeans(Class<T> requiredType);

}
