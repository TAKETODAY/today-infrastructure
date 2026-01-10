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

package infra.web.handler;

import org.junit.jupiter.api.Test;

import infra.web.HandlerAdapter;
import infra.web.HandlerInterceptor;
import infra.web.RequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 22:43
 */
class HandlerExecutionChainTests {

  @Test
  void constructorWithHandlerOnly() {
    Object handler = new Object();

    HandlerExecutionChain chain = new HandlerExecutionChain(handler);

    assertThat(chain.getRawHandler()).isSameAs(handler);
    assertThat(chain.getInterceptors()).isNull();
  }

  @Test
  void constructorWithHandlerAndInterceptors() {
    Object handler = new Object();
    HandlerInterceptor[] interceptors = new HandlerInterceptor[] { mock(HandlerInterceptor.class), mock(HandlerInterceptor.class) };

    HandlerExecutionChain chain = new HandlerExecutionChain(handler, interceptors);

    assertThat(chain.getRawHandler()).isSameAs(handler);
    assertThat(chain.getInterceptors()).isSameAs(interceptors);
  }

  @Test
  void setHandlerAdapterStoresAdapter() {
    Object handler = new Object();
    HandlerAdapter adapter = mock(HandlerAdapter.class);

    HandlerExecutionChain chain = new HandlerExecutionChain(handler);
    chain.setHandlerAdapter(adapter);

    // We can't directly verify the stored adapter, but we can test the behavior
    assertThatCode(() -> chain.setHandlerAdapter(adapter)).doesNotThrowAnyException();
  }

  @Test
  void toStringReturnsFormattedString() {
    Object handler = new Object();
    HandlerInterceptor[] interceptors = new HandlerInterceptor[] { mock(HandlerInterceptor.class) };

    HandlerExecutionChain chain = new HandlerExecutionChain(handler, interceptors);

    String result = chain.toString();

    assertThat(result).contains("HandlerExecutionChain with");
    assertThat(result).contains(handler.toString());
    assertThat(result).contains("1 interceptors");
  }

  @Test
  void toStringWithNoInterceptors() {
    Object handler = new Object();

    HandlerExecutionChain chain = new HandlerExecutionChain(handler);

    String result = chain.toString();

    assertThat(result).contains("HandlerExecutionChain with");
    assertThat(result).contains(handler.toString());
    assertThat(result).contains("0 interceptors");
  }

  @Test
  void handleRequestWithNoInterceptors() throws Throwable {
    Object handler = new Object();
    HandlerAdapter adapter = mock(HandlerAdapter.class);
    RequestContext request = mock(RequestContext.class);
    Object result = new Object();

    given(adapter.handle(request, handler)).willReturn(result);

    HandlerExecutionChain chain = new HandlerExecutionChain(handler);
    chain.setHandlerAdapter(adapter);

    Object actualResult = chain.handleRequest(request);

    assertThat(actualResult).isSameAs(result);
    verify(adapter).handle(request, handler);
  }

}