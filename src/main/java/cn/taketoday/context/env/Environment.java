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
package cn.taketoday.context.env;

import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.loader.BeanDefinitionLoader;

import java.util.Properties;

/**
 * 
 * @author Today <br>
 * 
 *         2018-11-14 18:58
 * @since 2.0.1
 */
public interface Environment {

	/**
	 * get properties
	 * 
	 * @return
	 */
	Properties getProperties();

	/**
	 * Return whether the given property key is available for resolution
	 */
	boolean containsProperty(String key);

	/**
	 * Return the property value associated with the given key, or {@code null} if
	 * the key cannot be resolved.
	 * 
	 * @param key
	 *            the property name to resolve
	 * @return
	 */
	String getProperty(String key);

	/**
	 * Return the property value associated with the given key, or
	 * {@code defaultValue} if the key cannot be resolved.
	 * 
	 * @param key
	 *            the property name to resolve
	 * @param defaultValue
	 *            the default value to return if no value is found
	 * @return
	 */
	String getProperty(String key, String defaultValue);

	/**
	 * Return the property value associated with the given key, or {@code null} if
	 * the key cannot be resolved.
	 * 
	 * @param key
	 *            the property name to resolve
	 * @param targetType
	 *            the expected type of the property value
	 * @return
	 */
	<T> T getProperty(String key, Class<T> targetType);

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

	/**
	 * Return the set of profiles explicitly made active for this environment.
	 */
	String[] getActiveProfiles();

	/**
	 * Return whether one or more of the given profiles is active or, in the case of
	 * logic is inverted, i.e. the method will return true if the given profile is
	 */
	boolean acceptsProfiles(String... profiles);

}
