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

package cn.taketoday.web.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import cn.taketoday.core.task.AsyncTaskExecutor;
import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.lang.Nullable;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskExecutor;
import cn.taketoday.web.context.async.CallableProcessingInterceptor;
import cn.taketoday.web.context.async.DeferredResult;
import cn.taketoday.web.context.async.DeferredResultProcessingInterceptor;

/**
 * Helps with configuring options for asynchronous request processing.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/9 10:56
 */
public class AsyncSupportConfigurer {

  @Nullable
  private AsyncTaskExecutor taskExecutor;

  @Nullable
  private Long timeout;

  private final List<CallableProcessingInterceptor> callableInterceptors = new ArrayList<>();

  private final List<DeferredResultProcessingInterceptor> deferredResultInterceptors = new ArrayList<>();

  /**
   * The provided task executor is used to:
   * <ol>
   * <li>Handle {@link Callable} controller method return values.
   * <li>Perform blocking writes when streaming to the response
   * through a reactive (e.g. Reactor, RxJava) controller method return value.
   * </ol>
   * <p>By default only a {@link SimpleAsyncTaskExecutor} is used. However when
   * using the above two use cases, it's recommended to configure an executor
   * backed by a thread pool such as {@link ThreadPoolTaskExecutor}.
   *
   * @param taskExecutor the task executor instance to use by default
   */
  public AsyncSupportConfigurer setTaskExecutor(AsyncTaskExecutor taskExecutor) {
    this.taskExecutor = taskExecutor;
    return this;
  }

  /**
   * Specify the amount of time, in milliseconds, before asynchronous request
   * handling times out. In Servlet 3, the timeout begins after the main request
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
    this.callableInterceptors.addAll(Arrays.asList(interceptors));
    return this;
  }

  /**
   * Configure lifecycle interceptors with callbacks around concurrent request
   * execution that starts when a controller returns a {@link DeferredResult}.
   *
   * @param interceptors the interceptors to register
   */
  public AsyncSupportConfigurer registerDeferredResultInterceptors(
          DeferredResultProcessingInterceptor... interceptors) {

    this.deferredResultInterceptors.addAll(Arrays.asList(interceptors));
    return this;
  }

  @Nullable
  protected AsyncTaskExecutor getTaskExecutor() {
    return this.taskExecutor;
  }

  @Nullable
  protected Long getTimeout() {
    return this.timeout;
  }

  protected List<CallableProcessingInterceptor> getCallableInterceptors() {
    return this.callableInterceptors;
  }

  protected List<DeferredResultProcessingInterceptor> getDeferredResultInterceptors() {
    return this.deferredResultInterceptors;
  }

}
