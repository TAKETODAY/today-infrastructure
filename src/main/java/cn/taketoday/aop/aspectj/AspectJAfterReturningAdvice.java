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

import org.aopalliance.intercept.MethodInvocation;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import cn.taketoday.aop.AfterAdvice;
import cn.taketoday.aop.AfterReturningAdvice;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.TypeUtils;

/**
 * Framework AOP advice wrapping an AspectJ after-returning advice method.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 4.0
 */
@SuppressWarnings("serial")
public class AspectJAfterReturningAdvice extends AbstractAspectJAdvice
        implements AfterReturningAdvice, AfterAdvice, Serializable {

  public AspectJAfterReturningAdvice(
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
  public void setReturningName(String name) {
    setReturningNameNoCheck(name);
  }

  @Override
  public void afterReturning(Object returnValue, MethodInvocation invocation) throws Throwable {
    if (shouldInvokeOnReturnValueOf(invocation.getMethod(), returnValue)) {
      invokeAdviceMethod(getJoinPointMatch(), returnValue, null);
    }
  }

  /**
   * Following AspectJ semantics, if a returning clause was specified, then the
   * advice is only invoked if the returned value is an instance of the given
   * returning type and generic type parameters, if any, match the assignment
   * rules. If the returning type is Object, the advice is *always* invoked.
   *
   * @param returnValue the return value of the target method
   * @return whether to invoke the advice method for the given return value
   */
  private boolean shouldInvokeOnReturnValueOf(Method method, @Nullable Object returnValue) {
    Class<?> type = getDiscoveredReturningType();
    Type genericType = getDiscoveredReturningGenericType();
    // If we aren't dealing with a raw type, check if generic parameters are assignable.
    return (matchesReturnValue(type, method, returnValue) &&
            (genericType == null || genericType == type ||
                    TypeUtils.isAssignable(genericType, method.getGenericReturnType())));
  }

  /**
   * Following AspectJ semantics, if a return value is null (or return type is void),
   * then the return type of target method should be used to determine whether advice
   * is invoked or not. Also, even if the return type is void, if the type of argument
   * declared in the advice method is Object, then the advice must still get invoked.
   *
   * @param type the type of argument declared in advice method
   * @param method the advice method
   * @param returnValue the return value of the target method
   * @return whether to invoke the advice method for the given return value and type
   */
  private boolean matchesReturnValue(Class<?> type, Method method, @Nullable Object returnValue) {
    if (returnValue != null) {
      return ClassUtils.isAssignableValue(type, returnValue);
    }
    else if (Object.class == type && void.class == method.getReturnType()) {
      return true;
    }
    else {
      return ClassUtils.isAssignable(type, method.getReturnType());
    }
  }

}
