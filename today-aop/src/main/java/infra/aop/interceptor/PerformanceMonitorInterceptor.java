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

package infra.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;

import infra.logging.Logger;
import infra.util.StopWatch;

/**
 * Simple AOP Alliance {@code MethodInterceptor} for performance monitoring.
 * This interceptor has no effect on the intercepted method call.
 *
 * <p>Uses a {@code StopWatch} for the actual performance measuring.
 *
 * @author Rod Johnson
 * @author Dmitriy Kopylenko
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see StopWatch
 * @since 3.0
 */
@SuppressWarnings("serial")
public class PerformanceMonitorInterceptor extends AbstractMonitoringInterceptor {

  /**
   * Create a new PerformanceMonitorInterceptor with a static logger.
   */
  public PerformanceMonitorInterceptor() { }

  /**
   * Create a new PerformanceMonitorInterceptor with a dynamic or static logger,
   * according to the given flag.
   *
   * @param useDynamicLogger whether to use a dynamic logger or a static logger
   * @see #setUseDynamicLogger
   */
  public PerformanceMonitorInterceptor(boolean useDynamicLogger) {
    setUseDynamicLogger(useDynamicLogger);
  }

  @Override
  protected Object invokeUnderTrace(MethodInvocation invocation, Logger logger) throws Throwable {
    String name = createInvocationTraceName(invocation);
    StopWatch stopWatch = new StopWatch(name);
    stopWatch.start(name);
    try {
      return invocation.proceed();
    }
    finally {
      stopWatch.stop();
      writeToLog(logger, stopWatch.shortSummary());
    }
  }

}
