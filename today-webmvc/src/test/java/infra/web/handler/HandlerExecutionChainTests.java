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