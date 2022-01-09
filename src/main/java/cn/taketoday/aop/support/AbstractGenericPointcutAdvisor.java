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

import cn.taketoday.aop.PointcutAdvisor;

/**
 * Abstract generic {@link PointcutAdvisor}
 * that allows for any {@link Advice} to be configured.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setAdvice
 * @see DefaultPointcutAdvisor
 * @since 4.0 2022/1/9 23:05
 */
@SuppressWarnings("serial")
public abstract class AbstractGenericPointcutAdvisor extends AbstractPointcutAdvisor {

  private Advice advice = EMPTY_ADVICE;

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
  public String toString() {
    return getClass().getName() + ": advice [" + getAdvice() + "]";
  }

}
