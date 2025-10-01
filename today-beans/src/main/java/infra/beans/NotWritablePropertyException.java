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
 * Exception thrown on an attempt to set the value of a property that
 * is not writable (typically because there is no setter method).
 *
 * @author Rod Johnson
 * @author Alef Arendsen
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 17:44
 */
public class NotWritablePropertyException extends InvalidPropertyException {

  private final String @Nullable [] possibleMatches;

  /**
   * Create a new NotWritablePropertyException.
   *
   * @param beanClass the offending bean class
   * @param propertyName the offending property name
   */
  public NotWritablePropertyException(Class<?> beanClass, String propertyName) {
    super(beanClass, propertyName,
            "Bean property '%s' is not writable or has an invalid setter method: Does the return type of the getter match the parameter type of the setter?"
                    .formatted(propertyName));
    this.possibleMatches = null;
  }

  /**
   * Create a new NotWritablePropertyException.
   *
   * @param beanClass the offending bean class
   * @param propertyName the offending property name
   * @param msg the detail message
   */
  public NotWritablePropertyException(Class<?> beanClass, String propertyName, String msg) {
    super(beanClass, propertyName, msg);
    this.possibleMatches = null;
  }

  /**
   * Create a new NotWritablePropertyException.
   *
   * @param beanClass the offending bean class
   * @param propertyName the offending property name
   * @param msg the detail message
   * @param cause the root cause
   */
  public NotWritablePropertyException(Class<?> beanClass, String propertyName, String msg, Throwable cause) {
    super(beanClass, propertyName, msg, cause);
    this.possibleMatches = null;
  }

  /**
   * Create a new NotWritablePropertyException.
   *
   * @param beanClass the offending bean class
   * @param propertyName the offending property name
   * @param msg the detail message
   * @param possibleMatches suggestions for actual bean property names
   * that closely match the invalid property name
   */
  public NotWritablePropertyException(Class<?> beanClass, String propertyName, String msg, String @Nullable [] possibleMatches) {
    super(beanClass, propertyName, msg);
    this.possibleMatches = possibleMatches;
  }

  /**
   * Return suggestions for actual bean property names that closely match
   * the invalid property name, if any.
   */
  public String @Nullable [] getPossibleMatches() {
    return this.possibleMatches;
  }

}
