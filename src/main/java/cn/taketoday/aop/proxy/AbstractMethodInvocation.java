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

import cn.taketoday.aop.TargetClassAware;
import cn.taketoday.aop.support.RuntimeMethodInterceptor;
import cn.taketoday.context.AttributeAccessorSupport;

/**
 * Implemented basic {@link #proceed()} logic
 *
 * <p>
 * Runtime {@link MethodInterceptor} will automatically match current {@link MethodInvocation}
 * </p>
 *
 * @author TODAY 2021/2/14 21:43
 * @see RuntimeMethodInterceptor
 * @see AttributeAccessorSupport
 * @since 3.0
 */
public abstract class AbstractMethodInvocation
        extends AttributeAccessorSupport implements MethodInvocation, TargetClassAware {

  /**
   * Basic {@link #proceed()} logic
   *
   * <p>
   * Subclasses can override this method to handle {@link Exception}
   * </p>
   *
   * @return
   *
   * @throws Throwable
   * @see cn.taketoday.aop.proxy.CglibAopProxy.CglibMethodInvocation
   * @see DefaultMethodInvocation
   * @see StandardMethodInvocation
   */
  @Override
  public Object proceed() throws Throwable {
    if (hasInterceptor()) {
      // It's an interceptor, so we just invoke it
      // runtime interceptor will automatically matches MethodInvocation
      return executeInterceptor();
    }
    // join-point
    return invokeJoinPoint();
  }

  /**
   * Invoke jon-point
   *
   * @return the result of the call to {@link MethodInvocation#proceed()}, might be
   * intercepted by the interceptor.
   */
  protected abstract Object invokeJoinPoint();

  /**
   * Determine whether there is an interceptor
   */
  protected abstract boolean hasInterceptor();

  /**
   * Invoke current {@link MethodInterceptor}
   * <p>
   * {@link #hasInterceptor()} must returns{@code true}
   * </p>
   *
   * @throws Throwable
   *         if the interceptors or the target-object throws an exception.
   * @see #hasInterceptor()
   */
  protected abstract Object executeInterceptor() throws Throwable;

}
