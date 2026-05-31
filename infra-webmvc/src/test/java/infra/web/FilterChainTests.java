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

package infra.web;

import org.junit.jupiter.api.Test;

import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link FilterChain}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class FilterChainTests {

  @Test
  void noFiltersDelegatesToDispatcherHandler() throws Throwable {
    DispatcherHandler handler = spy(DispatcherHandler.class);
    FilterChain chain = new FilterChain(new Filter[0], handler);
    MockRequestContext request = new MockRequestContext();

    chain.doFilter(request);

    verify(handler).handleRequestInternal(request);
  }

  @Test
  void singleFilterProceedsToHandler() throws Throwable {
    DispatcherHandler handler = spy(DispatcherHandler.class);
    FilterChain chain = new FilterChain(new Filter[] {
            (req, ch) -> ch.doFilter(req)
    }, handler);
    MockRequestContext request = new MockRequestContext();

    chain.doFilter(request);

    verify(handler).handleRequestInternal(request);
  }

  @Test
  void multipleFiltersExecuteInOrder() throws Throwable {
    DispatcherHandler handler = spy(DispatcherHandler.class);
    var order = new StringBuilder();
    FilterChain chain = new FilterChain(new Filter[] {
            (req, ch) -> {
              order.append("A");
              ch.doFilter(req);
            },
            (req, ch) -> {
              order.append("B");
              ch.doFilter(req);
            },
    }, handler);
    MockRequestContext request = new MockRequestContext();

    chain.doFilter(request);

    assertThat(order.toString()).isEqualTo("AB");
    verify(handler).handleRequestInternal(request);
  }

  @Test
  void filterCanShortCircuit() throws Throwable {
    DispatcherHandler handler = spy(DispatcherHandler.class);
    FilterChain chain = new FilterChain(new Filter[] {
            (req, ch) -> { /* do not proceed */ }
    }, handler);
    MockRequestContext request = new MockRequestContext();

    chain.doFilter(request);

    verify(handler, never()).handleRequestInternal(any());
  }

  @Test
  void shortCircuitSkipsSubsequentFilters() throws Throwable {
    DispatcherHandler handler = spy(DispatcherHandler.class);
    var executed = new StringBuilder();
    FilterChain chain = new FilterChain(new Filter[] {
            (req, ch) -> { executed.append("A"); /* short-circuit */ },
            (req, ch) -> { executed.append("B"); ch.doFilter(req); },
    }, handler);
    MockRequestContext request = new MockRequestContext();

    chain.doFilter(request);

    assertThat(executed.toString()).isEqualTo("A");
    verify(handler, never()).handleRequestInternal(any());
  }

  @Test
  void exceptionInFilterPropagates() {
    DispatcherHandler handler = spy(DispatcherHandler.class);
    var expected = new RuntimeException("filter failure");
    FilterChain chain = new FilterChain(new Filter[] {
            (req, ch) -> { throw expected; }
    }, handler);
    MockRequestContext request = new MockRequestContext();

    assertThatThrownBy(() -> chain.doFilter(request))
            .isSameAs(expected);
  }

  @Test
  void exceptionInFilterSkipsHandlerAndSubsequentFilters() throws Throwable {
    DispatcherHandler handler = spy(DispatcherHandler.class);
    var expected = new RuntimeException("filter failure");
    var executed = new StringBuilder();
    FilterChain chain = new FilterChain(new Filter[] {
            (req, ch) -> { executed.append("A"); throw expected; },
            (req, ch) -> { executed.append("B"); ch.doFilter(req); },
    }, handler);
    MockRequestContext request = new MockRequestContext();

    assertThatThrownBy(() -> chain.doFilter(request))
            .isSameAs(expected);

    assertThat(executed.toString()).isEqualTo("A");
    verify(handler, never()).handleRequestInternal(any());
  }

  @Test
  void exceptionFromTerminalHandlerPropagates() {
    var expected = new RuntimeException("handler failure");
    DispatcherHandler handler = new DispatcherHandler() {
      @Override
      void handleRequestInternal(RequestContext request) throws Throwable {
        throw expected;
      }
    };
    FilterChain chain = new FilterChain(new Filter[0], handler);
    MockRequestContext request = new MockRequestContext();

    assertThatThrownBy(() -> chain.doFilter(request))
            .isSameAs(expected);
  }

}
