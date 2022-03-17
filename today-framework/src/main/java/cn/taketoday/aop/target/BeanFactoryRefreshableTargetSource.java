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

package cn.taketoday.aop.target;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.lang.Assert;

/**
 * Refreshable TargetSource that fetches fresh target beans from a BeanFactory.
 *
 * <p>Can be subclassed to override {@code requiresRefresh()} to suppress
 * unnecessary refreshes. By default, a refresh will be performed every time
 * the "refreshCheckDelay" has elapsed.
 *
 * @author Rob Harrop
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author TODAY 2021/2/1 21:23
 * @see BeanFactory
 * @see #requiresRefresh()
 * @see #setRefreshCheckDelay
 * @since 3.0
 */
public class BeanFactoryRefreshableTargetSource extends AbstractRefreshableTargetSource {

  private final String beanName;
  private final BeanFactory beanFactory;

  /**
   * Create a new BeanFactoryRefreshableTargetSource for the given
   * bean factory and bean name.
   * <p>Note that the passed-in BeanFactory should have an appropriate
   * bean definition set up for the given bean name.
   *
   * @param beanFactory the BeanFactory to fetch beans from
   * @param beanName the name of the target bean
   */
  public BeanFactoryRefreshableTargetSource(BeanFactory beanFactory, String beanName) {
    Assert.notNull(beanName, "Bean name is required");
    Assert.notNull(beanFactory, "BeanFactory is required");
    this.beanName = beanName;
    this.beanFactory = beanFactory;
  }

  /**
   * Retrieve a fresh target object.
   */
  @Override
  protected final Object freshTarget() {
    return this.obtainFreshBean(this.beanFactory, this.beanName);
  }

  /**
   * A template method that subclasses may override to provide a
   * fresh target object for the given bean factory and bean name.
   * <p>This default implementation fetches a new target bean
   * instance from the bean factory.
   *
   * @see BeanFactory#getBean
   */
  protected Object obtainFreshBean(BeanFactory beanFactory, String beanName) {
    return beanFactory.getBean(beanName);
  }

}
