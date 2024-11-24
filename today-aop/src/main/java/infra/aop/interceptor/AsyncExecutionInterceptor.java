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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import infra.aop.support.AopUtils;
import infra.beans.factory.BeanFactory;
import infra.core.BridgeMethodResolver;
import infra.core.Ordered;
import infra.core.task.AsyncTaskExecutor;
import infra.core.task.SimpleAsyncTaskExecutor;
import infra.core.task.TaskExecutor;
import infra.lang.Nullable;

/**
 * AOP Alliance {@code MethodInterceptor} that processes method invocations
 * asynchronously, using a given {@link AsyncTaskExecutor}.
 * Typically used with the {@link infra.scheduling.annotation.Async} annotation.
 *
 * <p>In terms of target method signatures, any parameter types are supported.
 * However, the return type is constrained to either {@code void} or
 * {@code java.util.concurrent.Future}. In the latter case, the Future handle
 * returned from the proxy will be an actual asynchronous Future that can be used
 * to track the result of the asynchronous method execution. However, since the
 * target method needs to implement the same signature, it will have to return
 * a temporary Future handle that just passes the return value through
 * (like  {@link infra.scheduling.annotation.AsyncResult}
 * or EJB's {@code jakarta.ejb.AsyncResult}).
 *
 * <p>When the return type is {@code java.util.concurrent.Future}, any exception thrown
 * during the execution can be accessed and managed by the caller. With {@code void}
 * return type however, such exceptions cannot be transmitted back. In that case an
 * {@link AsyncUncaughtExceptionHandler} can be registered to process such exceptions.
 *
 * <p>The {@code AnnotationAsyncExecutionInterceptor} subclass is preferred for use
 * due to its support for executor qualification in conjunction with {@code @Async} annotation.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.scheduling.annotation.Async
 * @see infra.scheduling.annotation.AsyncAnnotationAdvisor
 * @see infra.scheduling.annotation.AnnotationAsyncExecutionInterceptor
 * @since 4.0
 */
public class AsyncExecutionInterceptor extends AsyncExecutionAspectSupport implements MethodInterceptor, Ordered {

  /**
   * Create a new instance with a default {@link AsyncUncaughtExceptionHandler}.
   *
   * @param defaultExecutor the {@link Executor} (typically a Framework {@link AsyncTaskExecutor}
   * or {@link java.util.concurrent.ExecutorService}) to delegate to;
   * a local executor for this interceptor will be built otherwise
   */
  public AsyncExecutionInterceptor(@Nullable Executor defaultExecutor) {
    super(defaultExecutor);
  }

  /**
   * Create a new {@code AsyncExecutionInterceptor}.
   *
   * @param defaultExecutor the {@link Executor} (typically a Framework {@link AsyncTaskExecutor}
   * or {@link java.util.concurrent.ExecutorService}) to delegate to;
   * a local executor for this interceptor will be built otherwise
   * @param exceptionHandler the {@link AsyncUncaughtExceptionHandler} to use
   */
  public AsyncExecutionInterceptor(@Nullable Executor defaultExecutor, AsyncUncaughtExceptionHandler exceptionHandler) {
    super(defaultExecutor, exceptionHandler);
  }

  /**
   * Intercept the given method invocation, submit the actual calling of the method to
   * the correct task executor and return immediately to the caller.
   *
   * @param invocation the method to intercept and make asynchronous
   * @return {@link Future} if the original method returns {@code Future}; {@code null}
   * otherwise.
   */
  @Override
  @Nullable
  public Object invoke(final MethodInvocation invocation) throws Throwable {
    Class<?> targetClass = invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null;
    final Method userMethod = BridgeMethodResolver.getMostSpecificMethod(invocation.getMethod(), targetClass);

    AsyncTaskExecutor executor = determineAsyncExecutor(userMethod);
    if (executor == null) {
      throw new IllegalStateException(
              "No executor specified and no default executor set on AsyncExecutionInterceptor either");
    }

    Callable<Object> task = () -> {
      try {
        Object result = invocation.proceed();
        if (result instanceof Future) {
          return ((Future<?>) result).get();
        }
      }
      catch (ExecutionException ex) {
        handleError(ex.getCause(), userMethod, invocation.getArguments());
      }
      catch (Throwable ex) {
        handleError(ex, userMethod, invocation.getArguments());
      }
      return null;
    };

    return doSubmit(task, executor, invocation.getMethod().getReturnType());
  }

  /**
   * Subclasses may override to provide support for extracting qualifier information,
   * e.g. via an annotation on the given method.
   *
   * @return always {@code null}
   * @see #determineAsyncExecutor(Method)
   */
  @Override
  @Nullable
  protected String getExecutorQualifier(Method method) {
    return null;
  }

  /**
   * This implementation searches for a unique {@link TaskExecutor}
   * bean in the context, or for an {@link Executor} bean named "taskExecutor" otherwise.
   * If neither of the two is resolvable (e.g. if no {@code BeanFactory} was configured at all),
   * this implementation falls back to a newly created {@link SimpleAsyncTaskExecutor} instance
   * for local use if no default could be found.
   *
   * @see #DEFAULT_TASK_EXECUTOR_BEAN_NAME
   */
  @Override
  @Nullable
  protected Executor getDefaultExecutor(@Nullable BeanFactory beanFactory) {
    Executor defaultExecutor = super.getDefaultExecutor(beanFactory);
    return defaultExecutor != null ? defaultExecutor : new SimpleAsyncTaskExecutor();
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

}
