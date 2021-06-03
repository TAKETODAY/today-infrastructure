/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import java.io.Serializable;
import java.util.Objects;

import org.aopalliance.aop.Advice;

import cn.taketoday.aop.PointcutAdvisor;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.OrderedSupport;

/**
 * Abstract base class for {@link PointcutAdvisor}
 * implementations. Can be subclassed for returning a specific pointcut/advice
 * or a freely configurable pointcut/advice.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 19:06
 * @since 3.0
 */
public abstract class AbstractPointcutAdvisor
        extends OrderedSupport implements PointcutAdvisor, Serializable {
  private static final long serialVersionUID = 1L;
  
  private Advice advice = EMPTY_ADVICE;

  @Override
  public int getOrder() {
    if (this.order != null) {
      return this.order;
    }
    Advice advice = getAdvice();
    if (advice instanceof Ordered) {
      return ((Ordered) advice).getOrder();
    }
    return Ordered.LOWEST_PRECEDENCE;
  }

  /**
   * Specify the advice that this advisor should apply.
   */
  public void setAdvice(Advice advice) {
    this.advice = advice;
  }

  @Override
  public Advice getAdvice() {
    return this.advice;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof PointcutAdvisor)) {
      return false;
    }
    PointcutAdvisor otherAdvisor = (PointcutAdvisor) other;
    return Objects.equals(getAdvice(), otherAdvisor.getAdvice())
            && Objects.equals(getPointcut(), otherAdvisor.getPointcut());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(advice);
  }

  @Override
  public String toString() {
    return getClass().getName() + ": advice [" + getAdvice() + "]";
  }
}
