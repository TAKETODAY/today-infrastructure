/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context;

import java.util.Properties;

import cn.taketoday.beans.BeanNameCreator;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.expression.ExpressionProcessor;
import cn.taketoday.util.ConvertUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY <br>
 * 2018-11-14 18:58
 * @since 2.0.1
 */
public interface Environment {
  String PROPERTIES_SUFFIX = ".properties";
  String KEY_ACTIVE_PROFILES = "context.active.profiles";
  String DEFAULT_YML_FILE = "classpath:application.yml"; // @since 1.0.2
  String DEFAULT_YAML_FILE = "classpath:application.yaml";
  String DEFAULT_PROPERTIES_FILE = "classpath:application.properties";

  /**
   * Get properties
   */
  Properties getProperties();

  /**
   * Return whether the given property key is available for resolution
   *
   * @param key
   *         key
   *
   * @return if contains
   */
  boolean containsProperty(String key);

  /**
   * Return the property value associated with the given key, or {@code null} if
   * the key cannot be resolved.
   *
   * @param key
   *         the property name to resolve
   *
   * @return Property value
   */
  String getProperty(String key);

  /**
   * Return the property value associated with the given key, or
   * {@code defaultValue} if the key cannot be resolved.
   *
   * @param key
   *         the property name to resolve
   * @param defaultValue
   *         the default value to return if no value is found
   *
   * @return the property value associated with the given key, or
   * {@code defaultValue} if the key cannot be resolved.
   */
  String getProperty(String key, String defaultValue);

  /**
   * Return the property value associated with the given key, or {@code null} if
   * the key cannot be resolved.
   *
   * @param key
   *         the property name to resolve
   * @param targetType
   *         the expected type of the property value
   *
   * @return the property value associated with the given key, or {@code null} if
   * the key cannot be resolved
   */
  default <T> T getProperty(String key, Class<T> targetType) {
    return getProperty(key, targetType, null);
  }

  /**
   * Return the property value associated with the given key, or
   * {@code defaultValue} if the key cannot be resolved.
   *
   * @param key
   *         the property name to resolve
   * @param targetType
   *         the expected type of the property value
   * @param defaultValue
   *         if the key cannot be resolved will return this value
   *
   * @return the property value associated with the given key, or
   * {@code defaultValue} if the key cannot be resolved
   *
   * @since 2.1.6
   */
  default <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
    return getProperty(key, s -> ConvertUtils.convert(targetType, s), defaultValue);
  }

  /**
   * @param key
   *         the property name to resolve
   * @param converter
   *         converter to convert a source value to target type value
   * @param defaultValue
   *         if the key cannot be resolved will return this value
   *
   * @return the property value associated with the given key and convert to
   * target type, or {@code defaultValue} if the key cannot be resolved
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
   *         profiles
   *
   * @return if accepted
   */
  boolean acceptsProfiles(String... profiles);

  /**
   * Get a bean name creator
   *
   * @return {@link BeanNameCreator} never be null
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
   * Get {@link ExpressionProcessor}
   *
   * @return {@link ExpressionProcessor}
   *
   * @since 2.1.7
   */
  ExpressionProcessor getExpressionProcessor();

  /**
   * Retrieve the flag for the given property key.
   *
   * @param key
   *         the property key
   *
   * @return {@code true} if the property is set to "true", {@code} false
   * otherwise
   */
  default boolean getFlag(String key) {
    return Boolean.parseBoolean(getProperty(key));
  }

  /**
   * Retrieve the flag for the given property key.
   * <p>
   * If there isn't a key returns defaultFlag
   * </p>
   *
   * @param key
   *         the property key
   *
   * @return {@code true} if the property is set to "true", {@code} false
   * otherwise ,If there isn't a key returns defaultFlag
   */
  default boolean getFlag(String key, boolean defaultFlag) {
    final String property = getProperty(key);
    return StringUtils.isEmpty(property) ? defaultFlag : Boolean.parseBoolean(property);
  }
}
