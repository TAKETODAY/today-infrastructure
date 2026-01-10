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

package infra.web.handler;

import org.junit.jupiter.api.Test;

import infra.web.HandlerAdapter;
import infra.web.HandlerAdapterNotFoundException;
import infra.web.HttpRequestHandler;
import infra.web.RequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 23:15
 */
class HandlerAdaptersTests {

  @Test
  void constructorWithNullAdaptersThrowsException() {
    assertThatThrownBy(() -> new HandlerAdapters(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("HandlerAdapters is required");
  }

  @Test
  void constructorWithValidAdapters() {
    HandlerAdapter adapter1 = mock(HandlerAdapter.class);
    HandlerAdapter adapter2 = mock(HandlerAdapter.class);
    HandlerAdapter[] adapters = new HandlerAdapter[] { adapter1, adapter2 };

    HandlerAdapters handlerAdapters = new HandlerAdapters(adapters);

    assertThat(handlerAdapters).isNotNull();
  }

  @Test
  void supportsReturnsTrueWhenAdapterSupportsHandler() {
    HandlerAdapter adapter1 = mock(HandlerAdapter.class);
    HandlerAdapter adapter2 = mock(HandlerAdapter.class);
    given(adapter1.supports("testHandler")).willReturn(false);
    given(adapter2.supports("testHandler")).willReturn(true);
    HandlerAdapter[] adapters = new HandlerAdapter[] { adapter1, adapter2 };

    HandlerAdapters handlerAdapters = new HandlerAdapters(adapters);

    boolean result = handlerAdapters.supports("testHandler");

    assertThat(result).isTrue();
  }

  @Test
  void supportsReturnsFalseWhenNoAdapterSupportsHandler() {
    HandlerAdapter adapter1 = mock(HandlerAdapter.class);
    HandlerAdapter adapter2 = mock(HandlerAdapter.class);
    given(adapter1.supports("testHandler")).willReturn(false);
    given(adapter2.supports("testHandler")).willReturn(false);
    HandlerAdapter[] adapters = new HandlerAdapter[] { adapter1, adapter2 };

    HandlerAdapters handlerAdapters = new HandlerAdapters(adapters);

    boolean result = handlerAdapters.supports("testHandler");

    assertThat(result).isFalse();
  }

  @Test
  void selectAdapterReturnsMatchingAdapter() {
    HandlerAdapter adapter1 = mock(HandlerAdapter.class);
    HandlerAdapter adapter2 = mock(HandlerAdapter.class);
    given(adapter1.supports("testHandler")).willReturn(false);
    given(adapter2.supports("testHandler")).willReturn(true);
    HandlerAdapter[] adapters = new HandlerAdapter[] { adapter1, adapter2 };

    HandlerAdapters handlerAdapters = new HandlerAdapters(adapters);

    HandlerAdapter result = handlerAdapters.selectAdapter("testHandler");

    assertThat(result).isSameAs(adapter2);
  }

  @Test
  void selectAdapterReturnsNullWhenNoAdapterSupportsHandler() {
    HandlerAdapter adapter1 = mock(HandlerAdapter.class);
    HandlerAdapter adapter2 = mock(HandlerAdapter.class);
    given(adapter1.supports("testHandler")).willReturn(false);
    given(adapter2.supports("testHandler")).willReturn(false);
    HandlerAdapter[] adapters = new HandlerAdapter[] { adapter1, adapter2 };

    HandlerAdapters handlerAdapters = new HandlerAdapters(adapters);

    HandlerAdapter result = handlerAdapters.selectAdapter("testHandler");

    assertThat(result).isNull();
  }

  @Test
  void handleWithHttpRequestHandler() throws Throwable {
    HttpRequestHandler httpRequestHandler = mock(HttpRequestHandler.class);
    RequestContext context = mock(RequestContext.class);
    Object expectedResult = new Object();
    given(httpRequestHandler.handleRequest(context)).willReturn(expectedResult);

    HandlerAdapter[] adapters = new HandlerAdapter[0];
    HandlerAdapters handlerAdapters = new HandlerAdapters(adapters);

    Object result = handlerAdapters.handle(context, httpRequestHandler);

    assertThat(result).isSameAs(expectedResult);
    verify(httpRequestHandler).handleRequest(context);
  }

  @Test
  void handleWithSupportedHandlerAdapter() throws Throwable {
    HandlerAdapter adapter1 = mock(HandlerAdapter.class);
    HandlerAdapter adapter2 = mock(HandlerAdapter.class);
    RequestContext context = mock(RequestContext.class);
    Object handler = new Object();
    Object expectedResult = new Object();

    given(adapter1.supports(handler)).willReturn(false);
    given(adapter2.supports(handler)).willReturn(true);
    given(adapter2.handle(context, handler)).willReturn(expectedResult);

    HandlerAdapter[] adapters = new HandlerAdapter[] { adapter1, adapter2 };
    HandlerAdapters handlerAdapters = new HandlerAdapters(adapters);

    Object result = handlerAdapters.handle(context, handler);

    assertThat(result).isSameAs(expectedResult);
    verify(adapter2).handle(context, handler);
  }

  @Test
  void handleThrowsExceptionWhenNoAdapterSupportsHandler() throws Throwable {
    HandlerAdapter adapter1 = mock(HandlerAdapter.class);
    HandlerAdapter adapter2 = mock(HandlerAdapter.class);
    RequestContext context = mock(RequestContext.class);
    Object handler = new Object();

    given(adapter1.supports(handler)).willReturn(false);
    given(adapter2.supports(handler)).willReturn(false);

    HandlerAdapter[] adapters = new HandlerAdapter[] { adapter1, adapter2 };
    HandlerAdapters handlerAdapters = new HandlerAdapters(adapters);

    assertThatThrownBy(() -> handlerAdapters.handle(context, handler))
            .isInstanceOf(HandlerAdapterNotFoundException.class);
  }

}