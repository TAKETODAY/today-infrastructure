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

package infra.web.filter;

import java.util.ArrayList;
import java.util.List;

import infra.web.Filter;
import infra.web.FilterChain;
import infra.web.RequestContext;

/**
 * A generic composite mock {@link Filter} that just delegates its behavior
 * to a chain (list) of user-supplied filters, achieving the functionality of a
 * {@link FilterChain}, but conveniently using only {@link Filter} instances.
 *
 * <p>This is useful for filters that require dependency injection, and can
 * therefore be set up in a Infra application context. Typically, this
 * composite would be used in conjunction with {@link DelegatingFilterProxy},
 * so that it can be declared in Infra but applied to a mock context.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class CompositeFilter implements Filter {

  private List<? extends Filter> filters = new ArrayList<>();

  public void setFilters(List<? extends Filter> filters) {
    this.filters = new ArrayList<>(filters);
  }

  /**
   * Forms a temporary chain from the list of delegate filters supplied ({@link #setFilters})
   * and executes them in order. Each filter delegates to the next one in the list, achieving
   * the normal behavior of a {@link FilterChain}, despite the fact that this is a {@link Filter}.
   *
   * @see Filter#doFilter(RequestContext, FilterChain)
   */
  @Override
  public void doFilter(RequestContext request, FilterChain chain) throws Exception {
    new VirtualFilterChain(chain, this.filters).doFilter(request);
  }

  private static class VirtualFilterChain implements FilterChain {

    private final FilterChain originalChain;

    private final List<? extends Filter> additionalFilters;

    private int currentPosition = 0;

    public VirtualFilterChain(FilterChain chain, List<? extends Filter> additionalFilters) {
      this.originalChain = chain;
      this.additionalFilters = additionalFilters;
    }

    @Override
    public void doFilter(RequestContext request) throws Exception {
      if (this.currentPosition == this.additionalFilters.size()) {
        this.originalChain.doFilter(request);
      }
      else {
        this.currentPosition++;
        Filter nextFilter = this.additionalFilters.get(this.currentPosition - 1);
        nextFilter.doFilter(request, this);
      }
    }
  }

}
