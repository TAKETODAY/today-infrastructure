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

import cn.taketoday.lang.Assert;

/**
 * @author TODAY 2021/10/26 21:49
 * @since 4.0
 */
public class NamedBeanHolder<T> {

  private final String beanName;

  private final T beanInstance;

  /**
   * Create a new holder for the given bean name plus instance.
   *
   * @param beanName the name of the bean
   * @param beanInstance the corresponding bean instance
   */
  public NamedBeanHolder(String beanName, T beanInstance) {
    Assert.notNull(beanName, "Bean name must not be null");
    this.beanName = beanName;
    this.beanInstance = beanInstance;
  }

  /**
   * Return the name of the bean.
   */
  public String getBeanName() {
    return this.beanName;
  }

  /**
   * Return the corresponding bean instance.
   */
  public T getBeanInstance() {
    return this.beanInstance;
  }

}
