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

/**
 * A chain of {@linkplain Filter web filters} that wraps the request
 * processing pipeline in {@link DispatcherHandler}.
 *
 * <p>Filters invoke the next element in the chain via
 * {@link #doFilter(RequestContext)}. When all filters have been given
 * control, the chain delegates to the terminal {@link DispatcherHandler} —
 * typically the dispatcher's internal request processing logic.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Filter
 * @see DispatcherHandler
 * @since 5.0
 */
public class FilterChain {

  private int index;

  private final Filter[] filters;

  private final DispatcherHandler dispatcherHandler;

  /**
   * Create a new {@code FilterChain} with the given filters and terminal handler.
   *
   * @param filters the list of web filters to apply; must not be null
   * @param dispatcherHandler the handler to invoke when all filters have completed
   */
  FilterChain(Filter[] filters, DispatcherHandler dispatcherHandler) {
    this.filters = filters;
    this.dispatcherHandler = dispatcherHandler;
  }

  /**
   * Proceed with the next filter in the chain, or invoke the terminal handler
   * if no filters remain.
   *
   * @param request the current request context
   * @throws Throwable if any filter or the terminal handler fails
   */
  public void doFilter(RequestContext request) throws Throwable {
    final Filter[] filters = this.filters;
    if (index < filters.length) {
      filters[index++].doFilter(request, this);
    }
    else {
      dispatcherHandler.handleRequestInternal(request);
    }
  }

}
