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

package infra.web.config.annotation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import infra.core.task.AsyncTaskExecutor;
import infra.web.async.CallableProcessingInterceptor;
import infra.web.async.DeferredResultProcessingInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 21:36
 */
class AsyncSupportConfigurerTests {

  @Test
  void setTaskExecutorStoresExecutor() {
    AsyncSupportConfigurer configurer = new AsyncSupportConfigurer();
    AsyncTaskExecutor taskExecutor = mock(AsyncTaskExecutor.class);

    AsyncSupportConfigurer result = configurer.setTaskExecutor(taskExecutor);

    assertThat(result).isSameAs(configurer);
    assertThat(configurer.taskExecutor).isSameAs(taskExecutor);
  }

  @Test
  void setDefaultTimeoutStoresTimeout() {
    AsyncSupportConfigurer configurer = new AsyncSupportConfigurer();
    long timeout = 5000L;

    AsyncSupportConfigurer result = configurer.setDefaultTimeout(timeout);

    assertThat(result).isSameAs(configurer);
    assertThat(configurer.timeout).isEqualTo(timeout);
  }

  @Test
  void registerCallableInterceptorsWithNullList() {
    AsyncSupportConfigurer configurer = new AsyncSupportConfigurer();
    CallableProcessingInterceptor interceptor1 = mock(CallableProcessingInterceptor.class);
    CallableProcessingInterceptor interceptor2 = mock(CallableProcessingInterceptor.class);

    AsyncSupportConfigurer result = configurer.registerCallableInterceptors(interceptor1, interceptor2);

    assertThat(result).isSameAs(configurer);
    assertThat(configurer.callableInterceptors).containsExactly(interceptor1, interceptor2);
  }

  @Test
  void registerCallableInterceptorsWithExistingList() {
    AsyncSupportConfigurer configurer = new AsyncSupportConfigurer();
    configurer.callableInterceptors = new ArrayList<>();
    CallableProcessingInterceptor interceptor1 = mock(CallableProcessingInterceptor.class);
    CallableProcessingInterceptor interceptor2 = mock(CallableProcessingInterceptor.class);

    AsyncSupportConfigurer result = configurer.registerCallableInterceptors(interceptor1, interceptor2);

    assertThat(result).isSameAs(configurer);
    assertThat(configurer.callableInterceptors).containsExactly(interceptor1, interceptor2);
  }

  @Test
  void registerDeferredResultInterceptorsWithNullList() {
    AsyncSupportConfigurer configurer = new AsyncSupportConfigurer();
    DeferredResultProcessingInterceptor interceptor1 = mock(DeferredResultProcessingInterceptor.class);
    DeferredResultProcessingInterceptor interceptor2 = mock(DeferredResultProcessingInterceptor.class);

    AsyncSupportConfigurer result = configurer.registerDeferredResultInterceptors(interceptor1, interceptor2);

    assertThat(result).isSameAs(configurer);
    assertThat(configurer.deferredResultInterceptors).containsExactly(interceptor1, interceptor2);
  }

  @Test
  void registerDeferredResultInterceptorsWithExistingList() {
    AsyncSupportConfigurer configurer = new AsyncSupportConfigurer();
    configurer.deferredResultInterceptors = new ArrayList<>();
    DeferredResultProcessingInterceptor interceptor1 = mock(DeferredResultProcessingInterceptor.class);
    DeferredResultProcessingInterceptor interceptor2 = mock(DeferredResultProcessingInterceptor.class);

    AsyncSupportConfigurer result = configurer.registerDeferredResultInterceptors(interceptor1, interceptor2);

    assertThat(result).isSameAs(configurer);
    assertThat(configurer.deferredResultInterceptors).containsExactly(interceptor1, interceptor2);
  }

  @Test
  void defaultValuesAreNull() {
    AsyncSupportConfigurer configurer = new AsyncSupportConfigurer();

    assertThat(configurer.taskExecutor).isNull();
    assertThat(configurer.timeout).isNull();
    assertThat(configurer.callableInterceptors).isNull();
    assertThat(configurer.deferredResultInterceptors).isNull();
  }

}