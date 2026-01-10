/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
