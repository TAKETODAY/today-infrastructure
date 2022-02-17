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

import cn.taketoday.lang.Nullable;

/**
 * Exception thrown when referring to an invalid bean property.
 * Carries the offending bean class and property name.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 17:39
 */
@SuppressWarnings("serial")
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
    super("Invalid property '" + propertyName + "' of bean class [" + beanClass.getName() + "]: " + msg, cause);
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
