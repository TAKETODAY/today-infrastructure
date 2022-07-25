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

package cn.taketoday.aop.aspectj;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.io.Serializable;
import java.lang.reflect.Method;

import cn.taketoday.aop.AfterAdvice;
import cn.taketoday.lang.Nullable;

/**
 * Framework AOP advice wrapping an AspectJ after-throwing advice method.
 *
 * @author Rod Johnson
 * @since 4.0
 */
@SuppressWarnings("serial")
public class AspectJAfterThrowingAdvice extends AbstractAspectJAdvice
        implements MethodInterceptor, AfterAdvice, Serializable {

  public AspectJAfterThrowingAdvice(
          Method aspectJBeforeAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {

    super(aspectJBeforeAdviceMethod, pointcut, aif);
  }

  @Override
  public boolean isBeforeAdvice() {
    return false;
  }

  @Override
  public boolean isAfterAdvice() {
    return true;
  }

  @Override
  public void setThrowingName(String name) {
    setThrowingNameNoCheck(name);
  }

  @Override
  @Nullable
  public Object invoke(MethodInvocation mi) throws Throwable {
    try {
      return mi.proceed();
    }
    catch (Throwable ex) {
      if (shouldInvokeOnThrowing(ex)) {
        invokeAdviceMethod(getJoinPointMatch(), null, ex);
      }
      throw ex;
    }
  }

  /**
   * In AspectJ semantics, after throwing advice that specifies a throwing clause
   * is only invoked if the thrown exception is a subtype of the given throwing type.
   */
  private boolean shouldInvokeOnThrowing(Throwable ex) {
    return getDiscoveredThrowingType().isAssignableFrom(ex.getClass());
  }

}
