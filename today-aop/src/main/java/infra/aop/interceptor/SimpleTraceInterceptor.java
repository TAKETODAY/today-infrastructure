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

import infra.lang.Assert;
import infra.logging.Logger;

/**
 * Simple AOP Alliance {@code MethodInterceptor} that can be introduced
 * in a chain to display verbose trace information about intercepted method
 * invocations, with method entry and method exit info.
 *
 * <p>Consider using {@code CustomizableTraceInterceptor} for more
 * advanced needs.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @author TODAY
 * @see CustomizableTraceInterceptor
 * @since 3.0
 */
@SuppressWarnings("serial")
public class SimpleTraceInterceptor extends AbstractTraceInterceptor {

  /**
   * Create a new SimpleTraceInterceptor with a static logger.
   */
  public SimpleTraceInterceptor() { }

  /**
   * Create a new SimpleTraceInterceptor with dynamic or static logger,
   * according to the given flag.
   *
   * @param useDynamicLogger whether to use a dynamic logger or a static logger
   * @see #setUseDynamicLogger
   */
  public SimpleTraceInterceptor(boolean useDynamicLogger) {
    setUseDynamicLogger(useDynamicLogger);
  }

  @Override
  protected Object invokeUnderTrace(MethodInvocation invocation, Logger logger) throws Throwable {
    String invocationDescription = getInvocationDescription(invocation);
    writeToLog(logger, "Entering " + invocationDescription);
    try {
      Object rval = invocation.proceed();
      writeToLog(logger, "Exiting " + invocationDescription);
      return rval;
    }
    catch (Throwable ex) {
      writeToLog(logger, "Exception thrown in " + invocationDescription, ex);
      throw ex;
    }
  }

  /**
   * Return a description for the given method invocation.
   *
   * @param invocation the invocation to describe
   * @return the description
   */
  protected String getInvocationDescription(MethodInvocation invocation) {
    Object target = invocation.getThis();
    Assert.state(target != null, "Target is required");
    String className = target.getClass().getName();
    return "method '" + invocation.getMethod().getName() + "' of class [" + className + "]";
  }

}
