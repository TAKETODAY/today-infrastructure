/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aop.aspectj;

import org.aopalliance.aop.Advice;

import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.PointcutAdvisor;
import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * AspectJPointcutAdvisor that adapts an {@link AbstractAspectJAdvice}
 * to the {@link cn.taketoday.aop.PointcutAdvisor} interface.
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 4.0
 */
public class AspectJPointcutAdvisor implements PointcutAdvisor, Ordered {

  private final AbstractAspectJAdvice advice;

  private final Pointcut pointcut;

  @Nullable
  private Integer order;

  /**
   * Create a new AspectJPointcutAdvisor for the given advice.
   *
   * @param advice the AbstractAspectJAdvice to wrap
   */
  public AspectJPointcutAdvisor(AbstractAspectJAdvice advice) {
    Assert.notNull(advice, "Advice is required");
    this.advice = advice;
    this.pointcut = advice.buildSafePointcut();
  }

  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    if (order != null) {
      return order;
    }
    return advice.getOrder();
  }

  @Override
  public Advice getAdvice() {
    return this.advice;
  }

  @Override
  public Pointcut getPointcut() {
    return this.pointcut;
  }

  /**
   * Return the name of the aspect (bean) in which the advice was declared.
   *
   * @see AbstractAspectJAdvice#getAspectName()
   */
  public String getAspectName() {
    return this.advice.getAspectName();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof AspectJPointcutAdvisor otherAdvisor)) {
      return false;
    }
    return this.advice.equals(otherAdvisor.advice);
  }

  @Override
  public int hashCode() {
    return AspectJPointcutAdvisor.class.hashCode() * 29 + this.advice.hashCode();
  }

}
