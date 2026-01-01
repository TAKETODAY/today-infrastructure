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

import java.util.ArrayList;

import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 15:53
 */
class DeferredResultInterceptorChainTests {

  @Test
  void constructorInitializesInterceptors() {
    ArrayList<DeferredResultProcessingInterceptor> interceptors = new ArrayList<>();
    DeferredResultInterceptorChain chain = new DeferredResultInterceptorChain(interceptors);

    assertThat(chain).isNotNull();
  }

  @Test
  void applyBeforeConcurrentHandlingInvokesAllInterceptors() throws Exception {
    TestDeferredResultProcessingInterceptor interceptor1 = new TestDeferredResultProcessingInterceptor();
    TestDeferredResultProcessingInterceptor interceptor2 = new TestDeferredResultProcessingInterceptor();
    ArrayList<DeferredResultProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    DeferredResultInterceptorChain chain = new DeferredResultInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    DeferredResult<String> deferredResult = new DeferredResult<>();

    chain.applyBeforeConcurrentHandling(request, deferredResult);

    assertThat(interceptor1.beforeConcurrentHandlingInvoked).isTrue();
    assertThat(interceptor2.beforeConcurrentHandlingInvoked).isTrue();
  }

  @Test
  void applyPreProcessInvokesAllInterceptorsAndTracksIndex() throws Exception {
    TestDeferredResultProcessingInterceptor interceptor1 = new TestDeferredResultProcessingInterceptor();
    TestDeferredResultProcessingInterceptor interceptor2 = new TestDeferredResultProcessingInterceptor();
    ArrayList<DeferredResultProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    DeferredResultInterceptorChain chain = new DeferredResultInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    DeferredResult<String> deferredResult = new DeferredResult<>();

    chain.applyPreProcess(request, deferredResult);

    assertThat(interceptor1.preProcessInvoked).isTrue();
    assertThat(interceptor2.preProcessInvoked).isTrue();
    assertThat(chain.preProcessingIndex).isEqualTo(1);
  }

  @Test
  void applyPostProcessInvokesInterceptorsInReverseOrder() {
    TestDeferredResultProcessingInterceptor interceptor1 = new TestDeferredResultProcessingInterceptor();
    TestDeferredResultProcessingInterceptor interceptor2 = new TestDeferredResultProcessingInterceptor();
    ArrayList<DeferredResultProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    DeferredResultInterceptorChain chain = new DeferredResultInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    DeferredResult<String> deferredResult = new DeferredResult<>();
    chain.preProcessingIndex = 1; // Simulate preProcess having been called

    Object result = chain.applyPostProcess(request, deferredResult, "result");

    assertThat(interceptor2.postProcessInvoked).isTrue();
    assertThat(interceptor1.postProcessInvoked).isTrue();
    assertThat(result).isEqualTo("result");
  }

  @Test
  void applyPostProcessReturnsExceptionOnFailure() {
    TestDeferredResultProcessingInterceptor interceptor1 = new TestDeferredResultProcessingInterceptor();
    FailingDeferredResultProcessingInterceptor interceptor2 = new FailingDeferredResultProcessingInterceptor();
    ArrayList<DeferredResultProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    DeferredResultInterceptorChain chain = new DeferredResultInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    DeferredResult<String> deferredResult = new DeferredResult<>();
    chain.preProcessingIndex = 1; // Simulate preProcess having been called

    Object result = chain.applyPostProcess(request, deferredResult, "result");

    assertThat(result).isInstanceOf(RuntimeException.class);
    assertThat(((RuntimeException) result).getMessage()).isEqualTo("Test exception");
  }

  @Test
  void triggerAfterTimeoutInvokesInterceptors() throws Exception {
    TestDeferredResultProcessingInterceptor interceptor1 = new TestDeferredResultProcessingInterceptor();
    TestDeferredResultProcessingInterceptor interceptor2 = new TestDeferredResultProcessingInterceptor();
    ArrayList<DeferredResultProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    DeferredResultInterceptorChain chain = new DeferredResultInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    DeferredResult<String> deferredResult = new DeferredResult<>();

    chain.triggerAfterTimeout(request, deferredResult);

    assertThat(interceptor1.handleTimeoutInvoked).isTrue();
    assertThat(interceptor2.handleTimeoutInvoked).isTrue();
  }

  @Test
  void triggerAfterTimeoutStopsWhenDeferredResultIsSet() throws Exception {
    TestDeferredResultProcessingInterceptor interceptor1 = new TestDeferredResultProcessingInterceptor();
    TestDeferredResultProcessingInterceptor interceptor2 = new TestDeferredResultProcessingInterceptor();
    ArrayList<DeferredResultProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    DeferredResultInterceptorChain chain = new DeferredResultInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    DeferredResult<String> deferredResult = new DeferredResult<>();
    deferredResult.setResult("test"); // Set the deferred result

    chain.triggerAfterTimeout(request, deferredResult);

    assertThat(interceptor1.handleTimeoutInvoked).isFalse();
    assertThat(interceptor2.handleTimeoutInvoked).isFalse(); // Should not be invoked
  }

  @Test
  void triggerAfterTimeoutStopsWhenInterceptorReturnsFalse() throws Exception {
    TestDeferredResultProcessingInterceptor interceptor1 = new TestDeferredResultProcessingInterceptor();
    FalseReturningDeferredResultProcessingInterceptor interceptor2 = new FalseReturningDeferredResultProcessingInterceptor();
    ArrayList<DeferredResultProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    DeferredResultInterceptorChain chain = new DeferredResultInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    DeferredResult<String> deferredResult = new DeferredResult<>();

    chain.triggerAfterTimeout(request, deferredResult);

    assertThat(interceptor1.handleTimeoutInvoked).isTrue();
    assertThat(interceptor2.handleTimeoutInvoked).isTrue();
    // interceptor2 returns false, so no further interceptors should be invoked
  }

  @Test
  void triggerAfterErrorInvokesInterceptors() throws Exception {
    TestDeferredResultProcessingInterceptor interceptor1 = new TestDeferredResultProcessingInterceptor();
    TestDeferredResultProcessingInterceptor interceptor2 = new TestDeferredResultProcessingInterceptor();
    ArrayList<DeferredResultProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    DeferredResultInterceptorChain chain = new DeferredResultInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    DeferredResult<String> deferredResult = new DeferredResult<>();
    RuntimeException error = new RuntimeException("test error");

    boolean result = chain.triggerAfterError(request, deferredResult, error);

    assertThat(interceptor1.handleErrorInvoked).isTrue();
    assertThat(interceptor2.handleErrorInvoked).isTrue();
    assertThat(result).isTrue();
  }

  @Test
  void triggerAfterErrorReturnsFalseWhenDeferredResultIsSet() throws Exception {
    TestDeferredResultProcessingInterceptor interceptor1 = new TestDeferredResultProcessingInterceptor();
    TestDeferredResultProcessingInterceptor interceptor2 = new TestDeferredResultProcessingInterceptor();
    ArrayList<DeferredResultProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    DeferredResultInterceptorChain chain = new DeferredResultInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    DeferredResult<String> deferredResult = new DeferredResult<>();
    deferredResult.setResult("test"); // Set the deferred result

    boolean result = chain.triggerAfterError(request, deferredResult, new RuntimeException("test error"));

    assertThat(interceptor1.handleErrorInvoked).isFalse();
    assertThat(interceptor2.handleErrorInvoked).isFalse();
    assertThat(result).isFalse();
  }

  @Test
  void triggerAfterErrorReturnsFalseWhenInterceptorReturnsFalse() throws Exception {
    TestDeferredResultProcessingInterceptor interceptor1 = new TestDeferredResultProcessingInterceptor();
    FalseReturningDeferredResultProcessingInterceptor interceptor2 = new FalseReturningDeferredResultProcessingInterceptor();
    ArrayList<DeferredResultProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    DeferredResultInterceptorChain chain = new DeferredResultInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    DeferredResult<String> deferredResult = new DeferredResult<>();

    boolean result = chain.triggerAfterError(request, deferredResult, new RuntimeException("test error"));

    assertThat(interceptor1.handleErrorInvoked).isTrue();
    assertThat(interceptor2.handleErrorInvoked).isTrue();
    assertThat(result).isFalse();
  }

  @Test
  void triggerAfterCompletionInvokesInterceptorsInReverseOrder() {
    TestDeferredResultProcessingInterceptor interceptor1 = new TestDeferredResultProcessingInterceptor();
    TestDeferredResultProcessingInterceptor interceptor2 = new TestDeferredResultProcessingInterceptor();
    ArrayList<DeferredResultProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    DeferredResultInterceptorChain chain = new DeferredResultInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    DeferredResult<String> deferredResult = new DeferredResult<>();
    chain.preProcessingIndex = 1; // Simulate preProcess having been called

    chain.triggerAfterCompletion(request, deferredResult);

    assertThat(interceptor2.afterCompletionInvoked).isTrue();
    assertThat(interceptor1.afterCompletionInvoked).isTrue();
  }

  @Test
  void triggerAfterCompletionHandlesExceptionInInterceptor() {
    TestDeferredResultProcessingInterceptor interceptor1 = new TestDeferredResultProcessingInterceptor();
    FailingDeferredResultProcessingInterceptor interceptor2 = new FailingDeferredResultProcessingInterceptor();
    ArrayList<DeferredResultProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    DeferredResultInterceptorChain chain = new DeferredResultInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    DeferredResult<String> deferredResult = new DeferredResult<>();
    chain.preProcessingIndex = 1; // Simulate preProcess having been called

    // Should not throw exception
    chain.triggerAfterCompletion(request, deferredResult);

    assertThat(interceptor1.afterCompletionInvoked).isTrue();
  }

  static class TestDeferredResultProcessingInterceptor implements DeferredResultProcessingInterceptor {
    boolean beforeConcurrentHandlingInvoked = false;
    boolean preProcessInvoked = false;
    boolean postProcessInvoked = false;
    boolean handleTimeoutInvoked = false;
    boolean handleErrorInvoked = false;
    boolean afterCompletionInvoked = false;

    @Override
    public <T> void beforeConcurrentHandling(RequestContext request, DeferredResult<T> deferredResult) throws Exception {
      beforeConcurrentHandlingInvoked = true;
    }

    @Override
    public <T> void preProcess(RequestContext request, DeferredResult<T> deferredResult) throws Exception {
      preProcessInvoked = true;
    }

    @Override
    public <T> void postProcess(RequestContext request, DeferredResult<T> deferredResult, Object concurrentResult) throws Exception {
      postProcessInvoked = true;
    }

    @Override
    public <T> boolean handleTimeout(RequestContext request, DeferredResult<T> deferredResult) throws Exception {
      handleTimeoutInvoked = true;
      return true;
    }

    @Override
    public <T> boolean handleError(RequestContext request, DeferredResult<T> deferredResult, Throwable t) throws Exception {
      handleErrorInvoked = true;
      return true;
    }

    @Override
    public <T> void afterCompletion(RequestContext request, DeferredResult<T> deferredResult) throws Exception {
      afterCompletionInvoked = true;
    }
  }

  static class FailingDeferredResultProcessingInterceptor implements DeferredResultProcessingInterceptor {
    @Override
    public <T> void beforeConcurrentHandling(RequestContext request, DeferredResult<T> deferredResult) throws Exception {
      throw new RuntimeException("Test exception");
    }

    @Override
    public <T> void preProcess(RequestContext request, DeferredResult<T> deferredResult) throws Exception {
      throw new RuntimeException("Test exception");
    }

    @Override
    public <T> void postProcess(RequestContext request, DeferredResult<T> deferredResult, Object concurrentResult) throws Exception {
      throw new RuntimeException("Test exception");
    }

    @Override
    public <T> boolean handleTimeout(RequestContext request, DeferredResult<T> deferredResult) throws Exception {
      throw new RuntimeException("Test exception");
    }

    @Override
    public <T> boolean handleError(RequestContext request, DeferredResult<T> deferredResult, Throwable t) throws Exception {
      throw new RuntimeException("Test exception");
    }

    @Override
    public <T> void afterCompletion(RequestContext request, DeferredResult<T> deferredResult) throws Exception {
      throw new RuntimeException("Test exception");
    }
  }

  static class FalseReturningDeferredResultProcessingInterceptor implements DeferredResultProcessingInterceptor {
    boolean handleTimeoutInvoked = false;
    boolean handleErrorInvoked = false;

    @Override
    public <T> boolean handleTimeout(RequestContext request, DeferredResult<T> deferredResult) throws Exception {
      handleTimeoutInvoked = true;
      return false;
    }

    @Override
    public <T> boolean handleError(RequestContext request, DeferredResult<T> deferredResult, Throwable t) throws Exception {
      handleErrorInvoked = true;
      return false;
    }
  }

}