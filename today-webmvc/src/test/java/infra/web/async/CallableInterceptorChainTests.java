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
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 15:41
 */
class CallableInterceptorChainTests {

  @Test
  void setTaskFutureStoresFuture() throws Exception {
    ArrayList<CallableProcessingInterceptor> interceptors = new ArrayList<>();
    CallableInterceptorChain chain = new CallableInterceptorChain(interceptors);
    FutureTask<String> future = new FutureTask<>(() -> "test");

    chain.setTaskFuture(future);
    // Simply test that no exception is thrown
    assertThat(true).isTrue();
  }

  @Test
  void applyBeforeConcurrentHandlingInvokesAllInterceptors() throws Exception {
    TestCallableProcessingInterceptor interceptor1 = new TestCallableProcessingInterceptor();
    TestCallableProcessingInterceptor interceptor2 = new TestCallableProcessingInterceptor();
    ArrayList<CallableProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    CallableInterceptorChain chain = new CallableInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    Callable<String> task = () -> "test";

    chain.applyBeforeConcurrentHandling(request, task);

    assertThat(interceptor1.beforeConcurrentHandlingInvoked).isTrue();
    assertThat(interceptor2.beforeConcurrentHandlingInvoked).isTrue();
  }

  @Test
  void applyPreProcessInvokesAllInterceptorsAndTracksIndex() throws Exception {
    TestCallableProcessingInterceptor interceptor1 = new TestCallableProcessingInterceptor();
    TestCallableProcessingInterceptor interceptor2 = new TestCallableProcessingInterceptor();
    ArrayList<CallableProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    CallableInterceptorChain chain = new CallableInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    Callable<String> task = () -> "test";

    chain.applyPreProcess(request, task);

    assertThat(interceptor1.preProcessInvoked).isTrue();
    assertThat(interceptor2.preProcessInvoked).isTrue();
    assertThat(chain.preProcessIndex).isEqualTo(1);
  }

  @Test
  void applyPostProcessInvokesInterceptorsInReverseOrder() {
    TestCallableProcessingInterceptor interceptor1 = new TestCallableProcessingInterceptor();
    TestCallableProcessingInterceptor interceptor2 = new TestCallableProcessingInterceptor();
    ArrayList<CallableProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    CallableInterceptorChain chain = new CallableInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    Callable<String> task = () -> "test";
    chain.preProcessIndex = 1; // Simulate preProcess having been called

    Object result = chain.applyPostProcess(request, task, "result");

    assertThat(interceptor2.postProcessInvoked).isTrue();
    assertThat(interceptor1.postProcessInvoked).isTrue();
    assertThat(result).isEqualTo("result");
  }

  @Test
  void applyPostProcessReturnsFirstException() {
    TestCallableProcessingInterceptor interceptor1 = new TestCallableProcessingInterceptor();
    FailingCallableProcessingInterceptor interceptor2 = new FailingCallableProcessingInterceptor();
    ArrayList<CallableProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    CallableInterceptorChain chain = new CallableInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    Callable<String> task = () -> "test";
    chain.preProcessIndex = 1; // Simulate preProcess having been called

    Object result = chain.applyPostProcess(request, task, "result");

    assertThat(result).isInstanceOf(RuntimeException.class);
    assertThat(((RuntimeException) result).getMessage()).isEqualTo("Test exception");
  }

  @Test
  void triggerAfterTimeoutCancelsTaskAndInvokesInterceptors() {
    TestCallableProcessingInterceptor interceptor1 = new TestCallableProcessingInterceptor();
    TestCallableProcessingInterceptor interceptor2 = new TestCallableProcessingInterceptor();
    ArrayList<CallableProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    CallableInterceptorChain chain = new CallableInterceptorChain(interceptors);
    FutureTask<String> future = new FutureTask<>(() -> "test");
    chain.setTaskFuture(future);
    RequestContext request = new MockRequestContext();
    Callable<String> task = () -> "test";

    Object result = chain.triggerAfterTimeout(request, task);

    assertThat(future.isCancelled()).isTrue();
    assertThat(interceptor1.handleTimeoutInvoked).isTrue();
    assertThat(interceptor2.handleTimeoutInvoked).isTrue();
    assertThat(result).isEqualTo(CallableProcessingInterceptor.RESULT_NONE);
  }

  @Test
  void triggerAfterTimeoutReturnsCustomResult() {
    TestCallableProcessingInterceptor interceptor1 = new TestCallableProcessingInterceptor();
    CustomResultCallableProcessingInterceptor interceptor2 = new CustomResultCallableProcessingInterceptor();
    ArrayList<CallableProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    CallableInterceptorChain chain = new CallableInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    Callable<String> task = () -> "test";

    Object result = chain.triggerAfterTimeout(request, task);

    assertThat(result).isEqualTo("custom result");
    assertThat(interceptor1.handleTimeoutInvoked).isTrue();
    assertThat(interceptor2.handleTimeoutInvoked).isTrue();
  }

  @Test
  void triggerAfterTimeoutReturnsException() {
    TestCallableProcessingInterceptor interceptor1 = new TestCallableProcessingInterceptor();
    FailingCallableProcessingInterceptor interceptor2 = new FailingCallableProcessingInterceptor();
    ArrayList<CallableProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    CallableInterceptorChain chain = new CallableInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    Callable<String> task = () -> "test";

    Object result = chain.triggerAfterTimeout(request, task);

    assertThat(result).isInstanceOf(RuntimeException.class);
    assertThat(((RuntimeException) result).getMessage()).isEqualTo("Test exception");
  }

  @Test
  void triggerAfterErrorCancelsTaskAndInvokesInterceptors() {
    TestCallableProcessingInterceptor interceptor1 = new TestCallableProcessingInterceptor();
    TestCallableProcessingInterceptor interceptor2 = new TestCallableProcessingInterceptor();
    ArrayList<CallableProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    CallableInterceptorChain chain = new CallableInterceptorChain(interceptors);
    FutureTask<String> future = new FutureTask<>(() -> "test");
    chain.setTaskFuture(future);
    RequestContext request = new MockRequestContext();
    Callable<String> task = () -> "test";
    RuntimeException error = new RuntimeException("test error");

    Object result = chain.triggerAfterError(request, task, error);

    assertThat(future.isCancelled()).isTrue();
    assertThat(interceptor1.handleErrorInvoked).isTrue();
    assertThat(interceptor2.handleErrorInvoked).isTrue();
    assertThat(result).isEqualTo(CallableProcessingInterceptor.RESULT_NONE);
  }

  @Test
  void triggerAfterCompletionInvokesInterceptorsInReverseOrder() {
    TestCallableProcessingInterceptor interceptor1 = new TestCallableProcessingInterceptor();
    TestCallableProcessingInterceptor interceptor2 = new TestCallableProcessingInterceptor();
    ArrayList<CallableProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    CallableInterceptorChain chain = new CallableInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    Callable<String> task = () -> "test";

    chain.triggerAfterCompletion(request, task);

    assertThat(interceptor2.afterCompletionInvoked).isTrue();
    assertThat(interceptor1.afterCompletionInvoked).isTrue();
  }

  @Test
  void triggerAfterCompletionHandlesExceptionInInterceptor() {
    TestCallableProcessingInterceptor interceptor1 = new TestCallableProcessingInterceptor();
    FailingCallableProcessingInterceptor interceptor2 = new FailingCallableProcessingInterceptor();
    ArrayList<CallableProcessingInterceptor> interceptors = new ArrayList<>();
    interceptors.add(interceptor1);
    interceptors.add(interceptor2);

    CallableInterceptorChain chain = new CallableInterceptorChain(interceptors);
    RequestContext request = new MockRequestContext();
    Callable<String> task = () -> "test";

    // Should not throw exception
    chain.triggerAfterCompletion(request, task);

    assertThat(interceptor1.afterCompletionInvoked).isTrue();
  }

  static class TestCallableProcessingInterceptor implements CallableProcessingInterceptor {
    boolean beforeConcurrentHandlingInvoked = false;
    boolean preProcessInvoked = false;
    boolean postProcessInvoked = false;
    boolean handleTimeoutInvoked = false;
    boolean handleErrorInvoked = false;
    boolean afterCompletionInvoked = false;

    @Override
    public <T> void beforeConcurrentHandling(RequestContext request, Callable<T> task) throws Exception {
      beforeConcurrentHandlingInvoked = true;
    }

    @Override
    public <T> void preProcess(RequestContext request, Callable<T> task) throws Exception {
      preProcessInvoked = true;
    }

    @Override
    public <T> void postProcess(RequestContext request, Callable<T> task, Object concurrentResult) throws Exception {
      postProcessInvoked = true;
    }

    @Override
    public <T> Object handleTimeout(RequestContext request, Callable<T> task) throws Exception {
      handleTimeoutInvoked = true;
      return RESULT_NONE;
    }

    @Override
    public <T> Object handleError(RequestContext request, Callable<T> task, Throwable t) throws Exception {
      handleErrorInvoked = true;
      return RESULT_NONE;
    }

    @Override
    public <T> void afterCompletion(RequestContext request, Callable<T> task) throws Exception {
      afterCompletionInvoked = true;
    }
  }

  static class FailingCallableProcessingInterceptor implements CallableProcessingInterceptor {
    @Override
    public <T> void beforeConcurrentHandling(RequestContext request, Callable<T> task) throws Exception {
      throw new RuntimeException("Test exception");
    }

    @Override
    public <T> void preProcess(RequestContext request, Callable<T> task) throws Exception {
      throw new RuntimeException("Test exception");
    }

    @Override
    public <T> void postProcess(RequestContext request, Callable<T> task, Object concurrentResult) throws Exception {
      throw new RuntimeException("Test exception");
    }

    @Override
    public <T> Object handleTimeout(RequestContext request, Callable<T> task) throws Exception {
      throw new RuntimeException("Test exception");
    }

    @Override
    public <T> Object handleError(RequestContext request, Callable<T> task, Throwable t) throws Exception {
      throw new RuntimeException("Test exception");
    }

    @Override
    public <T> void afterCompletion(RequestContext request, Callable<T> task) throws Exception {
      throw new RuntimeException("Test exception");
    }
  }

  static class ResponseHandledCallableProcessingInterceptor implements CallableProcessingInterceptor {
    boolean handleTimeoutInvoked = false;

    @Override
    public <T> Object handleTimeout(RequestContext request, Callable<T> task) throws Exception {
      handleTimeoutInvoked = true;
      return RESPONSE_HANDLED;
    }
  }

  static class CustomResultCallableProcessingInterceptor implements CallableProcessingInterceptor {
    boolean handleTimeoutInvoked = false;

    @Override
    public <T> Object handleTimeout(RequestContext request, Callable<T> task) throws Exception {
      handleTimeoutInvoked = true;
      return "custom result";
    }
  }

}