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

/**
 * Exception thrown on an attempt to get the value of a property
 * that isn't readable, because there's no getter method.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 17:56
 */
public class NotReadablePropertyException extends InvalidPropertyException {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Create a new NotReadablePropertyException.
   *
   * @param beanClass the offending bean class
   * @param propertyName the offending property
   */
  public NotReadablePropertyException(Class<?> beanClass, String propertyName) {
    super(beanClass, propertyName,
            "Bean property '" + propertyName + "' is not readable or has an invalid getter method: " +
                    "Does the return type of the getter match the parameter type of the setter?");
  }

  /**
   * Create a new NotReadablePropertyException.
   *
   * @param beanClass the offending bean class
   * @param propertyName the offending property
   * @param msg the detail message
   */
  public NotReadablePropertyException(Class<?> beanClass, String propertyName, String msg) {
    super(beanClass, propertyName, msg);
  }

  /**
   * Create a new NotReadablePropertyException.
   *
   * @param beanClass the offending bean class
   * @param propertyName the offending property
   * @param msg the detail message
   * @param cause the root cause
   */
  public NotReadablePropertyException(Class<?> beanClass, String propertyName, String msg, Throwable cause) {
    super(beanClass, propertyName, msg, cause);
  }

}