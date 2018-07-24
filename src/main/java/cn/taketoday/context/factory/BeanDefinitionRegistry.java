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
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;

/**
 * @author Today
 * @date 2018年7月8日 下午7:56:53
 */
public interface BeanDefinitionRegistry {

	Map<String, BeanDefinition> getBeanDefinitionsMap();
	
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
	 * @return the names of all beans defined in this registry, or an empty array if
	 *         none defined
	 */
	Set<String> getBeanDefinitionNames();

	/**
	 * Return the bean definitions of all beans defined in this registry.
	 * 
	 * @return
	 */
	Set<BeanDefinition> getBeanDefinitions();

	/**
	 * Return the number of beans defined in the registry.
	 * 
	 * @return the number of beans defined in the registry
	 */
	int getBeanDefinitionCount();

	/**
	 * Determine whether the given bean name is already in use within this registry,
	 * i.e. whether there is a local bean or alias registered under this name.
	 * 
	 * @param beanName
	 *            the name to check
	 * @return whether the given bean name is already in use
	 */
	boolean isBeanNameInUse(String beanName);
	
	/**
	 * get properties
	 * @return
	 */
	Properties getProperties();

}
