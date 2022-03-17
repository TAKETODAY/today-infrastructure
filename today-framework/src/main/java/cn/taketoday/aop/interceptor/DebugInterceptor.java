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
package cn.taketoday.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;

/**
 * AOP Alliance {@code MethodInterceptor} that can be introduced in a chain
 * to display verbose information about intercepted invocations to the logger.
 *
 * <p>Logs full invocation details on method entry and method exit,
 * including invocation arguments and invocation count. This is only
 * intended for debugging purposes; use {@code SimpleTraceInterceptor}
 * or {@code CustomizableTraceInterceptor} for pure tracing purposes.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY
 * @see SimpleTraceInterceptor
 * @see CustomizableTraceInterceptor
 * @since 3.0
 */
@SuppressWarnings("serial")
public class DebugInterceptor extends SimpleTraceInterceptor {

  private volatile long count;

  /**
   * Create a new DebugInterceptor with a static logger.
   */
  public DebugInterceptor() { }

  /**
   * Create a new DebugInterceptor with dynamic or static logger,
   * according to the given flag.
   *
   * @param useDynamicLogger whether to use a dynamic logger or a static logger
   * @see #setUseDynamicLogger
   */
  public DebugInterceptor(boolean useDynamicLogger) {
    setUseDynamicLogger(useDynamicLogger);
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    synchronized(this) {
      this.count++;
    }
    return super.invoke(invocation);
  }

  @Override
  protected String getInvocationDescription(MethodInvocation invocation) {
    return invocation + "; count=" + this.count;
  }

  /**
   * Return the number of times this interceptor has been invoked.
   */
  public long getCount() {
    return this.count;
  }

  /**
   * Reset the invocation count to zero.
   */
  public synchronized void resetCount() {
    this.count = 0;
  }

}
