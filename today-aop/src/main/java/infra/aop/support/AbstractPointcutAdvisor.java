/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.aop.support;

import org.aopalliance.aop.Advice;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

import infra.aop.PointcutAdvisor;
import infra.core.Ordered;
import infra.core.OrderedSupport;
import infra.util.ObjectUtils;

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
  @Serial
  private static final long serialVersionUID = 1L;

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

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof PointcutAdvisor otherAdvisor)) {
      return false;
    }
    return (ObjectUtils.nullSafeEquals(getAdvice(), otherAdvisor.getAdvice()) &&
            ObjectUtils.nullSafeEquals(getPointcut(), otherAdvisor.getPointcut()));
  }

  @Override
  public int hashCode() {
    return PointcutAdvisor.class.hashCode();
  }

}
