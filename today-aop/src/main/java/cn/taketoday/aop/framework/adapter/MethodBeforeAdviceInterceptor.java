/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.aop.framework.adapter;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.io.Serializable;

import cn.taketoday.aop.BeforeAdvice;
import cn.taketoday.aop.MethodBeforeAdvice;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Interceptor to wrap a {@link MethodBeforeAdvice}.
 * <p>Used internally by the AOP framework; application developers should not
 * need to use this class directly.
 *
 * @author Rod Johnson
 * @see AfterReturningAdviceInterceptor
 * @see ThrowsAdviceInterceptor
 */
@SuppressWarnings("serial")
public class MethodBeforeAdviceInterceptor implements MethodInterceptor, BeforeAdvice, Serializable {

  private final MethodBeforeAdvice advice;

  /**
   * Create a new MethodBeforeAdviceInterceptor for the given advice.
   *
   * @param advice the MethodBeforeAdvice to wrap
   */
  public MethodBeforeAdviceInterceptor(MethodBeforeAdvice advice) {
    Assert.notNull(advice, "Advice must not be null");
    this.advice = advice;
  }

  @Override
  @Nullable
  public Object invoke(MethodInvocation mi) throws Throwable {
    this.advice.before(mi);
    return mi.proceed();
  }

}
