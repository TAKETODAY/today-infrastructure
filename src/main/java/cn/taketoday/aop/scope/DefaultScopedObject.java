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

package cn.taketoday.aop.scope;

import java.io.Serializable;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.lang.Assert;

/**
 * Default implementation of the {@link ScopedObject} interface.
 *
 * <p>Simply delegates the calls to the underlying
 * {@link ConfigurableBeanFactory bean factory}
 * ({@link ConfigurableBeanFactory#getBean(String)}/
 * {@link ConfigurableBeanFactory#destroyScopedBean(String)}).
 *
 * @author Juergen Hoeller
 * @see cn.taketoday.beans.factory.BeanFactory#getBean
 * @see ConfigurableBeanFactory#destroyScopedBean
 * @since 4.0
 */
@SuppressWarnings("serial")
public class DefaultScopedObject implements ScopedObject, Serializable {

  private final ConfigurableBeanFactory beanFactory;

  private final String targetBeanName;

  /**
   * Creates a new instance of the {@link DefaultScopedObject} class.
   *
   * @param beanFactory the {@link ConfigurableBeanFactory} that holds the scoped target object
   * @param targetBeanName the name of the target bean
   */
  public DefaultScopedObject(ConfigurableBeanFactory beanFactory, String targetBeanName) {
    Assert.notNull(beanFactory, "BeanFactory must not be null");
    Assert.hasText(targetBeanName, "'targetBeanName' must not be empty");
    this.beanFactory = beanFactory;
    this.targetBeanName = targetBeanName;
  }

  @Override
  public Object getTargetObject() {
    return beanFactory.getBean(targetBeanName);
  }

  @Override
  public void removeFromScope() {
    this.beanFactory.destroyScopedBean(this.targetBeanName);
  }

}
