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
package cn.taketoday.context.factory;

import java.util.Set;

import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;

/**
 * @author Today BeanFactory
 * @date 2018年6月23日 上午11:22:26
 */
public interface BeanFactory {

	/**
	 * refresh factory
	 */
	void onRefresh();

	/**
	 * remove bean with the given name
	 * 
	 * @param name
	 * @throws NoSuchBeanDefinitionException
	 */
	void removeBean(String name) throws NoSuchBeanDefinitionException;

	/**
	 * register a bean with the given name and type
	 * 
	 * @param name
	 * @param clazz
	 * @throws BeanDefinitionStoreException
	 */
	void registerBean(String name, Class<?> clazz) throws BeanDefinitionStoreException;

	/**
	 * register a bean with the given type
	 * 
	 * @param clazz
	 * @throws BeanDefinitionStoreException
	 */
	void registerBean(Class<?> clazz) throws BeanDefinitionStoreException;

	/**
	 * register a bean with the given types
	 * 
	 * @param clazz
	 * @throws BeanDefinitionStoreException
	 */
	void registerBean(Set<Class<?>> clazz) throws BeanDefinitionStoreException;

	/**
	 * find the bean with the given type, throw an NoSuchBeanDefinitionException if
	 * it doesn't exist
	 * 
	 * @param name
	 * @return get bean instance
	 * @throws NoSuchBeanDefinitionException
	 */
	Object getBean(String name) throws NoSuchBeanDefinitionException;

	/**
	 * find the bean with the given type, throw an NoSuchBeanDefinitionException if
	 * it doesn't exist
	 * 
	 * @param requiredType
	 * @return get casted bean instance
	 * @throws NoSuchBeanDefinitionException
	 */
	<T> T getBean(Class<T> requiredType) throws NoSuchBeanDefinitionException;

	/**
	 * find the bean with the given name and cast to required type, throw an
	 * NoSuchBeanDefinitionException if it doesn't exist
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
	 * whether there is a bean with the given name.
	 * 
	 * @param name
	 *            bean name
	 * @return
	 */
	boolean containsBean(String name);

	/**
	 * whether there is a bean with the given type.
	 * 
	 * @param type
	 *            bean type
	 * @return
	 */
	boolean containsBean(Class<?> type);

	/**
	 * is Singleton ?
	 * 
	 * @param name
	 * @return
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
	 * @return
	 */
	Set<String> getAliases(Class<?> type);

}
