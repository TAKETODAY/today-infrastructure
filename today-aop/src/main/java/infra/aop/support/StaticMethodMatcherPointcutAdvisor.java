/*
 * Copyright 2017 - 2024 the original author or authors.
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

import java.io.Serializable;

import infra.aop.Pointcut;
import infra.aop.PointcutAdvisor;
import infra.core.Ordered;
import infra.lang.Assert;

/**
 * Convenient base class for Advisors that are also static pointcuts.
 * Serializable if Advice and subclass are.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/4 12:11
 * @since 3.0
 */
@SuppressWarnings("serial")
public abstract class StaticMethodMatcherPointcutAdvisor
        extends StaticMethodMatcherPointcut implements PointcutAdvisor, Ordered, Serializable {

  private Advice advice = EMPTY_ADVICE;

  /**
   * Create a new StaticMethodMatcherPointcutAdvisor,
   * expecting bean-style configuration.
   *
   * @see #setAdvice
   */
  public StaticMethodMatcherPointcutAdvisor() { }

  /**
   * Create a new StaticMethodMatcherPointcutAdvisor for the given advice.
   *
   * @param advice the Advice to use
   */
  public StaticMethodMatcherPointcutAdvisor(Advice advice) {
    Assert.notNull(advice, "Advice is required");
    this.advice = advice;
  }

  public void setAdvice(Advice advice) {
    this.advice = advice;
  }

  @Override
  public Advice getAdvice() {
    return this.advice;
  }

  @Override
  public Pointcut getPointcut() {
    return this;
  }

}
