/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import infra.beans.factory.support.StandardBeanFactory;
import infra.core.Ordered;
import infra.core.task.AsyncTaskExecutor;
import infra.core.task.SimpleAsyncTaskExecutor;
import infra.core.task.SyncTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/2/9 22:33
 */
class AsyncExecutionInterceptorTests {

  @Test
  @SuppressWarnings("unchecked")
  void invokeOnInterfaceWithGeneric() throws Throwable {
    AsyncExecutionInterceptor interceptor = spy(new AsyncExecutionInterceptor(null));
    FutureRunner impl = new FutureRunner();
    MethodInvocation mi = mock();
    given(mi.getThis()).willReturn(impl);
    given(mi.getMethod()).willReturn(GenericRunner.class.getMethod("run"));

    interceptor.invoke(mi);
    ArgumentCaptor<Class<?>> classArgumentCaptor = ArgumentCaptor.forClass(Class.class);
    verify(interceptor).doSubmit(any(Callable.class), any(AsyncTaskExecutor.class), classArgumentCaptor.capture());
    assertThat(classArgumentCaptor.getValue()).isEqualTo(Future.class);
  }

  @Test
  void invokeReturnsFutureResultAndUnwrapsTargetFuture() throws Throwable {
    AsyncExecutionInterceptor interceptor = new AsyncExecutionInterceptor(new SyncTaskExecutor());
    MethodInvocation invocation = mock();
    given(invocation.getThis()).willReturn(new FutureService());
    given(invocation.getMethod()).willReturn(FutureService.class.getMethod("future"));
    given(invocation.proceed()).willReturn(CompletableFuture.completedFuture("value"));

    Object result = interceptor.invoke(invocation);

    assertThat(result).isInstanceOf(Future.class);
    assertThat(((Future<?>) result).get(1, TimeUnit.SECONDS)).isEqualTo("value");
  }

  @Test
  void invokeHandlesVoidFailuresWithConfiguredHandler() throws Throwable {
    AtomicReference<Throwable> handled = new AtomicReference<>();
    AsyncExecutionInterceptor interceptor = new AsyncExecutionInterceptor(null);
    interceptor.setExecutor(new SyncTaskExecutor());
    interceptor.setExceptionHandler((ex, method, params) -> handled.set(ex));
    MethodInvocation invocation = mock();
    IllegalStateException failure = new IllegalStateException("failure");
    given(invocation.getThis()).willReturn(new VoidService());
    given(invocation.getMethod()).willReturn(VoidService.class.getMethod("run"));
    given(invocation.proceed()).willThrow(failure);

    assertThat(interceptor.invoke(invocation)).isNull();
    assertThat(handled).hasValue(failure);
  }

  @Test
  void propagatesFutureFailuresThroughTheReturnedFuture() throws Throwable {
    var interceptor = new AsyncExecutionInterceptor(new SyncTaskExecutor());
    MethodInvocation invocation = mock();
    var failure = new IllegalStateException("failed future");
    given(invocation.getMethod()).willReturn(FutureService.class.getMethod("future"));
    given(invocation.proceed()).willReturn(CompletableFuture.failedFuture(failure));

    Future<?> result = (Future<?>) interceptor.invoke(invocation);
    assertThatExceptionOfType(ExecutionException.class).isThrownBy(() -> result.get(1, TimeUnit.SECONDS))
            .withCause(failure);
  }

  @Test
  void requiresABeanFactoryWhenAnExecutorIsQualified() {
    var interceptor = new AsyncExecutionInterceptor(null);

    assertThatIllegalStateException().isThrownBy(() -> interceptor.findQualifiedExecutor(null, "worker"))
            .withMessageContaining("BeanFactory must be set", "qualified executor 'worker'");
  }

  @Test
  void ambiguousExecutorLookupRequiresAnExplicitDefaultName() {
    var beans = new StandardBeanFactory();
    beans.registerSingleton("first", new SyncTaskExecutor());
    beans.registerSingleton("second", new SyncTaskExecutor());
    var interceptor = new AsyncExecutionInterceptor(null);

    assertThat(interceptor.getDefaultExecutor(beans)).isInstanceOf(SimpleAsyncTaskExecutor.class);
    var defaultExecutor = new SyncTaskExecutor();
    beans.registerSingleton("taskExecutor", defaultExecutor);
    assertThat(interceptor.getDefaultExecutor(beans)).isSameAs(defaultExecutor);
    assertThat(interceptor.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
  }

  @Test
  void invokeRejectsMissingExecutor() throws Exception {
    AsyncExecutionInterceptor interceptor = new AsyncExecutionInterceptor(null) {
      @Override
      protected Executor getDefaultExecutor(infra.beans.factory.BeanFactory beanFactory) {
        return null;
      }
    };
    MethodInvocation invocation = mock();
    given(invocation.getThis()).willReturn(new VoidService());
    given(invocation.getMethod()).willReturn(VoidService.class.getMethod("run"));

    org.assertj.core.api.Assertions.assertThatIllegalStateException()
            .isThrownBy(() -> interceptor.invoke(invocation));
  }

  interface GenericRunner<O> {

    O run();
  }

  static class FutureRunner implements GenericRunner<Future<Void>> {
    @Override
    public Future<Void> run() {
      return CompletableFuture.runAsync(() -> {
      });
    }
  }

  static class FutureService {
    public Future<String> future() {
      return CompletableFuture.completedFuture("value");
    }
  }

  static class VoidService {
    public void run() {
    }
  }
}
