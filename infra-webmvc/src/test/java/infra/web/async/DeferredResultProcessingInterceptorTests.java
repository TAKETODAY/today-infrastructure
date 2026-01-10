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

import org.junit.jupiter.api.Test;

import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 15:51
 */
class DeferredResultProcessingInterceptorTests {

  @Test
  void defaultMethodsDoNotThrowExceptions() throws Exception {
    DeferredResultProcessingInterceptor interceptor = new DeferredResultProcessingInterceptor() { };
    RequestContext request = new MockRequestContext();
    DeferredResult<String> deferredResult = new DeferredResult<>();

    // Should not throw any exceptions
    interceptor.beforeConcurrentHandling(request, deferredResult);
    interceptor.preProcess(request, deferredResult);
    interceptor.postProcess(request, deferredResult, "result");
    boolean timeoutResult = interceptor.handleTimeout(request, deferredResult);
    boolean errorResult = interceptor.handleError(request, deferredResult, new RuntimeException("test"));
    interceptor.afterCompletion(request, deferredResult);

    // Check default return values
    assertThat(timeoutResult).isTrue();
    assertThat(errorResult).isTrue();
  }

  @Test
  void handleTimeoutReturnsTrueByDefault() throws Exception {
    DeferredResultProcessingInterceptor interceptor = new DeferredResultProcessingInterceptor() { };
    RequestContext request = new MockRequestContext();
    DeferredResult<String> deferredResult = new DeferredResult<>();

    boolean result = interceptor.handleTimeout(request, deferredResult);

    assertThat(result).isTrue();
  }

  @Test
  void handleErrorReturnsTrueByDefault() throws Exception {
    DeferredResultProcessingInterceptor interceptor = new DeferredResultProcessingInterceptor() { };
    RequestContext request = new MockRequestContext();
    DeferredResult<String> deferredResult = new DeferredResult<>();

    boolean result = interceptor.handleError(request, deferredResult, new RuntimeException("test"));

    assertThat(result).isTrue();
  }

  @Test
  void customImplementationOverridesDefaultBehavior() throws Exception {
    boolean[] called = { false, false, false, false, false, false };

    DeferredResultProcessingInterceptor interceptor = new DeferredResultProcessingInterceptor() {
      @Override
      public <T> void beforeConcurrentHandling(RequestContext request, DeferredResult<T> deferredResult) throws Exception {
        called[0] = true;
      }

      @Override
      public <T> void preProcess(RequestContext request, DeferredResult<T> deferredResult) throws Exception {
        called[1] = true;
      }

      @Override
      public <T> void postProcess(RequestContext request, DeferredResult<T> deferredResult, Object concurrentResult) throws Exception {
        called[2] = true;
      }

      @Override
      public <T> boolean handleTimeout(RequestContext request, DeferredResult<T> deferredResult) throws Exception {
        called[3] = true;
        return false;
      }

      @Override
      public <T> boolean handleError(RequestContext request, DeferredResult<T> deferredResult, Throwable t) throws Exception {
        called[4] = true;
        return false;
      }

      @Override
      public <T> void afterCompletion(RequestContext request, DeferredResult<T> deferredResult) throws Exception {
        called[5] = true;
      }
    };

    RequestContext request = new MockRequestContext();
    DeferredResult<String> deferredResult = new DeferredResult<>();

    interceptor.beforeConcurrentHandling(request, deferredResult);
    interceptor.preProcess(request, deferredResult);
    interceptor.postProcess(request, deferredResult, "result");
    boolean timeoutResult = interceptor.handleTimeout(request, deferredResult);
    boolean errorResult = interceptor.handleError(request, deferredResult, new RuntimeException("test"));
    interceptor.afterCompletion(request, deferredResult);

    // Verify all methods were called
    assertThat(called).containsOnly(true);
    assertThat(timeoutResult).isFalse();
    assertThat(errorResult).isFalse();
  }

}