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

package cn.taketoday.aop.framework.adapter;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.aop.AfterAdvice;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Interceptor to wrap an after-throwing advice.
 *
 * <p>The signatures on handler methods on the {@code ThrowsAdvice}
 * implementation method argument must be of the form:<br>
 *
 * {@code void afterThrowing([Method, args, target], ThrowableSubclass);}
 *
 * <p>Only the last argument is required.
 *
 * <p>Some examples of valid methods would be:
 *
 * <pre class="code">public void afterThrowing(Exception ex)</pre>
 * <pre class="code">public void afterThrowing(RemoteException)</pre>
 * <pre class="code">public void afterThrowing(Method method, Object[] args, Object target, Exception ex)</pre>
 * <pre class="code">public void afterThrowing(Method method, Object[] args, Object target, ServletException ex)</pre>
 *
 * <p>This is a framework class that need not be used directly by Spring users.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see MethodBeforeAdviceInterceptor
 * @see AfterReturningAdviceInterceptor
 */
public class ThrowsAdviceInterceptor implements MethodInterceptor, AfterAdvice {

  private static final String AFTER_THROWING = "afterThrowing";

  private static final Logger logger = LoggerFactory.getLogger(ThrowsAdviceInterceptor.class);

  private final Object throwsAdvice;

  /** Methods on throws advice, keyed by exception class. */
  private final Map<Class<?>, Method> exceptionHandlerMap = new HashMap<>();

  /**
   * Create a new ThrowsAdviceInterceptor for the given ThrowsAdvice.
   *
   * @param throwsAdvice the advice object that defines the exception handler methods
   * (usually a {@link cn.taketoday.aop.ThrowsAdvice} implementation)
   */
  public ThrowsAdviceInterceptor(Object throwsAdvice) {
    Assert.notNull(throwsAdvice, "Advice must not be null");
    this.throwsAdvice = throwsAdvice;

    Method[] methods = throwsAdvice.getClass().getMethods();
    for (Method method : methods) {
      if (method.getName().equals(AFTER_THROWING) &&
              (method.getParameterCount() == 1 || method.getParameterCount() == 4)) {
        Class<?> throwableParam = method.getParameterTypes()[method.getParameterCount() - 1];
        if (Throwable.class.isAssignableFrom(throwableParam)) {
          // An exception handler to register...
          this.exceptionHandlerMap.put(throwableParam, method);
          if (logger.isDebugEnabled()) {
            logger.debug("Found exception handler method on throws advice: " + method);
          }
        }
      }
    }

    if (this.exceptionHandlerMap.isEmpty()) {
      throw new IllegalArgumentException(
              "At least one handler method must be found in class [" + throwsAdvice.getClass() + "]");
    }
  }

  /**
   * Return the number of handler methods in this advice.
   */
  public int getHandlerMethodCount() {
    return this.exceptionHandlerMap.size();
  }

  @Override
  @Nullable
  public Object invoke(MethodInvocation mi) throws Throwable {
    try {
      return mi.proceed();
    }
    catch (Throwable ex) {
      Method handlerMethod = getExceptionHandler(ex);
      if (handlerMethod != null) {
        invokeHandlerMethod(mi, ex, handlerMethod);
      }
      throw ex;
    }
  }

  /**
   * Determine the exception handle method for the given exception.
   *
   * @param exception the exception thrown
   * @return a handler for the given exception type, or {@code null} if none found
   */
  @Nullable
  private Method getExceptionHandler(Throwable exception) {
    Class<?> exceptionClass = exception.getClass();
    if (logger.isTraceEnabled()) {
      logger.trace("Trying to find handler for exception of type [" + exceptionClass.getName() + "]");
    }
    Method handler = this.exceptionHandlerMap.get(exceptionClass);
    while (handler == null && exceptionClass != Throwable.class) {
      exceptionClass = exceptionClass.getSuperclass();
      handler = this.exceptionHandlerMap.get(exceptionClass);
    }
    if (handler != null && logger.isTraceEnabled()) {
      logger.trace("Found handler for exception of type [" + exceptionClass.getName() + "]: " + handler);
    }
    return handler;
  }

  private void invokeHandlerMethod(MethodInvocation mi, Throwable ex, Method method) throws Throwable {
    Object[] handlerArgs;
    if (method.getParameterCount() == 1) {
      handlerArgs = new Object[] { ex };
    }
    else {
      handlerArgs = new Object[] { mi.getMethod(), mi.getArguments(), mi.getThis(), ex };
    }
    try {
      method.invoke(this.throwsAdvice, handlerArgs);
    }
    catch (InvocationTargetException targetEx) {
      throw targetEx.getTargetException();
    }
  }

}
