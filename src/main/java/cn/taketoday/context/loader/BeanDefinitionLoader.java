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

import java.util.Set;

import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.factory.BeanDefinitionRegistry;

/**
 * @author Today
 * @date 2018年6月23日 上午11:18:22
 */
public interface BeanDefinitionLoader {

	/**
	 * Get registered bean Definitions
	 * 
	 * @return
	 */
	public BeanDefinitionRegistry getRegistry();

	/**
	 * 
	 * @param beans
	 * @throws BeanDefinitionStoreException
	 */
	public void loadBeanDefinitions(Set<Class<?>> beans) throws BeanDefinitionStoreException;

	/**
	 * 
	 * @param clazz
	 * @throws BeanDefinitionStoreException
	 */
	public void loadBeanDefinition(Class<?> clazz) throws BeanDefinitionStoreException;
	
	/**
	 * 
	 * @param name
	 * @param clazz
	 * @throws BeanDefinitionStoreException
	 */
	public void loadBeanDefinition(String name, Class<?> clazz) throws BeanDefinitionStoreException;

}
