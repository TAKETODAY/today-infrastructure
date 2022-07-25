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

import org.aopalliance.aop.Advice;

import java.io.Serializable;

import cn.taketoday.aop.Pointcut;

/**
 * Convenient Pointcut-driven Advisor implementation.
 *
 * <p>This is the most commonly used Advisor implementation. It can be used
 * with any pointcut and advice type, except for introductions. There is
 * normally no need to subclass this class, or to implement custom Advisors.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 19:05
 * @see #setPointcut
 * @see #setAdvice
 * @since 3.0
 */
@SuppressWarnings("serial")
public class DefaultPointcutAdvisor
        extends AbstractGenericPointcutAdvisor implements Serializable {

  private Pointcut pointcut = Pointcut.TRUE;

  /**
   * Create an empty DefaultPointcutAdvisor.
   * <p>Advice must be set before use using setter methods.
   * Pointcut will normally be set also, but defaults to {@code Pointcut.TRUE}.
   */
  public DefaultPointcutAdvisor() { }

  /**
   * Create a DefaultPointcutAdvisor that matches all methods.
   * <p>{@code Pointcut.TRUE} will be used as Pointcut.
   *
   * @param advice the Advice to use
   */
  public DefaultPointcutAdvisor(Advice advice) {
    this(Pointcut.TRUE, advice);
  }

  /**
   * Create a DefaultPointcutAdvisor, specifying Pointcut and Advice.
   *
   * @param pointcut the Pointcut targeting the Advice
   * @param advice the Advice to run when Pointcut matches
   */
  public DefaultPointcutAdvisor(Pointcut pointcut, Advice advice) {
    this.pointcut = pointcut;
    setAdvice(advice);
  }

  /**
   * Specify the pointcut targeting the advice.
   * <p>Default is {@code Pointcut.TRUE}.
   *
   * @see #setAdvice
   */
  public void setPointcut(Pointcut pointcut) {
    this.pointcut = (pointcut != null ? pointcut : Pointcut.TRUE);
  }

  @Override
  public Pointcut getPointcut() {
    return this.pointcut;
  }

  @Override
  public String toString() {
    return getClass().getName() + ": pointcut [" + getPointcut() + "]; advice [" + getAdvice() + "]";
  }

}
