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
import cn.taketoday.context.loader.BeanDefinitionLoader;

import java.util.Set;

/**
 * 
 * @author Today <br>
 * 
 *         2018-06-23 11:22:26
 */
public interface BeanFactory {

	
	String FACTORY_BEAN_PREFIX = "$";
	
	/**
	 * 
	 * @param name
	 * @param bean
	 */
	void registerSingleton(String name, Object bean);

	/**
	 * refresh factory, initialize singleton
	 */
	void onRefresh();

	/**
	 * remove bean with the given name
	 * 
	 * @param name
	 *            bean name
	 * @throws NoSuchBeanDefinitionException
	 */
	void removeBeanDefinition(String name) throws NoSuchBeanDefinitionException;

	/**
	 * register a bean with the given name and type
	 * 
	 * @param beanDefinition
	 *            bean definition
	 * @throws BeanDefinitionStoreException
	 * @throws ConfigurationException
	 * @since 1.2.0
	 */
	void registerBeanDefinition(String name, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException, ConfigurationException;

	/**
	 * register a bean with the given name and type
	 * 
	 * @param name
	 *            bean name
	 * @param clazz
	 *            bean class
	 * @throws BeanDefinitionStoreException
	 */
	void registerBeanDefinition(String name, Class<?> clazz) throws BeanDefinitionStoreException;

	/**
	 * register a bean with the given type
	 * 
	 * @param clazz
	 *            bean class
	 * @throws BeanDefinitionStoreException
	 * @throws ConfigurationException
	 */
	void registerBeanDefinition(Class<?> clazz) throws BeanDefinitionStoreException, ConfigurationException;

	/**
	 * register a bean with the given types
	 * 
	 * @param clazz
	 *            bean classes
	 * @throws BeanDefinitionStoreException
	 * @throws ConfigurationException
	 */
	void registerBeanDefinition(Set<Class<?>> clazz) throws BeanDefinitionStoreException, ConfigurationException;

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
	 * find the bean with the given type, throw an NoSuchBeanDefinitionException if
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
	 * whether there is a bean with the given name.
	 * 
	 * @param name
	 *            bean name
	 * @return
	 */
	boolean containsBeanDefinition(String name);

	/**
	 * whether there is a bean with the given type.
	 * 
	 * @param type
	 *            bean type
	 * @return
	 */
	boolean containsBeanDefinition(Class<?> type);

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

	/**
	 * 
	 * @param beanDefinitionRegistry
	 */
	void setBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry);

	/**
	 * get the bean definition registry
	 * 
	 * @return
	 */
	BeanDefinitionRegistry getBeanDefinitionRegistry();

	/**
	 * get bean definition loader
	 * 
	 * @return
	 */
	BeanDefinitionLoader getBeanDefinitionLoader();

}
