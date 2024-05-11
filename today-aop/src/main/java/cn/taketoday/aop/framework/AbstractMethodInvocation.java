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

package cn.taketoday.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.util.HashMap;

import cn.taketoday.aop.ProxyMethodInvocation;
import cn.taketoday.aop.support.RuntimeMethodInterceptor;
import cn.taketoday.core.AttributeAccessorSupport;
import cn.taketoday.util.ObjectUtils;

/**
 * Implemented basic {@link #proceed()} logic
 *
 * <p>
 * Runtime {@link MethodInterceptor} will automatically match current {@link MethodInvocation}
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RuntimeMethodInterceptor
 * @see AttributeAccessorSupport
 * @since 3.0 2021/2/14 21:43
 */
public abstract class AbstractMethodInvocation extends AttributeAccessorSupport
        implements MethodInvocation, Cloneable, ProxyMethodInvocation {

  /**
   * Return the proxy that this method invocation was made through.
   *
   * @return the original proxy object
   */
  public abstract Object getProxy();

  /**
   * Basic logic. Proceeds to the next interceptor in the chain.
   * <p>
   * Subclasses can override this method to handle {@link Exception}
   * </p>
   *
   * @return see the children interfaces' proceed definition.
   * @throws Throwable if the join-point throws an exception.
   * @see CglibAopProxy.CglibMethodInvocation
   * @see DefaultMethodInvocation
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
  protected abstract Object invokeJoinPoint() throws Throwable;

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
   * @throws Throwable if the interceptors or the target-object throws an exception.
   * @see #hasInterceptor()
   */
  protected abstract Object executeInterceptor() throws Throwable;

  /**
   * This implementation returns a shallow copy of this invocation object,
   * including an independent copy of the original arguments array.
   * <p>We want a shallow copy in this case: We want to use the same interceptor
   * chain and other object references, but we want an independent value for the
   * current interceptor index.
   *
   * @see java.lang.Object#clone()
   */
  public MethodInvocation invocableClone() {
    Object[] cloneArguments = this.getArguments();
    if (ObjectUtils.isNotEmpty(cloneArguments)) {
      // Build an independent copy of the arguments array.
      cloneArguments = cloneArguments.clone();
    }
    return invocableClone(cloneArguments);
  }

  /**
   * This implementation returns a shallow copy of this invocation object,
   * using the given arguments array for the clone.
   * <p>We want a shallow copy in this case: We want to use the same interceptor
   * chain and other object references, but we want an independent value for the
   * current interceptor index.
   *
   * @see java.lang.Object#clone()
   */
  public MethodInvocation invocableClone(Object... arguments) {
    // Force initialization of the user attributes Map,
    // for having a shared Map reference in the clone.
    if (this.attributes == null) {
      this.attributes = new HashMap<>();
    }

    // Create the MethodInvocation clone.
    try {
      AbstractMethodInvocation clone = (AbstractMethodInvocation) clone();
      clone.setArguments(arguments);
      return clone;
    }
    catch (CloneNotSupportedException ex) {
      throw new IllegalStateException(
              "Should be able to clone object of type [%s]: %s".formatted(getClass(), ex));
    }
  }

  public abstract void setArguments(Object[] arguments);

}
