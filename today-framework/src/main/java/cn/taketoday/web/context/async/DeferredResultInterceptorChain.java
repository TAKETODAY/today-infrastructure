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

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.RequestContext;

/**
 * Assists with the invocation of {@link DeferredResultProcessingInterceptor}'s.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class DeferredResultInterceptorChain {

  private static final Logger logger = LoggerFactory.getLogger(DeferredResultInterceptorChain.class);

  private final List<DeferredResultProcessingInterceptor> interceptors;

  private int preProcessingIndex = -1;

  public DeferredResultInterceptorChain(List<DeferredResultProcessingInterceptor> interceptors) {
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

  public Object applyPostProcess(RequestContext request, DeferredResult<?> deferredResult,
          Object concurrentResult) {

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
        logger.trace("Ignoring failure in afterCompletion method", ex);
      }
    }
  }

}
