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

package infra.web.filter;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.web.Filter;
import infra.web.FilterChain;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/6/1 11:01
 */
class CompositeFilterTests {

  @Test
  void singleFilter() throws Throwable {
    MockFilter targetFilter = new MockFilter();

    CompositeFilter composite = new CompositeFilter();
    composite.setFilters(List.of(targetFilter));

    MockRequestContext context = new MockRequestContext();
    composite.doFilter(context, null);

    assertThat(context.getRequest().getAttribute("called")).isEqualTo(Boolean.TRUE);
  }

  @Test
  void noFiltersDelegatesToOriginalChain() throws Throwable {
    var chainInvoked = new boolean[] { false };
    FilterChain originalChain = request -> chainInvoked[0] = true;

    CompositeFilter composite = new CompositeFilter();
    composite.setFilters(List.of());
    composite.doFilter(new MockRequestContext(), originalChain);

    assertThat(chainInvoked[0]).isTrue();
  }

  @Test
  void multipleFiltersExecuteInOrder() throws Throwable {
    var order = new StringBuilder();
    CompositeFilter composite = new CompositeFilter();
    composite.setFilters(List.of(
            (request, chain) -> { order.append("A"); chain.doFilter(request); },
            (request, chain) -> { order.append("B"); chain.doFilter(request); }
    ));

    var chainInvoked = new boolean[] { false };
    FilterChain originalChain = request -> chainInvoked[0] = true;
    composite.doFilter(new MockRequestContext(), originalChain);

    assertThat(order.toString()).isEqualTo("AB");
    assertThat(chainInvoked[0]).isTrue();
  }

  @Test
  void shortCircuitSkipsSubsequentFiltersAndOriginalChain() throws Throwable {
    var order = new StringBuilder();
    CompositeFilter composite = new CompositeFilter();
    composite.setFilters(List.of(
            (request, chain) -> order.append("A"),
            (request, chain) -> order.append("B")
    ));

    var chainInvoked = new boolean[] { false };
    FilterChain originalChain = request -> chainInvoked[0] = true;
    composite.doFilter(new MockRequestContext(), originalChain);

    assertThat(order.toString()).isEqualTo("A");
    assertThat(chainInvoked[0]).isFalse();
  }

  @Test
  void exceptionInFilterPropagates() {
    var expected = new RuntimeException("filter failure");
    CompositeFilter composite = new CompositeFilter();
    composite.setFilters(List.of(
            (request, chain) -> { throw expected; }
    ));

    assertThatThrownBy(() -> composite.doFilter(new MockRequestContext(), null))
            .isSameAs(expected);
  }

  @Test
  void setFiltersCopiesList() {
    var original = List.<Filter>of(
            (request, chain) -> chain.doFilter(request)
    );
    CompositeFilter composite = new CompositeFilter();
    composite.setFilters(original);

    // Modify original list to verify composite has its own copy
    // (List.of is immutable, so we just verify the internal list is not the same reference)
    assertThat(composite).extracting("filters").isNotSameAs(original);
  }

  public static class MockFilter implements Filter {

    @Override
    public void doFilter(RequestContext request, FilterChain chain) throws Throwable {
      request.setAttribute("called", Boolean.TRUE);
    }

  }

}