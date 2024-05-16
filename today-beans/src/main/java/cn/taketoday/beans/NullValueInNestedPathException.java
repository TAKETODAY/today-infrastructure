/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.beans;

/**
 * Exception thrown when navigation of a valid nested property
 * path encounters a NullPointerException.
 *
 * <p>For example, navigating "spouse.age" could fail because the
 * spouse property of the target object has a null value.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 17:45
 */
public class NullValueInNestedPathException extends InvalidPropertyException {

  /**
   * Create a new NullValueInNestedPathException.
   *
   * @param beanClass the offending bean class
   * @param propertyName the offending property
   */
  public NullValueInNestedPathException(Class<?> beanClass, String propertyName) {
    super(beanClass, propertyName, "Value of nested property '%s' is null".formatted(propertyName));
  }

  /**
   * Create a new NullValueInNestedPathException.
   *
   * @param beanClass the offending bean class
   * @param propertyName the offending property
   * @param msg the detail message
   */
  public NullValueInNestedPathException(Class<?> beanClass, String propertyName, String msg) {
    super(beanClass, propertyName, msg);
  }

  /**
   * Create a new NullValueInNestedPathException.
   *
   * @param beanClass the offending bean class
   * @param propertyName the offending property
   * @param msg the detail message
   * @param cause the root cause
   */
  public NullValueInNestedPathException(Class<?> beanClass, String propertyName, String msg, Throwable cause) {
    super(beanClass, propertyName, msg, cause);
  }

}
