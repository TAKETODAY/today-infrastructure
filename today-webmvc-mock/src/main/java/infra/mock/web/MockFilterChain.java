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

package infra.mock.web;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import infra.lang.Assert;
import infra.mock.api.Filter;
import infra.mock.api.FilterChain;
import infra.mock.api.FilterConfig;
import infra.mock.api.MockApi;
import infra.mock.api.MockException;
import infra.mock.api.MockRequest;
import infra.mock.api.MockResponse;
import infra.util.ObjectUtils;

/**
 * Mock implementation of the {@link FilterChain} interface.
 *
 * <p>A {@link MockFilterChain} can be configured with one or more filters and a
 * Mock API to invoke. The first time the chain is called, it invokes all filters
 * and saves the request and response. Subsequent invocations
 * raise an {@link IllegalStateException} unless {@link #reset()} is called.
 *
 * @author Juergen Hoeller
 * @author Rob Winch
 * @author Rossen Stoyanchev
 * @see MockFilterConfig
 * @see PassThroughFilterChain
 * @since 4.0
 */
public class MockFilterChain implements FilterChain {

  @Nullable
  private MockRequest request;

  @Nullable
  private MockResponse response;

  private final List<Filter> filters;

  @Nullable
  private Iterator<Filter> iterator;

  /**
   * Register a single do-nothing {@link Filter} implementation. The first
   * invocation saves the request and response. Subsequent invocations raise
   * an {@link IllegalStateException} unless {@link #reset()} is called.
   */
  public MockFilterChain() {
    this.filters = Collections.emptyList();
  }

  /**
   * @since 4.0
   */
  public MockFilterChain(MockApi mockApi) {
    this.filters = initFilterList(mockApi);
  }

  /**
   * @param mockApi the {@link MockApi} to invoke in this {@link FilterChain}
   * @param filters the {@link Filter}'s to invoke in this {@link FilterChain}
   * @since 4.0
   */
  public MockFilterChain(MockApi mockApi, Filter... filters) {
    Assert.notNull(filters, "filters cannot be null");
    Assert.noNullElements(filters, "filters cannot contain null values");
    this.filters = initFilterList(mockApi, filters);
  }

  private static List<Filter> initFilterList(MockApi mockApi, Filter... filters) {
    Filter[] allFilters = ObjectUtils.addObjectToArray(filters, new MockFilterProxy(mockApi));
    return Arrays.asList(allFilters);
  }

  /**
   * Return the request that {@link #doFilter} has been called with.
   */
  @Nullable
  public MockRequest getRequest() {
    return this.request;
  }

  /**
   * Return the response that {@link #doFilter} has been called with.
   */
  @Nullable
  public MockResponse getResponse() {
    return this.response;
  }

  /**
   * Invoke registered {@link Filter Filters} and/or {@link MockApi} also saving the
   * request and response.
   */
  @Override
  public void doFilter(MockRequest request, MockResponse response) throws IOException, MockException {
    Assert.notNull(request, "Request is required");
    Assert.notNull(response, "Response is required");
    Assert.state(this.request == null, "This FilterChain has already been called!");

    if (this.iterator == null) {
      this.iterator = this.filters.iterator();
    }

    if (this.iterator.hasNext()) {
      Filter nextFilter = this.iterator.next();
      nextFilter.doFilter(request, response, this);
    }

    this.request = request;
    this.response = response;
  }

  /**
   * Reset the {@link MockFilterChain} allowing it to be invoked again.
   */
  public void reset() {
    this.request = null;
    this.response = null;
    this.iterator = null;
  }

  /**
   *
   */
  private static final class MockFilterProxy implements Filter {

    private final MockApi delegateMockApi;

    private MockFilterProxy(MockApi mockApi) {
      Assert.notNull(mockApi, "servlet cannot be null");
      this.delegateMockApi = mockApi;
    }

    @Override
    public void doFilter(MockRequest request, MockResponse response, FilterChain chain)
            throws IOException, MockException {

      this.delegateMockApi.service(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws MockException {
    }

    @Override
    public void destroy() {
    }

    @Override
    public String toString() {
      return this.delegateMockApi.toString();
    }
  }

}
