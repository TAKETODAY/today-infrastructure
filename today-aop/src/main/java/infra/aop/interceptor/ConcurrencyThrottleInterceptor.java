/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.aop.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;

import infra.util.ConcurrencyThrottleSupport;

/**
 * Interceptor that throttles concurrent access, blocking invocations
 * if a specified concurrency limit is reached.
 *
 * <p>Can be applied to methods of local services that involve heavy use
 * of system resources, in a scenario where it is more efficient to
 * throttle concurrency for a specific service rather than restricting
 * the entire thread pool (e.g. the web container's thread pool).
 *
 * <p>The default concurrency limit of this interceptor is 1.
 * Specify the "concurrencyLimit" bean property to change this value.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/9/11 17:20
 * @see #setConcurrencyLimit
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ConcurrencyThrottleInterceptor extends ConcurrencyThrottleSupport implements MethodInterceptor, Serializable {

  /**
   * Create a default {@code ConcurrencyThrottleInterceptor}
   * with concurrency limit 1.
   */
  public ConcurrencyThrottleInterceptor() {
    this(1);
  }

  /**
   * Create a {@code ConcurrencyThrottleInterceptor}
   * with the given concurrency limit.
   *
   * @since 5.0
   */
  public ConcurrencyThrottleInterceptor(int concurrencyLimit) {
    setConcurrencyLimit(concurrencyLimit);
  }

  @Override
  public @Nullable Object invoke(MethodInvocation methodInvocation) throws Throwable {
    beforeAccess();
    try {
      return methodInvocation.proceed();
    }
    finally {
      afterAccess();
    }
  }

}
