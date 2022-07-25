/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.beans;

import java.io.Serial;

import cn.taketoday.lang.Nullable;

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
  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  private final String[] possibleMatches;

  /**
   * Create a new NotWritablePropertyException.
   *
   * @param beanClass the offending bean class
   * @param propertyName the offending property name
   */
  public NotWritablePropertyException(Class<?> beanClass, String propertyName) {
    super(beanClass, propertyName,
            "Bean property '" + propertyName + "' is not writable or has an invalid setter method: " +
                    "Does the return type of the getter match the parameter type of the setter?");
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
  public NotWritablePropertyException(Class<?> beanClass, String propertyName, String msg, @Nullable String[] possibleMatches) {
    super(beanClass, propertyName, msg);
    this.possibleMatches = possibleMatches;
  }

  /**
   * Return suggestions for actual bean property names that closely match
   * the invalid property name, if any.
   */
  @Nullable
  public String[] getPossibleMatches() {
    return this.possibleMatches;
  }

}
