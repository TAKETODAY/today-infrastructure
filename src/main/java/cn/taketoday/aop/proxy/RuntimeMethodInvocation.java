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

package cn.taketoday.aop.proxy;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import cn.taketoday.aop.support.RuntimeMethodInterceptor;
import cn.taketoday.context.AttributeAccessorSupport;

/**
 * @author TODAY 2021/2/14 21:43
 * @see RuntimeMethodInterceptor
 * @see AttributeAccessorSupport
 * @since 3.0
 */
public abstract class RuntimeMethodInvocation
        extends AttributeAccessorSupport implements MethodInvocation {

  @Override
  public Object proceed() throws Throwable {
    if (shouldCallJoinPoint()) {
      // join-point
      return invokeJoinPoint();
    }

    final MethodInterceptor interceptor = currentMethodInterceptor();
    if (interceptor instanceof RuntimeMethodInterceptor) {
      // runtime
      final RuntimeMethodInterceptor runtimeInterceptor = (RuntimeMethodInterceptor) interceptor;
      if (matchesRuntime(runtimeInterceptor)) {
        return runtimeInterceptor.invoke(this);
      }
      else {
        // next in the chain.
        return proceed();
      }
    }
    // It's an interceptor, so we just invoke it: The pointcut will have
    // been evaluated statically before this object was constructed.
    return interceptor.invoke(this);
  }

  protected abstract Object invokeJoinPoint();

  protected abstract boolean shouldCallJoinPoint();

  protected abstract MethodInterceptor currentMethodInterceptor();

  protected abstract boolean matchesRuntime(RuntimeMethodInterceptor runtimeInterceptor);

}
