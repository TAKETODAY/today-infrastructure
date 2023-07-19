/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.util.List;
import java.util.concurrent.Callable;

import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.task.AsyncTaskExecutor;
import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.RequestContext;

/**
 * WebAsyncManager Factory
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/3 22:26
 */
public class WebAsyncManagerFactory {
  private static final Logger log = LoggerFactory.getLogger(WebAsyncManagerFactory.class);

  private CallableProcessingInterceptor[] callableInterceptors = new CallableProcessingInterceptor[0];

  private DeferredResultProcessingInterceptor[] deferredResultInterceptors = new DeferredResultProcessingInterceptor[0];

  @Nullable
  private Long asyncRequestTimeout;

  private AsyncTaskExecutor taskExecutor = new MvcSimpleAsyncTaskExecutor();

  // Async

  /**
   * Set the default {@link AsyncTaskExecutor} to use when a controller method
   * return a {@link Callable}. Controller methods can override this default on
   * a per-request basis by returning an {@link WebAsyncTask}.
   * <p>By default a {@link SimpleAsyncTaskExecutor} instance is used.
   * It's recommended to change that default in production as the simple executor
   * does not re-use threads.
   */
  public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
    this.taskExecutor = taskExecutor;
  }

  /**
   * Specify the amount of time, in milliseconds, before concurrent handling
   * should time out. In Servlet 3, the timeout begins after the main request
   * processing thread has exited and ends when the request is dispatched again
   * for further processing of the concurrently produced result.
   * <p>If this value is not set, the default timeout of the underlying
   * implementation is used.
   *
   * @param timeout the timeout value in milliseconds
   */
  public void setAsyncRequestTimeout(long timeout) {
    this.asyncRequestTimeout = timeout;
  }

  /**
   * Configure {@code CallableProcessingInterceptor}'s to register on async requests.
   *
   * @param interceptors the interceptors to register
   */
  public void setCallableInterceptors(List<CallableProcessingInterceptor> interceptors) {
    this.callableInterceptors = interceptors.toArray(new CallableProcessingInterceptor[0]);
  }

  /**
   * Configure {@code DeferredResultProcessingInterceptor}'s to register on async requests.
   *
   * @param interceptors the interceptors to register
   */
  public void setDeferredResultInterceptors(List<DeferredResultProcessingInterceptor> interceptors) {
    this.deferredResultInterceptors = interceptors.toArray(new DeferredResultProcessingInterceptor[0]);
  }

  public WebAsyncManager getWebAsyncManager(RequestContext request) {
    AsyncWebRequest asyncWebRequest = request.getAsyncWebRequest();
    asyncWebRequest.setTimeout(asyncRequestTimeout);

    WebAsyncManager asyncManager = new WebAsyncManager(request);
    asyncManager.setTaskExecutor(taskExecutor);
    asyncManager.setAsyncRequest(asyncWebRequest);
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
      if (taskExecutorWarning && log.isWarnEnabled()) {
        synchronized(this) {
          if (taskExecutorWarning) {
            log.warn("""
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
      super.execute(task);
    }
  }

}
