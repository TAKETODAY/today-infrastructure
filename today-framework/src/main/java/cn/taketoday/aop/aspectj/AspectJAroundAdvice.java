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
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.weaver.tools.JoinPointMatch;

import java.io.Serializable;
import java.lang.reflect.Method;

import cn.taketoday.aop.ProxyMethodInvocation;
import cn.taketoday.lang.Nullable;

/**
 * Framework AOP around advice (MethodInterceptor) that wraps
 * an AspectJ advice method. Exposes ProceedingJoinPoint.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
public class AspectJAroundAdvice extends AbstractAspectJAdvice implements MethodInterceptor, Serializable {

  public AspectJAroundAdvice(
          Method aspectJAroundAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {

    super(aspectJAroundAdviceMethod, pointcut, aif);
  }

  @Override
  public boolean isBeforeAdvice() {
    return false;
  }

  @Override
  public boolean isAfterAdvice() {
    return false;
  }

  @Override
  protected boolean supportsProceedingJoinPoint() {
    return true;
  }

  @Override
  @Nullable
  public Object invoke(MethodInvocation mi) throws Throwable {
    if (!(mi instanceof ProxyMethodInvocation pmi)) {
      throw new IllegalStateException("MethodInvocation is not a Framework ProxyMethodInvocation: " + mi);
    }
    ProceedingJoinPoint pjp = lazyGetProceedingJoinPoint(pmi);
    JoinPointMatch jpm = getJoinPointMatch(pmi);
    return invokeAdviceMethod(pjp, jpm, null, null);
  }

  /**
   * Return the ProceedingJoinPoint for the current invocation,
   * instantiating it lazily if it hasn't been bound to the thread already.
   *
   * @param rmi the current Framework AOP ReflectiveMethodInvocation,
   * which we'll use for attribute binding
   * @return the ProceedingJoinPoint to make available to advice methods
   */
  protected ProceedingJoinPoint lazyGetProceedingJoinPoint(ProxyMethodInvocation rmi) {
    return new MethodInvocationProceedingJoinPoint(rmi);
  }

}
