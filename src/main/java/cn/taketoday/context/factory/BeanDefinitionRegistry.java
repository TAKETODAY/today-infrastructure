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

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;

/**
 * Store bean definitions.
 * 
 * 
 * @author Today <br>
 * 
 *         2018-07-08 19:56:53 2018-08-06 11:07
 */
public interface BeanDefinitionRegistry {

	/**
	 * Get instances Map
	 * 
	 * @return
	 */
	Map<String, Object> getSingletonsMap();

	/**
	 * All the {@link PropertyValue}.
	 * 
	 * @return
	 */
	Set<PropertyValue> getDependency();

	/**
	 * get bean instance, one {@link BeanDefinition} can have a lot of names, so
	 * can't put instances in BeanDefinition.
	 * 
	 * @param name
	 *            bean name
	 * @return bean instance
	 * @throws NoSuchBeanDefinitionException
	 */
	Object getSingleton(String name);

	/**
	 * put instance.
	 * 
	 * @param name
	 *            bean name
	 * @param bean
	 *            bean instance
	 */
	Object putSingleton(String name, Object bean);

	/**
	 * contains instance with given name?
	 * 
	 * @param name
	 *            bean name
	 * @return
	 */
	boolean containsInstance(String name);

	/**
	 * 
	 * @param name
	 *            exclude name
	 */
	void addExcludeName(String name);

	/**
	 * 
	 * @return
	 */
	Map<String, BeanDefinition> getBeanDefinitionsMap();

	/**
	 * save bean definition.
	 * 
	 * @param beanName
	 *            bean name
	 * @param beanDefinition
	 *            bean definition instance
	 * @throws BeanDefinitionStoreException
	 */
	void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionStoreException;

	/**
	 * Remove the BeanDefinition for the given name.
	 * 
	 * @param beanName
	 *            the name of the bean instance to register
	 * @throws NoSuchBeanDefinitionException
	 *             if there is no such bean definition
	 */
	void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * Return the BeanDefinition for the given bean name.
	 * 
	 * @param beanName
	 *            name of the bean to find a definition for
	 * @return the BeanDefinition for the given name (never {@code null})
	 * @throws NoSuchBeanDefinitionException
	 *             if there is no such bean definition
	 */
	BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * Check if this registry contains a bean definition with the given name.
	 * 
	 * @param beanName
	 *            the name of the bean to look for
	 * @return if this registry contains a bean definition with the given name
	 */
	boolean containsBeanDefinition(String beanName);

	/**
	 * Return the names of all beans defined in this registry.
	 * 
	 * @return the names of all beans defined in this registry, or an empty set if
	 *         none defined
	 */
	Set<String> getBeanDefinitionNames();

	/**
	 * Return the number of beans defined in the registry.
	 * 
	 * @return the number of beans defined in the registry
	 */
	int getBeanDefinitionCount();

	/**
	 * get properties
	 * 
	 * @return
	 */
	Properties getProperties();

}
