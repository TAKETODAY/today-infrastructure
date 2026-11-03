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

package infra.http.service.invoker;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import infra.core.ParameterizedTypeReference;
import infra.http.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/2/17 16:44
 */
class HttpServiceProxyFactoryTests {

  @Test
  void requestValuesProcessorAddedAfterBuildDoesNotAffectFactory() {
    var builder = HttpServiceProxyFactory.forAdapter(mock(HttpExchangeAdapter.class));
    var factory = builder.build();

    var processorInvoked = new AtomicBoolean();
    builder.httpRequestValuesProcessor((method, parameters, arguments, requestValues) ->
            processorInvoked.set(true));

    factory.createClient(Service.class).execute();
    assertThat(processorInvoked).isFalse();

    var newFactory = builder.build();
    newFactory.createClient(Service.class).execute();
    assertThat(processorInvoked).isTrue();
  }

  @Test
  void proxyFactoryCustomizerAddedAfterBuildDoesNotAffectFactory() {
    var adapter = mock(HttpExchangeAdapter.class);
    given(adapter.exchangeForBody(any(), eq(ParameterizedTypeReference.forType(String.class))))
            .willReturn("decorated");
    var builder = HttpServiceProxyFactory.forAdapter(adapter);
    var factory = builder.build();

    var interceptor = (MethodInterceptor) invocation -> "intercepted";
    builder.proxyFactoryCustomizer((proxyFactory, serviceType) ->
            proxyFactory.addAdvice(0, interceptor));

    assertThat(factory.createClient(Service.class).execute()).isEqualTo("decorated");

    var newFactory = builder.build();
    assertThat(newFactory.createClient(Service.class).execute()).isEqualTo("intercepted");
  }

  @Test
  void proxyFactoryCustomizer() {
    var interceptor = (MethodInterceptor) invocation -> "intercepted";
    var proxyFactory = HttpServiceProxyFactory.forAdapter(mock(HttpExchangeAdapter.class))
            .proxyFactoryCustomizer((factory, serviceType) -> factory.addAdvice(0, interceptor))
            .build();

    String result = proxyFactory.createClient(Service.class).execute();

    assertThat(result).isEqualTo("intercepted");
  }

  private interface Service {

    @GetExchange
    String execute();
  }

}
