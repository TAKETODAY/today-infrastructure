/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.async;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

import infra.beans.factory.BeanFactoryUtils;
import infra.context.ApplicationContext;
import infra.core.task.AsyncTaskExecutor;
import infra.core.task.SimpleAsyncTaskExecutor;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.HttpContext;
import infra.web.util.TimeoutUtils;

/**
 * Creates a {@link WebAsyncManager} for each request, applying the configured
 * task executor, default timeout, and processing interceptors.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/3 22:26
 */
public class WebAsyncManagerFactory {

  private @Nullable List<CallableProcessingInterceptor> callableInterceptors;

  private @Nullable List<DeferredResultProcessingInterceptor> deferredResultInterceptors;

  private @Nullable Duration asyncRequestTimeout;

  private AsyncTaskExecutor taskExecutor = new MvcSimpleAsyncTaskExecutor();

  // Async

  /**
   * Set the default executor for asynchronous {@link Callable} processing.
   * A controller can override this default for a request by returning a
   * {@link WebAsyncTask} with its own executor.
   * <p>Configure an executor for production use; the built-in
   * {@link SimpleAsyncTaskExecutor} is not suitable under load.
   *
   * @param taskExecutor the executor to use by default
   */
  public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
    this.taskExecutor = taskExecutor;
  }

  /**
   * Set the default timeout for asynchronous request processing.
   * A timeout specified by a {@link WebAsyncTask} or {@link DeferredResult}
   * takes precedence over this default.
   * <p>If no timeout is specified for the request and this default is
   * {@code null}, the underlying {@link WebAsyncRequest} retains its timeout.
   * A value of {@link Duration#ZERO} is passed to the underlying request,
   * which determines how to interpret it.
   *
   * @param timeout the default timeout, or {@code null} to clear it
   * @throws IllegalArgumentException if the timeout is negative, positive but
   * less than one millisecond, or too large to convert to milliseconds
   */
  public void setAsyncRequestTimeout(@Nullable Duration timeout) {
    TimeoutUtils.validate(timeout);
    this.asyncRequestTimeout = timeout;
  }

  /**
   * Set the interceptors to register with each manager for asynchronous
   * {@link Callable} processing.
   *
   * @param interceptors the interceptors to register, or {@code null} for none
   */
  public void setCallableInterceptors(@Nullable List<CallableProcessingInterceptor> interceptors) {
    this.callableInterceptors = interceptors;
  }

  /**
   * Set the interceptors to register with each manager for asynchronous
   * {@link DeferredResult} processing.
   *
   * @param interceptors the interceptors to register, or {@code null} for none
   */
  public void setDeferredResultInterceptors(@Nullable List<DeferredResultProcessingInterceptor> interceptors) {
    this.deferredResultInterceptors = interceptors;
  }

  /**
   * Create a new manager for the given request using this factory's configuration.
   *
   * @param request the HTTP context for the request
   * @return the configured manager
   */
  public WebAsyncManager createWebAsyncManager(HttpContext request) {
    WebAsyncManager asyncManager = new WebAsyncManager(request);
    asyncManager.setTaskExecutor(taskExecutor);
    asyncManager.setTimeout(asyncRequestTimeout);
    asyncManager.registerCallableInterceptors(callableInterceptors);
    asyncManager.registerDeferredResultInterceptors(deferredResultInterceptors);
    return asyncManager;
  }

  /**
   * Look up a factory bean by type in the given application context.
   *
   * @param context the application context to search
   * @return the resolved factory bean, or a new factory with default settings
   * if no bean is found
   */
  public static WebAsyncManagerFactory find(ApplicationContext context) {
    WebAsyncManagerFactory factory = BeanFactoryUtils.find(context, WebAsyncManagerFactory.class);
    if (factory != null) {
      return factory;
    }
    return new WebAsyncManagerFactory();
  }

  /**
   * The default MVC executor, which warns against using it in production.
   */
  @SuppressWarnings("serial")
  private static class MvcSimpleAsyncTaskExecutor extends SimpleAsyncTaskExecutor {

    private static boolean taskExecutorWarning = true;

    public MvcSimpleAsyncTaskExecutor() {
      super("MvcAsync");
    }

    @Override
    public void execute(Runnable task) {
      if (taskExecutorWarning) {
        Logger logger = LoggerFactory.getLogger(WebAsyncManagerFactory.class);
        if (logger.isWarnEnabled()) {
          synchronized(this) {
            if (taskExecutorWarning) {
              logger.warn("""
                      !!!
                      Performing asynchronous handling through the default Web MVC SimpleAsyncTaskExecutor.
                      This executor is not suitable for production use under load.
                      Please, configure an AsyncTaskExecutor through the WebMvc config.
                      -------------------------------
                      !!!""");
              taskExecutorWarning = false;
            }
          }
        }
      }
      super.execute(task);
    }
  }

}
