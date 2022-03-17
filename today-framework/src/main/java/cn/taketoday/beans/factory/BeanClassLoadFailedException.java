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
import cn.taketoday.beans.factory.config.BeanDefinition;
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

  private final BeanDefinition beanDefinition;
  @Nullable
  private final String resourceDescription;
  private final String beanName;

  /**
   * Create a new CannotLoadBeanClassException.
   *
   * that the bean definition came from
   *
   * @param beanName the name of the bean requested
   * @param cause the root cause
   */
  public BeanClassLoadFailedException(BeanDefinition def, @Nullable String beanName, ClassNotFoundException cause) {
    super("Error loading class [" + def.getBeanClassName() + "] for bean with name '" + beanName
            + "'" + getDesc(def) + ": problem with class file or dependent class", cause);
    this.beanDefinition = def;
    this.resourceDescription = def.getResourceDescription();
    this.beanName = beanName;
  }

  /**
   * Create a new CannotLoadBeanClassException.
   *
   * @param cause the root cause
   */
  public BeanClassLoadFailedException(BeanDefinition def, @Nullable String beanName, LinkageError cause) {
    super("Error loading class [" + def.getBeanClassName() + "] for bean with name '" + beanName
            + "'" + getDesc(def) + ": problem with class file or dependent class", cause);
    this.beanDefinition = def;
    this.beanName = beanName;
    this.resourceDescription = def.getResourceDescription();
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
    return beanName;
  }

  /**
   * Return the name of the class we were trying to load.
   */
  @Nullable
  public String getBeanClassName() {
    return this.beanDefinition.getBeanClassName();
  }

  public BeanDefinition getBeanDefinition() {
    return beanDefinition;
  }

}
