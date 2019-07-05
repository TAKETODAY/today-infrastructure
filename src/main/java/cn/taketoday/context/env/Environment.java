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
package cn.taketoday.context.env;

import java.util.Properties;

import javax.el.ELProcessor;

import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.conversion.Converter;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.utils.ConvertUtils;

/**
 * 
 * @author Today <br>
 * 
 *         2018-11-14 18:58
 * @since 2.0.1
 */
public interface Environment {

    /**
     * Get properties
     */
    Properties getProperties();

    /**
     * Return whether the given property key is available for resolution
     * 
     * @param key
     *            key
     * @return if contains
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
     * @return the property value associated with the given key, or
     *         {@code defaultValue} if the key cannot be resolved.
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
     * @return the property value associated with the given key, or {@code null} if
     *         the key cannot be resolved
     */
    default <T> T getProperty(String key, Class<T> targetType) {
        return getProperty(key, targetType, null);
    }

    /**
     * Return the property value associated with the given key, or
     * {@link defaultValue} if the key cannot be resolved.
     * 
     * @param key
     *            the property name to resolve
     * @param targetType
     *            the expected type of the property value
     * @param defaultValue
     *            if the key cannot be resolved will return this value
     * @return the property value associated with the given key, or
     *         {@link defaultValue} if the key cannot be resolved
     * @since 2.1.6
     */
    @SuppressWarnings("unchecked")
    default <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        return getProperty(key, (s) -> (T) ConvertUtils.convert(s, targetType), defaultValue);
    }

    /**
     * @param key
     *            the property name to resolve
     * @param converter
     *            converter to convert a source value to target type value
     * @param defaultValue
     *            if the key cannot be resolved will return this value
     * @return the property value associated with the given key and convert to
     *         target type, or {@link defaultValue} if the key cannot be resolved
     */
    @SuppressWarnings("unchecked")
    default <S, T> T getProperty(String key, Converter<S, T> converter, T defaultValue) {
        final String property = getProperty(key);
        if (property == null) {
            return defaultValue;
        }
        return converter.convert((S) property);
    }

    /**
     * Return the set of profiles explicitly made active for this environment.
     * 
     * @return active profiles
     */
    String[] getActiveProfiles();

    /**
     * If active profiles is empty return false. If active profiles is not empty
     * then will compare all active profiles.
     * 
     * @param profiles
     *            profiles
     * @return if accepted
     */
    boolean acceptsProfiles(String... profiles);

    /**
     * Get a bean name creator
     * 
     * @return {@link BeanNameCreator}
     */
    BeanNameCreator getBeanNameCreator();

    /**
     * Get bean definition loader
     * 
     * @return {@link BeanDefinitionLoader}
     */
    BeanDefinitionLoader getBeanDefinitionLoader();

    /**
     * Get the bean definition registry
     * 
     * @return {@link BeanDefinitionRegistry}
     */
    BeanDefinitionRegistry getBeanDefinitionRegistry();

    /**
     * Get {@link ELProcessor}
     * 
     * @return {@link ELProcessor}
     * @since 2.1.5
     */
    ELProcessor getELProcessor();
}
