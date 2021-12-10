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

package cn.taketoday.aop.support;

import cn.taketoday.aop.Pointcut;
import cn.taketoday.lang.Nullable;

/**
 * Concrete BeanFactory-based PointcutAdvisor that allows for any Advice
 * to be configured as reference to an Advice bean in the BeanFactory,
 * as well as the Pointcut to be configured through a bean property.
 *
 * <p>Specifying the name of an advice bean instead of the advice object itself
 * (if running within a BeanFactory) increases loose coupling at initialization time,
 * in order to not initialize the advice object until the pointcut actually matches.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setPointcut
 * @see #setAdviceBeanName
 * @since 4.0 2021/12/10 21:20
 */
@SuppressWarnings("serial")
public class DefaultBeanFactoryPointcutAdvisor extends AbstractBeanFactoryPointcutAdvisor {

  private Pointcut pointcut = Pointcut.TRUE;

  /**
   * Specify the pointcut targeting the advice.
   * <p>Default is {@code Pointcut.TRUE}.
   *
   * @see #setAdviceBeanName
   */
  public void setPointcut(@Nullable Pointcut pointcut) {
    this.pointcut = (pointcut != null ? pointcut : Pointcut.TRUE);
  }

  @Override
  public Pointcut getPointcut() {
    return this.pointcut;
  }

  @Override
  public String toString() {
    return getClass().getName() + ": pointcut [" + getPointcut() + "]; advice bean '" + getAdviceBeanName() + "'";
  }

}

