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

package infra.aop.framework.adapter;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import infra.aop.AfterAdvice;
import infra.aop.framework.AopConfigException;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

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
 *
 * <p>This is a framework class that need not be used directly by Framework users.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MethodBeforeAdviceInterceptor
 * @see AfterReturningAdviceInterceptor
 * @since 4.0
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
   * (usually a {@link infra.aop.ThrowsAdvice} implementation)
   */
  public ThrowsAdviceInterceptor(Object throwsAdvice) {
    Assert.notNull(throwsAdvice, "Advice is required");
    this.throwsAdvice = throwsAdvice;

    Method[] methods = throwsAdvice.getClass().getMethods();
    for (Method method : methods) {
      if (method.getName().equals(AFTER_THROWING)) {
        Class<?> throwableParam = null;
        if (method.getParameterCount() == 1) {
          // just a Throwable parameter
          throwableParam = method.getParameterTypes()[0];
          if (!Throwable.class.isAssignableFrom(throwableParam)) {
            throw new AopConfigException("Invalid afterThrowing signature: " +
                "single argument must be a Throwable subclass");
          }
        }
        else if (method.getParameterCount() == 4) {
          // Method, Object[], target, throwable
          Class<?>[] paramTypes = method.getParameterTypes();
          if (!Method.class.equals(paramTypes[0])
              || !Object[].class.equals(paramTypes[1])
              || Throwable.class.equals(paramTypes[2])
              || !Throwable.class.isAssignableFrom(paramTypes[3])) {
            throw new AopConfigException("Invalid afterThrowing signature: " +
                "four arguments must be Method, Object[], target, throwable: " + method);
          }
          throwableParam = paramTypes[3];
        }
        if (throwableParam == null) {
          throw new AopConfigException("Unsupported afterThrowing signature: single throwable argument " +
              "or four arguments Method, Object[], target, throwable expected: " + method);
        }
        // An exception handler to register...
        Method existingMethod = this.exceptionHandlerMap.put(throwableParam, method);
        if (existingMethod != null) {
          throw new AopConfigException("Only one afterThrowing method per specific Throwable subclass " +
              "allowed: " + method + " / " + existingMethod);
        }
        if (logger.isDebugEnabled()) {
          logger.debug("Found exception handler method on throws advice: {}", method);
        }
      }
    }

    if (this.exceptionHandlerMap.isEmpty()) {
      throw new AopConfigException(
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
