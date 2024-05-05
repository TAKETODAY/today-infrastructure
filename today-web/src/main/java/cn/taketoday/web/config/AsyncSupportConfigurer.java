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

package cn.taketoday.web.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import cn.taketoday.core.task.AsyncTaskExecutor;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.async.CallableProcessingInterceptor;
import cn.taketoday.web.async.DeferredResult;
import cn.taketoday.web.async.DeferredResultProcessingInterceptor;

/**
 * Helps with configuring options for asynchronous request processing.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/9 10:56
 */
public class AsyncSupportConfigurer {

  @Nullable
  protected AsyncTaskExecutor taskExecutor;

  @Nullable
  protected Long timeout;

  @Nullable
  protected List<CallableProcessingInterceptor> callableInterceptors;

  @Nullable
  protected List<DeferredResultProcessingInterceptor> deferredResultInterceptors;

  /**
   * The provided task executor is used for the following:
   * <ol>
   * <li>Handle {@link Callable} controller method return values.
   * <li>Perform blocking writes when streaming to the response
   * through a reactive (e.g. Reactor, RxJava) controller method return value.
   * </ol>
   * <p>If your application has controllers with such return types, please
   * configure an {@link AsyncTaskExecutor} as the one used by default is not
   * suitable for production under load.
   *
   * @param taskExecutor the task executor instance to use by default
   */
  public AsyncSupportConfigurer setTaskExecutor(AsyncTaskExecutor taskExecutor) {
    this.taskExecutor = taskExecutor;
    return this;
  }

  /**
   * Specify the amount of time, in milliseconds, before asynchronous request
   * handling times out. the timeout begins after the main request
   * processing thread has exited and ends when the request is dispatched again
   * for further processing of the concurrently produced result.
   * <p>If this value is not set, the default timeout of the underlying
   * implementation is used.
   *
   * @param timeout the timeout value in milliseconds
   */
  public AsyncSupportConfigurer setDefaultTimeout(long timeout) {
    this.timeout = timeout;
    return this;
  }

  /**
   * Configure lifecycle interceptors with callbacks around concurrent request
   * execution that starts when a controller returns a
   * {@link java.util.concurrent.Callable}.
   *
   * @param interceptors the interceptors to register
   */
  public AsyncSupportConfigurer registerCallableInterceptors(CallableProcessingInterceptor... interceptors) {
    if (callableInterceptors == null) {
      callableInterceptors = new ArrayList<>();
    }
    CollectionUtils.addAll(callableInterceptors, interceptors);
    return this;
  }

  /**
   * Configure lifecycle interceptors with callbacks around concurrent request
   * execution that starts when a controller returns a {@link DeferredResult}.
   *
   * @param interceptors the interceptors to register
   */
  public AsyncSupportConfigurer registerDeferredResultInterceptors(DeferredResultProcessingInterceptor... interceptors) {
    if (deferredResultInterceptors == null) {
      deferredResultInterceptors = new ArrayList<>();
    }
    CollectionUtils.addAll(deferredResultInterceptors, interceptors);
    return this;
  }

}
