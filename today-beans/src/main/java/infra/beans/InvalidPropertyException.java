/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.beans;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when referring to an invalid bean property.
 * Carries the offending bean class and property name.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 17:39
 */
public class InvalidPropertyException extends FatalBeanException {

  private final Class<?> beanClass;

  private final String propertyName;

  /**
   * Create a new InvalidPropertyException.
   *
   * @param beanClass the offending bean class
   * @param propertyName the offending property
   * @param msg the detail message
   */
  public InvalidPropertyException(Class<?> beanClass, String propertyName, String msg) {
    this(beanClass, propertyName, msg, null);
  }

  /**
   * Create a new InvalidPropertyException.
   *
   * @param beanClass the offending bean class
   * @param propertyName the offending property
   * @param msg the detail message
   * @param cause the root cause
   */
  public InvalidPropertyException(Class<?> beanClass, String propertyName, String msg, @Nullable Throwable cause) {
    super("Invalid property '%s' of bean class [%s]: %s".formatted(propertyName, beanClass.getName(), msg), cause);
    this.beanClass = beanClass;
    this.propertyName = propertyName;
  }

  /**
   * Return the offending bean class.
   */
  public Class<?> getBeanClass() {
    return this.beanClass;
  }

  /**
   * Return the name of the offending property.
   */
  public String getPropertyName() {
    return this.propertyName;
  }

}
