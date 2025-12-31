/*
 * Copyright 2017 - 2025 the original author or authors.
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