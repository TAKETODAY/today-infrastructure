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

package infra.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import infra.core.task.AsyncTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
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
}