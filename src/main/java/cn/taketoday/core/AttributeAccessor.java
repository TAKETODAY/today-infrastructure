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
package cn.taketoday.core;

import java.util.Map;
import java.util.function.Function;

/**
 * Interface defining a generic contract for attaching and accessing metadata
 * to/from arbitrary objects.
 *
 * @author Rob Harrop
 * @author TODAY <br>
 * 2020-02-22 12:47
 * @since 2.1.7
 */
public interface AttributeAccessor {

  /**
   * Set the attribute defined by {@code name} to the supplied {@code value}. If
   * {@code value} is {@code null}, the attribute is {@link #removeAttribute
   * removed}.
   * <p>
   * In general, users should take care to prevent overlaps with other metadata
   * attributes by using fully-qualified names, perhaps using class or package
   * names as prefix.
   *
   * @param name
   *         the unique attribute key
   * @param value
   *         the attribute value to be attached
   */
  void setAttribute(String name, Object value);

  /**
   * Get the value of the attribute identified by {@code name}. Return
   * {@code null} if the attribute doesn't exist.
   *
   * @param name
   *         the unique attribute key
   *
   * @return the current value of the attribute, if any
   */
  Object getAttribute(String name);

  /**
   * Remove the attribute identified by {@code name} and return its value. Return
   * {@code null} if no attribute under {@code name} is found.
   *
   * @param name
   *         the unique attribute key
   *
   * @return the last value of the attribute, if any
   */
  Object removeAttribute(String name);

  /**
   * Return {@code true} if the attribute identified by {@code name} exists.
   * Otherwise return {@code false}.
   *
   * @param name
   *         the unique attribute key
   */
  boolean hasAttribute(String name);

  /**
   * Return the names of all attributes.
   */
  String[] attributeNames();

  /**
   * Return attributes map
   *
   * @return attributes map
   *
   * @since 3.0
   */
  Map<String, Object> getAttributes();

  /**
   * Compute a new value for the attribute identified by {@code name} if
   * necessary and {@linkplain #setAttribute set} the new value in this
   * {@code AttributeAccessor}.
   * <p>If a value for the attribute identified by {@code name} already exists
   * in this {@code AttributeAccessor}, the existing value will be returned
   * without applying the supplied compute function.
   * <p>The default implementation of this method is not thread safe but can
   * overridden by concrete implementations of this interface.
   *
   * @param <T>
   *         the type of the attribute value
   * @param name
   *         the unique attribute key
   * @param computeFunction
   *         a function that computes a new value for the attribute
   *         name; the function must not return a {@code null} value
   *
   * @return the existing value or newly computed value for the named attribute
   *
   * @throws IllegalStateException
   *         If computeFunction returns {@code null}
   * @see #getAttribute(String)
   * @see #setAttribute(String, Object)
   * @since 3.0
   */
  @SuppressWarnings("unchecked")
  default <T> T computeAttribute(String name, Function<String, T> computeFunction) {
    Assert.notNull(name, "Name must not be null");
    Assert.notNull(computeFunction, "Compute function must not be null");
    Object value = getAttribute(name);
    if (value == null) {
      value = computeFunction.apply(name);
      if (value == null) {
        throw new IllegalStateException("Compute function must not return null for attribute named '" + name + '\'');
      }
      setAttribute(name, value);
    }
    return (T) value;
  }

  /**
   * @since 3.0
   */
  void copyAttributesFrom(AttributeAccessor source);

}
