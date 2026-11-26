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

import java.time.Duration;
import java.util.List;

import infra.core.task.AsyncTaskExecutor;
import infra.web.HttpContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

class WebAsyncManagerFactoryTests {

  @Test
  void createsManagerWithConfiguredExecutorTimeoutAndInterceptors() {
    WebAsyncManagerFactory factory = new WebAsyncManagerFactory();
    AsyncTaskExecutor executor = mock(AsyncTaskExecutor.class);
    CallableProcessingInterceptor callable = mock(CallableProcessingInterceptor.class);
    DeferredResultProcessingInterceptor deferred = mock(DeferredResultProcessingInterceptor.class);
    Duration timeout = Duration.ofSeconds(3);
    factory.setTaskExecutor(executor);
    factory.setAsyncRequestTimeout(timeout);
    factory.setCallableInterceptors(List.of(callable));
    factory.setDeferredResultInterceptors(List.of(deferred));

    WebAsyncManager manager = factory.createWebAsyncManager(mock(HttpContext.class));

    assertThat(manager.getCallableInterceptor(callable.getClass().getName() + ":" + callable.hashCode()))
            .isSameAs(callable);
    assertThat(manager.getDeferredResultInterceptor(deferred.getClass().getName() + ":" + deferred.hashCode()))
            .isSameAs(deferred);
  }

  @Test
  void acceptsNullConfigurationAndRejectsInvalidTimeouts() {
    WebAsyncManagerFactory factory = new WebAsyncManagerFactory();
    factory.setAsyncRequestTimeout(null);
    factory.setCallableInterceptors(null);
    factory.setDeferredResultInterceptors(null);

    assertThat(factory.createWebAsyncManager(mock(HttpContext.class))).isNotNull();
    assertThatIllegalArgumentException().isThrownBy(() -> factory.setAsyncRequestTimeout(Duration.ofNanos(-1)))
            .withMessageContaining("must not be negative");
    assertThatIllegalArgumentException().isThrownBy(() -> factory.setAsyncRequestTimeout(Duration.ofNanos(1)))
            .withMessageContaining("must be zero or at least 1 millisecond");
  }

  @Test
  void createsDefaultFactoryWhenApplicationContextHasNoFactory() {
    var context = mock(infra.context.ApplicationContext.class);

    assertThat(WebAsyncManagerFactory.find(context)).isNotNull().isInstanceOf(WebAsyncManagerFactory.class);
  }

}
