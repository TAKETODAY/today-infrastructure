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

package cn.taketoday.web.async;

import java.util.ArrayList;

import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.RequestContext;

/**
 * Assists with the invocation of {@link DeferredResultProcessingInterceptor}'s.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class DeferredResultInterceptorChain {

  private final ArrayList<DeferredResultProcessingInterceptor> interceptors;

  private int preProcessingIndex = -1;

  public DeferredResultInterceptorChain(ArrayList<DeferredResultProcessingInterceptor> interceptors) {
    this.interceptors = interceptors;
  }

  public void applyBeforeConcurrentHandling(RequestContext request, DeferredResult<?> deferredResult)
          throws Exception {

    for (DeferredResultProcessingInterceptor interceptor : this.interceptors) {
      interceptor.beforeConcurrentHandling(request, deferredResult);
    }
  }

  public void applyPreProcess(RequestContext request, DeferredResult<?> deferredResult) throws Exception {
    for (DeferredResultProcessingInterceptor interceptor : this.interceptors) {
      interceptor.preProcess(request, deferredResult);
      this.preProcessingIndex++;
    }
  }

  @Nullable
  public Object applyPostProcess(RequestContext request, DeferredResult<?> deferredResult, @Nullable Object concurrentResult) {

    try {
      for (int i = this.preProcessingIndex; i >= 0; i--) {
        this.interceptors.get(i).postProcess(request, deferredResult, concurrentResult);
      }
    }
    catch (Throwable ex) {
      return ex;
    }
    return concurrentResult;
  }

  public void triggerAfterTimeout(RequestContext request, DeferredResult<?> deferredResult) throws Exception {
    for (DeferredResultProcessingInterceptor interceptor : this.interceptors) {
      if (deferredResult.isSetOrExpired()) {
        return;
      }
      if (!interceptor.handleTimeout(request, deferredResult)) {
        break;
      }
    }
  }

  /**
   * Determine if further error handling should be bypassed.
   *
   * @return {@code true} to continue error handling, or false to bypass any further
   * error handling
   */
  public boolean triggerAfterError(RequestContext request, DeferredResult<?> deferredResult, Throwable ex)
          throws Exception {

    for (DeferredResultProcessingInterceptor interceptor : this.interceptors) {
      if (deferredResult.isSetOrExpired()) {
        return false;
      }
      if (!interceptor.handleError(request, deferredResult, ex)) {
        return false;
      }
    }
    return true;
  }

  public void triggerAfterCompletion(RequestContext request, DeferredResult<?> deferredResult) {
    for (int i = this.preProcessingIndex; i >= 0; i--) {
      try {
        this.interceptors.get(i).afterCompletion(request, deferredResult);
      }
      catch (Throwable ex) {
        LoggerFactory.getLogger(DeferredResultInterceptorChain.class)
                .trace("Ignoring failure in afterCompletion method", ex);
      }
    }
  }

}
