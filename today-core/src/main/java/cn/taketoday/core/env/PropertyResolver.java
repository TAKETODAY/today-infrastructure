/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.core.env;

import cn.taketoday.lang.Nullable;

/**
 * Interface for resolving properties against any underlying source.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Environment
 * @see PropertySourcesPropertyResolver
 * @since 4.0
 */
public interface PropertyResolver {

  /**
   * Return whether the given property key is available for resolution,
   * i.e. if the value for the given key is not {@code null}.
   */
  boolean containsProperty(String key);

  /**
   * Return the property value associated with the given key,
   * or {@code null} if the key cannot be resolved.
   *
   * @param key the property name to resolve
   * @see #getProperty(String, String)
   * @see #getProperty(String, Class)
   * @see #getRequiredProperty(String)
   */
  @Nullable
  String getProperty(String key);

  /**
   * Return the property value associated with the given key, or
   * {@code defaultValue} if the key cannot be resolved.
   *
   * @param key the property name to resolve
   * @param defaultValue the default value to return if no value is found
   * @see #getRequiredProperty(String)
   * @see #getProperty(String, Class)
   */
  String getProperty(String key, String defaultValue);

  /**
   * Return the property value associated with the given key,
   * or {@code null} if the key cannot be resolved.
   *
   * @param key the property name to resolve
   * @param targetType the expected type of the property value
   * @see #getRequiredProperty(String, Class)
   */
  @Nullable
  <T> T getProperty(String key, Class<T> targetType);

  /**
   * Return the property value associated with the given key,
   * or {@code defaultValue} if the key cannot be resolved.
   *
   * @param key the property name to resolve
   * @param targetType the expected type of the property value
   * @param defaultValue the default value to return if no value is found
   * @see #getRequiredProperty(String, Class)
   */
  <T> T getProperty(String key, Class<T> targetType, T defaultValue);

  /**
   * Return the property value associated with the given key (never {@code null}).
   *
   * @throws IllegalStateException if the key cannot be resolved
   * @see #getRequiredProperty(String, Class)
   */
  String getRequiredProperty(String key) throws IllegalStateException;

  /**
   * Return the property value associated with the given key, converted to the given
   * targetType (never {@code null}).
   *
   * @throws IllegalStateException if the given key cannot be resolved
   */
  <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException;

  /**
   * Retrieve the flag for the given property key.
   * <p>
   * Returns {@code false} if key not found
   *
   * @param key the property key
   * @return {@code true} if the property is set to "true", {@code} false
   * otherwise
   */
  default boolean getFlag(String key) {
    return getFlag(key, false);
  }

  /**
   * Retrieve the flag for the given property key.
   * <p>
   * If there isn't a key returns defaultFlag
   * </p>
   *
   * @param key the property key
   * @param defaultFlag default return value if key not found
   * @return {@code true} if the property is set to "true", {@code} false
   * otherwise ,If there isn't a key returns defaultFlag
   */
  default boolean getFlag(String key, boolean defaultFlag) {
    return getProperty(key, Boolean.class, defaultFlag);
  }

  /**
   * Resolve ${...} placeholders in the given text, replacing them with corresponding
   * property values as resolved by {@link #getProperty}. Unresolvable placeholders with
   * no default value are ignored and passed through unchanged.
   *
   * @param text the String to resolve
   * @return the resolved String (never {@code null})
   * @throws IllegalArgumentException if given text is {@code null}
   * @see #resolveRequiredPlaceholders
   */
  String resolvePlaceholders(String text);

  /**
   * Resolve ${...} placeholders in the given text, replacing them with corresponding
   * property values as resolved by {@link #getProperty}. Unresolvable placeholders with
   * no default value will cause an IllegalArgumentException to be thrown.
   *
   * @return the resolved String (never {@code null})
   * @throws IllegalArgumentException if given text is {@code null}
   * or if any placeholders are unresolvable
   */
  String resolveRequiredPlaceholders(String text) throws IllegalArgumentException;

}
