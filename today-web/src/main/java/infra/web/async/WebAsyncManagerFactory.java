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

package infra.web.async;

import java.util.List;
import java.util.concurrent.Callable;

import infra.beans.factory.BeanFactoryUtils;
import infra.context.ApplicationContext;
import infra.core.task.AsyncTaskExecutor;
import infra.core.task.SimpleAsyncTaskExecutor;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.RequestContext;

/**
 * WebAsyncManager Factory
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/3 22:26
 */
public class WebAsyncManagerFactory {

  @Nullable
  private List<CallableProcessingInterceptor> callableInterceptors;

  @Nullable
  private List<DeferredResultProcessingInterceptor> deferredResultInterceptors;

  @Nullable
  private Long asyncRequestTimeout;

  private AsyncTaskExecutor taskExecutor = new MvcSimpleAsyncTaskExecutor();

  // Async

  /**
   * Set the default {@link AsyncTaskExecutor} to use when a controller method
   * return a {@link Callable}. Controller methods can override this default on
   * a per-request basis by returning an {@link WebAsyncTask}.
   * <p>If your application has controllers with such return types, please
   * configure an {@link AsyncTaskExecutor} as the one used by default is not
   * suitable for production under load.
   */
  public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
    this.taskExecutor = taskExecutor;
  }

  /**
   * Specify the amount of time, in milliseconds, before concurrent handling
   * should time out. the timeout begins after the main request
   * processing thread has exited and ends when the request is dispatched again
   * for further processing of the concurrently produced result.
   * <p>If this value is not set, the default timeout of the underlying
   * implementation is used.
   *
   * @param timeout the timeout value in milliseconds
   */
  public void setAsyncRequestTimeout(@Nullable Long timeout) {
    this.asyncRequestTimeout = timeout;
  }

  /**
   * Configure {@code CallableProcessingInterceptor}'s to register on async requests.
   *
   * @param interceptors the interceptors to register
   */
  public void setCallableInterceptors(@Nullable List<CallableProcessingInterceptor> interceptors) {
    this.callableInterceptors = interceptors;
  }

  /**
   * Configure {@code DeferredResultProcessingInterceptor}'s to register on async requests.
   *
   * @param interceptors the interceptors to register
   */
  public void setDeferredResultInterceptors(@Nullable List<DeferredResultProcessingInterceptor> interceptors) {
    this.deferredResultInterceptors = interceptors;
  }

  public WebAsyncManager getWebAsyncManager(RequestContext request) {
    WebAsyncManager asyncManager = new WebAsyncManager(request);
    asyncManager.setTaskExecutor(taskExecutor);
    asyncManager.setTimeout(asyncRequestTimeout);
    asyncManager.registerCallableInterceptors(callableInterceptors);
    asyncManager.registerDeferredResultInterceptors(deferredResultInterceptors);
    return asyncManager;
  }

  public static WebAsyncManagerFactory find(ApplicationContext context) {
    WebAsyncManagerFactory factory = BeanFactoryUtils.find(context, WebAsyncManagerFactory.class);
    if (factory != null) {
      return factory;
    }
    return new WebAsyncManagerFactory();
  }

  /**
   * A default MVC AsyncTaskExecutor that warns if used.
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
