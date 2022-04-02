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
