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

package infra.scheduling.aspectj;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import infra.aop.interceptor.AsyncUncaughtExceptionHandler;
import infra.scheduling.annotation.Async;
import infra.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

class AspectJAsyncConfigurationTests {

  @Test
  void asyncAdvisorUsesConfiguredExecutorAndExceptionHandler() throws NoSuchMethodException {
    Executor executor = mock(Executor.class);
    AsyncUncaughtExceptionHandler handler = mock(AsyncUncaughtExceptionHandler.class);
    TestableConfiguration configuration = new TestableConfiguration();
    configuration.setConfiguration(() -> executor, () -> handler);
    willAnswer(invocation -> {
      invocation.<Runnable>getArgument(0).run();
      return null;
    }).given(executor).execute(any(Runnable.class));

    AnnotationAsyncExecutionAspect aspect = configuration.asyncAdvisor();
    aspect.setBeanFactory(null);
    try {
      assertThat(aspect).isSameAs(AnnotationAsyncExecutionAspect.aspectOf());
      Method method = AsyncService.class.getDeclaredMethod("fail", IllegalStateException.class);
      IllegalStateException failure = new IllegalStateException("async failure");

      new AsyncService().fail(failure);

      then(executor).should().execute(any(Runnable.class));
      then(handler).should().handleUncaughtException(failure, method, failure);
    }
    finally {
      aspect.configure(null, null);
      aspect.setBeanFactory(null);
    }
  }

  @Test
  void asyncAdvisorRunsOnCallingThreadWhenNoExecutorIsAvailable() throws Exception {
    AnnotationAsyncExecutionAspect aspect = new AspectJAsyncConfiguration().asyncAdvisor();
    aspect.setBeanFactory(null);

    assertThat(new AsyncService().currentThread().get(5, TimeUnit.SECONDS)).isSameAs(Thread.currentThread());
  }

  @Test
  void unannotatedMethodHasNoExecutorQualifier() throws NoSuchMethodException {
    Method method = Object.class.getMethod("toString");
    assertThat(AnnotationAsyncExecutionAspect.aspectOf().getExecutorQualifier(method)).isNull();
  }

  private static class TestableConfiguration extends AspectJAsyncConfiguration {

    void setConfiguration(Supplier<Executor> executor, Supplier<AsyncUncaughtExceptionHandler> exceptionHandler) {
      this.executor = executor;
      this.exceptionHandler = exceptionHandler;
    }
  }

  static class AsyncService {

    @Async
    public Future<Thread> currentThread() {
      return Future.ok(Thread.currentThread());
    }

    @Async
    public void fail(IllegalStateException failure) {
      throw failure;
    }
  }
}
