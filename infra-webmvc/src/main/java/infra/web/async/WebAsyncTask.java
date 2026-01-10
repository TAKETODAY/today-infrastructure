/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.async;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.Callable;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.core.task.AsyncTaskExecutor;
import infra.lang.Assert;
import infra.web.RequestContext;

/**
 * Holder for a {@link Callable}, a timeout value, and a task executor.
 *
 * @param <V> the value type
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class WebAsyncTask<V> implements BeanFactoryAware {

  private final Callable<V> callable;

  @Nullable
  private Long timeout;

  @Nullable
  private AsyncTaskExecutor executor;

  @Nullable
  private String executorName;

  @Nullable
  private BeanFactory beanFactory;

  @Nullable
  private Callable<V> timeoutCallback;

  @Nullable
  private Callable<V> errorCallback;

  @Nullable
  private Runnable completionCallback;

  /**
   * Create a {@code WebAsyncTask} wrapping the given {@link Callable}.
   *
   * @param callable the callable for concurrent handling
   */
  public WebAsyncTask(Callable<V> callable) {
    Assert.notNull(callable, "Callable is required");
    this.callable = callable;
  }

  /**
   * Create a {@code WebAsyncTask} with a timeout value and a {@link Callable}.
   *
   * @param timeout a timeout value in milliseconds
   * @param callable the callable for concurrent handling
   */
  public WebAsyncTask(@Nullable Long timeout, Callable<V> callable) {
    this(callable);
    this.timeout = timeout;
  }

  /**
   * Create a {@code WebAsyncTask} with a timeout value, an executor name, and a {@link Callable}.
   *
   * @param timeout the timeout value in milliseconds; ignored if {@code null}
   * @param executorName the name of an executor bean to use
   * @param callable the callable for concurrent handling
   */
  public WebAsyncTask(@Nullable Long timeout, String executorName, Callable<V> callable) {
    this(callable);
    Assert.notNull(executorName, "Executor name is required");
    this.executorName = executorName;
    this.timeout = timeout;
  }

  /**
   * Create a {@code WebAsyncTask} with a timeout value, an executor instance, and a Callable.
   *
   * @param timeout the timeout value in milliseconds; ignored if {@code null}
   * @param executor the executor to use
   * @param callable the callable for concurrent handling
   */
  public WebAsyncTask(@Nullable Long timeout, AsyncTaskExecutor executor, Callable<V> callable) {
    this(callable);
    Assert.notNull(executor, "Executor is required");
    this.executor = executor;
    this.timeout = timeout;
  }

  /**
   * Return the {@link Callable} to use for concurrent handling (never {@code null}).
   */
  public Callable<?> getCallable() {
    return this.callable;
  }

  /**
   * Return the timeout value in milliseconds, or {@code null} if no timeout is set.
   */
  @Nullable
  public Long getTimeout() {
    return this.timeout;
  }

  /**
   * A {@link BeanFactory} to use for resolving an executor name.
   * <p>This factory reference will automatically be set when
   * {@code WebAsyncTask} is used within a Web MVC controller.
   */
  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  /**
   * Return the AsyncTaskExecutor to use for concurrent handling,
   * or {@code null} if none specified.
   */
  @Nullable
  public AsyncTaskExecutor getExecutor() {
    if (this.executor != null) {
      return this.executor;
    }
    else if (this.executorName != null) {
      Assert.state(this.beanFactory != null, "BeanFactory is required to look up an executor bean by name");
      return this.beanFactory.getBean(this.executorName, AsyncTaskExecutor.class);
    }
    else {
      return null;
    }
  }

  /**
   * Register code to invoke when the async request times out.
   * <p>This method is called from a container thread when an async request times
   * out before the {@code Callable} has completed. The callback is executed in
   * the same thread and therefore should return without blocking. It may return
   * an alternative value to use, including an {@link Exception} or return
   * {@link CallableProcessingInterceptor#RESULT_NONE RESULT_NONE}.
   */
  public void onTimeout(Callable<V> callback) {
    this.timeoutCallback = callback;
  }

  /**
   * Register code to invoke for an error during async request processing.
   * <p>This method is called from a container thread when an error occurred
   * while processing an async request before the {@code Callable} has
   * completed. The callback is executed in the same thread and therefore
   * should return without blocking. It may return an alternative value to
   * use, including an {@link Exception} or return
   * {@link CallableProcessingInterceptor#RESULT_NONE RESULT_NONE}.
   */
  public void onError(Callable<V> callback) {
    this.errorCallback = callback;
  }

  /**
   * Register code to invoke when the async request completes.
   * <p>This method is called from a container thread when an async request
   * completed for any reason, including timeout and network error.
   */
  public void onCompletion(Runnable callback) {
    this.completionCallback = callback;
  }

  CallableProcessingInterceptor createInterceptor() {
    return new CallableProcessingInterceptor() {
      @Override
      public <T> Object handleTimeout(RequestContext request, Callable<T> task) throws Exception {
        return (timeoutCallback != null ? timeoutCallback.call() : RESULT_NONE);
      }

      @Override
      public <T> Object handleError(RequestContext request, Callable<T> task, Throwable t) throws Exception {
        return (errorCallback != null ? errorCallback.call() : RESULT_NONE);
      }

      @Override
      public <T> void afterCompletion(RequestContext request, Callable<T> task) throws Exception {
        if (completionCallback != null) {
          completionCallback.run();
        }
      }
    };
  }

}
