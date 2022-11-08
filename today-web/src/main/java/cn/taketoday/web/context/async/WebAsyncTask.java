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

package cn.taketoday.web.context.async;

import java.util.concurrent.Callable;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.core.task.AsyncTaskExecutor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * Holder for a {@link Callable}, a timeout value, and a task executor.
 *
 * @param <V> the value type
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class WebAsyncTask<V> implements BeanFactoryAware {

  private final Callable<V> callable;

  private Long timeout;

  private AsyncTaskExecutor executor;

  private String executorName;

  private BeanFactory beanFactory;

  private Callable<V> timeoutCallback;

  private Callable<V> errorCallback;

  private Runnable completionCallback;

  /**
   * Create a {@code WebAsyncTask} wrapping the given {@link Callable}.
   *
   * @param callable the callable for concurrent handling
   */
  public WebAsyncTask(Callable<V> callable) {
    Assert.notNull(callable, "Callable must not be null");
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
    Assert.notNull(executorName, "Executor name must not be null");
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
    Assert.notNull(executor, "Executor must not be null");
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

  CallableProcessingInterceptor getInterceptor() {
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
