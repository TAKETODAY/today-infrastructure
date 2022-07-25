/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.beans.factory;

import cn.taketoday.beans.BeansException;
import cn.taketoday.util.ClassUtils;

/**
 * Thrown when a bean doesn't match the expected type.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/10/26 21:36
 * @since 4.0
 */
@SuppressWarnings("serial")
public class BeanNotOfRequiredTypeException extends BeansException {

  /** The name of the instance that was of the wrong type. */
  private final String beanName;

  /** The required type. */
  private final Class<?> requiredType;

  /** The offending type. */
  private final Class<?> actualType;

  /**
   * Create a new BeanNotOfRequiredTypeException.
   *
   * @param beanName the name of the bean requested
   * @param requiredType the required type
   * @param actualType the actual type returned, which did not match
   * the expected type
   */
  public BeanNotOfRequiredTypeException(String beanName, Class<?> requiredType, Class<?> actualType) {
    super("Bean named '" + beanName + "' is expected to be of type '" + ClassUtils.getQualifiedName(requiredType) +
            "' but was actually of type '" + ClassUtils.getQualifiedName(actualType) + "'");
    this.beanName = beanName;
    this.actualType = actualType;
    this.requiredType = requiredType;
  }

  /**
   * Return the name of the instance that was of the wrong type.
   */
  public String getBeanName() {
    return this.beanName;
  }

  /**
   * Return the expected type for the bean.
   */
  public Class<?> getRequiredType() {
    return this.requiredType;
  }

  /**
   * Return the actual type of the instance found.
   */
  public Class<?> getActualType() {
    return this.actualType;
  }

}
