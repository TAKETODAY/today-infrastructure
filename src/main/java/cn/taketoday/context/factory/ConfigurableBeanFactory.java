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

import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;

import java.util.Set;

/**
 * @author Today <br>
 * 
 *         2018-11-14 19:40
 */
public interface ConfigurableBeanFactory extends BeanFactory, SingletonBeanRegistry, BeanDefinitionRegistry {

	/**
	 * register a bean with the given name and type
	 * 
	 * @param beanDefinition
	 *            bean definition
	 * @throws BeanDefinitionStoreException
	 * @throws ConfigurationException
	 * @since 1.2.0
	 */
	void registerBean(String name, BeanDefinition beanDefinition) throws BeanDefinitionStoreException;

	/**
	 * remove bean with the given name
	 * 
	 * @param name
	 *            bean name
	 * @throws NoSuchBeanDefinitionException
	 */
	void removeBean(String name) throws BeanDefinitionStoreException;

	/**
	 * register a bean with the given name and type
	 * 
	 * @param name
	 *            bean name
	 * @param clazz
	 *            bean class
	 * @throws BeanDefinitionStoreException
	 */
	void registerBean(String name, Class<?> clazz) throws BeanDefinitionStoreException;

	/**
	 * register a bean with the given type
	 * 
	 * @param clazz
	 *            bean class
	 * @throws BeanDefinitionStoreException
	 * @throws ConfigurationException
	 */
	void registerBean(Class<?> clazz) //
			throws BeanDefinitionStoreException, ConfigurationException;

	/**
	 * register a bean with the given types
	 * 
	 * @param clazz
	 *            bean classes
	 * @throws BeanDefinitionStoreException
	 * @throws ConfigurationException
	 */
	void registerBean(Set<Class<?>> clazz) throws BeanDefinitionStoreException;

}
