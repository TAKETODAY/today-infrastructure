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

import java.util.concurrent.Callable;

import infra.web.ServerHttpExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AsyncInterceptorDefaultsTests {

  @Test
  void defaultCallableInterceptorDoesNotInterfereWithProcessing() throws Exception {
    ServerHttpExchange request = mock(ServerHttpExchange.class);
    Callable<String> task = () -> "result";
    CallableProcessingInterceptor interceptor = new CallableProcessingInterceptor() { };
    interceptor.beforeConcurrentHandling(request, task);
    interceptor.preProcess(request, task);
    interceptor.postProcess(request, task, task.call());
    assertThat(interceptor.handleTimeout(request, task)).isSameAs(CallableProcessingInterceptor.RESULT_NONE);
    assertThat(interceptor.handleError(request, task, new IllegalStateException())).isSameAs(CallableProcessingInterceptor.RESULT_NONE);
    interceptor.afterCompletion(request, task);
    verifyNoInteractions(request);
  }

  @Test
  void timeoutInterceptorSuppliesTimeoutExceptionForBothAsyncTypes() {
    ServerHttpExchange request = mock(ServerHttpExchange.class);
    var interceptor = new TimeoutAsyncProcessingInterceptor();
    assertThat(interceptor.handleTimeout(request, () -> "result")).isInstanceOf(AsyncRequestTimeoutException.class);
    DeferredResult<String> deferred = new DeferredResult<>();
    assertThat(interceptor.handleTimeout(request, deferred)).isFalse();
    assertThat(deferred.getResult()).isInstanceOf(AsyncRequestTimeoutException.class);
  }

}
