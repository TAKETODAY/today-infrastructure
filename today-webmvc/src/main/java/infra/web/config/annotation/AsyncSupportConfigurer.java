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

package infra.web.config.annotation;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import infra.core.task.AsyncTaskExecutor;
import infra.util.CollectionUtils;
import infra.web.async.CallableProcessingInterceptor;
import infra.web.async.DeferredResult;
import infra.web.async.DeferredResultProcessingInterceptor;

/**
 * Helps with configuring options for asynchronous request processing.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/9 10:56
 */
public class AsyncSupportConfigurer {

  protected @Nullable AsyncTaskExecutor taskExecutor;

  protected @Nullable Long timeout;

  protected @Nullable List<CallableProcessingInterceptor> callableInterceptors;

  protected @Nullable List<DeferredResultProcessingInterceptor> deferredResultInterceptors;

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
