/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.mock.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.MockApi;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.mock.api.Filter;
import cn.taketoday.mock.api.FilterChain;
import cn.taketoday.mock.api.FilterConfig;
import cn.taketoday.mock.api.MockException;
import cn.taketoday.mock.api.MockRequest;
import cn.taketoday.mock.api.MockResponse;

/**
 * Mock implementation of the {@link FilterChain} interface.
 *
 * <p>A {@link MockFilterChain} can be configured with one or more filters and a
 * Servlet to invoke. The first time the chain is called, it invokes all filters
 * and the Servlet, and saves the request and response. Subsequent invocations
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
   * Create a FilterChain with a Servlet.
   *
   * @param mockApi the Servlet to invoke
   * @since 4.0
   */
  public MockFilterChain(MockApi mockApi) {
    this.filters = initFilterList(mockApi);
  }

  /**
   * Create a {@code FilterChain} with Filter's and a Servlet.
   *
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
   * A filter that simply delegates to a Servlet.
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
