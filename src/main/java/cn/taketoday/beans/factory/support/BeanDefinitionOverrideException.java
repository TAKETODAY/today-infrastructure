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

package cn.taketoday.beans.factory.support;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;

/**
 * Subclass of {@link BeanDefinitionStoreException} indicating an invalid override
 * attempt: typically registering a new definition for the same bean name while
 * {@link StandardBeanFactory#isAllowBeanDefinitionOverriding()} is {@code false}.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/10/1 15:53
 * @see StandardBeanFactory#setAllowBeanDefinitionOverriding
 * @see StandardBeanFactory#registerBeanDefinition
 * @since 4.0
 */
@SuppressWarnings("serial")
public class BeanDefinitionOverrideException extends BeanDefinitionStoreException {

  private final BeanDefinition beanDefinition;

  private final BeanDefinition existingDefinition;

  /**
   * Create a new BeanDefinitionOverrideException for the given new and existing definition.
   *
   * @param beanName the name of the bean
   * @param beanDefinition the newly registered bean definition
   * @param existingDefinition the existing bean definition for the same name
   */
  public BeanDefinitionOverrideException(
          String beanName, BeanDefinition beanDefinition, BeanDefinition existingDefinition) {
    super("Cannot register bean definition [" + beanDefinition + "] for bean '" + beanName +
                  "': There is already [" + existingDefinition + "] bound.");
    this.beanDefinition = beanDefinition;
    this.existingDefinition = existingDefinition;
  }

  /**
   * Return the newly registered bean definition.
   */
  public BeanDefinition getBeanDefinition() {
    return this.beanDefinition;
  }

  /**
   * Return the existing bean definition for the same name.
   */
  public BeanDefinition getExistingDefinition() {
    return this.existingDefinition;
  }

}
