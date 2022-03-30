/*
 * Copyright 2002-2018 the original author or authors.
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

package cn.taketoday.web.context.async;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.RequestContext;

/**
 * Assists with the invocation of {@link CallableProcessingInterceptor}'s.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 4.0
 */
class CallableInterceptorChain {

  private static final Logger logger = LoggerFactory.getLogger(CallableInterceptorChain.class);

  private final List<CallableProcessingInterceptor> interceptors;

  private int preProcessIndex = -1;

  private volatile Future<?> taskFuture;

  public CallableInterceptorChain(List<CallableProcessingInterceptor> interceptors) {
    this.interceptors = interceptors;
  }

  public void setTaskFuture(Future<?> taskFuture) {
    this.taskFuture = taskFuture;
  }

  public void applyBeforeConcurrentHandling(RequestContext request, Callable<?> task) throws Exception {
    for (CallableProcessingInterceptor interceptor : this.interceptors) {
      interceptor.beforeConcurrentHandling(request, task);
    }
  }

  public void applyPreProcess(RequestContext request, Callable<?> task) throws Exception {
    for (CallableProcessingInterceptor interceptor : this.interceptors) {
      interceptor.preProcess(request, task);
      this.preProcessIndex++;
    }
  }

  public Object applyPostProcess(RequestContext request, Callable<?> task, Object concurrentResult) {
    Throwable exceptionResult = null;
    for (int i = this.preProcessIndex; i >= 0; i--) {
      try {
        this.interceptors.get(i).postProcess(request, task, concurrentResult);
      }
      catch (Throwable ex) {
        // Save the first exception but invoke all interceptors
        if (exceptionResult != null) {
          if (logger.isTraceEnabled()) {
            logger.trace("Ignoring failure in postProcess method", ex);
          }
        }
        else {
          exceptionResult = ex;
        }
      }
    }
    return (exceptionResult != null) ? exceptionResult : concurrentResult;
  }

  public Object triggerAfterTimeout(RequestContext request, Callable<?> task) {
    cancelTask();
    for (CallableProcessingInterceptor interceptor : this.interceptors) {
      try {
        Object result = interceptor.handleTimeout(request, task);
        if (result == CallableProcessingInterceptor.RESPONSE_HANDLED) {
          break;
        }
        else if (result != CallableProcessingInterceptor.RESULT_NONE) {
          return result;
        }
      }
      catch (Throwable ex) {
        return ex;
      }
    }
    return CallableProcessingInterceptor.RESULT_NONE;
  }

  private void cancelTask() {
    Future<?> future = this.taskFuture;
    if (future != null) {
      try {
        future.cancel(true);
      }
      catch (Throwable ex) {
        // Ignore
      }
    }
  }

  public Object triggerAfterError(RequestContext request, Callable<?> task, Throwable throwable) {
    cancelTask();
    for (CallableProcessingInterceptor interceptor : this.interceptors) {
      try {
        Object result = interceptor.handleError(request, task, throwable);
        if (result == CallableProcessingInterceptor.RESPONSE_HANDLED) {
          break;
        }
        else if (result != CallableProcessingInterceptor.RESULT_NONE) {
          return result;
        }
      }
      catch (Throwable ex) {
        return ex;
      }
    }
    return CallableProcessingInterceptor.RESULT_NONE;
  }

  public void triggerAfterCompletion(RequestContext request, Callable<?> task) {
    for (int i = this.interceptors.size() - 1; i >= 0; i--) {
      try {
        this.interceptors.get(i).afterCompletion(request, task);
      }
      catch (Throwable ex) {
        if (logger.isTraceEnabled()) {
          logger.trace("Ignoring failure in afterCompletion method", ex);
        }
      }
    }
  }

}
