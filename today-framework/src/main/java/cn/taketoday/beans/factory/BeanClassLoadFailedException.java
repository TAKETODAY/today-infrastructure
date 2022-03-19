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
import cn.taketoday.lang.Nullable;

/**
 * Exception thrown when the BeanFactory cannot load the specified class
 * of a given bean.
 *
 * @author TODAY 2021/10/23 19:19
 * @since 4.0
 */
@SuppressWarnings("serial")
public class BeanClassLoadFailedException extends BeansException {

  @Nullable
  private final String resourceDescription;

  private final String beanName;

  @Nullable
  private final String beanClassName;

  /**
   * Create a new CannotLoadBeanClassException.
   *
   * @param resourceDescription description of the resource
   * that the bean definition came from
   * @param beanName the name of the bean requested
   * @param beanClassName the name of the bean class
   * @param cause the root cause
   */
  public BeanClassLoadFailedException(@Nullable String resourceDescription, String beanName,
          @Nullable String beanClassName, ClassNotFoundException cause) {

    super("Cannot find class [" + beanClassName + "] for bean with name '" + beanName + "'" +
            (resourceDescription != null ? " defined in " + resourceDescription : ""), cause);
    this.resourceDescription = resourceDescription;
    this.beanName = beanName;
    this.beanClassName = beanClassName;
  }

  /**
   * Create a new CannotLoadBeanClassException.
   *
   * @param resourceDescription description of the resource
   * that the bean definition came from
   * @param beanName the name of the bean requested
   * @param beanClassName the name of the bean class
   * @param cause the root cause
   */
  public BeanClassLoadFailedException(@Nullable String resourceDescription, String beanName,
          @Nullable String beanClassName, LinkageError cause) {

    super("Error loading class [" + beanClassName + "] for bean with name '" + beanName + "'" +
            (resourceDescription != null ? " defined in " + resourceDescription : "") +
            ": problem with class file or dependent class", cause);
    this.resourceDescription = resourceDescription;
    this.beanName = beanName;
    this.beanClassName = beanClassName;
  }

  /**
   * Return the description of the resource that the bean
   * definition came from.
   */
  @Nullable
  public String getResourceDescription() {
    return this.resourceDescription;
  }

  /**
   * Return the name of the bean requested.
   */
  public String getBeanName() {
    return this.beanName;
  }

  /**
   * Return the name of the class we were trying to load.
   */
  @Nullable
  public String getBeanClassName() {
    return this.beanClassName;
  }

}
