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
package cn.taketoday.context.loader;

import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.factory.BeanDefinitionRegistry;

import java.util.Collection;

/**
 * Create bean definition
 * 
 * @author Today <br>
 * 
 *         2018-06-23 11:18:22
 */
public interface BeanDefinitionLoader {

	/**
	 * Create a bean definition with given class type
	 * 
	 * @param beanClass
	 *            the bean type
	 * @return A new {@link BeanDefinition} with out DestroyMethods
	 */
	BeanDefinition createBeanDefinition(Class<?> beanClass);

	/**
	 * Get registered bean definition registry
	 * 
	 * @return registry
	 */
	BeanDefinitionRegistry getRegistry();

	/**
	 * Load bean definitions with given bean collection.
	 * 
	 * @param beans
	 *            beans collection
	 * @throws BeanDefinitionStoreException
	 *             BeanDefinition could not be store
	 */
	void loadBeanDefinitions(Collection<Class<?>> beans) throws BeanDefinitionStoreException;

	/**
	 * Load bean definition with given bean class.
	 * 
	 * @param clazz
	 *            bean class
	 * @throws BeanDefinitionStoreException
	 *             BeanDefinition could not be store
	 */
	void loadBeanDefinition(Class<?> clazz) throws BeanDefinitionStoreException;

	/**
	 * Load bean definition with given bean class and bean name.
	 * 
	 * @param name
	 *            bean name
	 * @param beanClass
	 *            bean class
	 * @throws BeanDefinitionStoreException
	 *             BeanDefinition could not be store
	 */
	void loadBeanDefinition(String name, Class<?> beanClass) throws BeanDefinitionStoreException;

	/**
	 * Register bean definition with given class
	 * 
	 * @param clazz
	 *            bean class
	 * @throws BeanDefinitionStoreException
	 *             BeanDefinition could not be store
	 */
	void register(Class<?> clazz) throws BeanDefinitionStoreException;

	/**
	 * Register bean definition with given name , and resolve property values
	 * 
	 * @param name
	 *            bean name
	 * @param beanDefinition
	 *            bean definition instance
	 * @throws BeanDefinitionStoreException
	 *             BeanDefinition could not be store
	 * 
	 */
	void register(String name, BeanDefinition beanDefinition) throws BeanDefinitionStoreException;

}
