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

package infra.web.mock;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import infra.lang.Assert;
import infra.util.ObjectUtils;
import infra.web.Filter;
import infra.web.FilterChain;
import infra.web.HttpContext;
import infra.web.mock.api.MockHandler;

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

  private HttpContext httpContext;

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
  public MockFilterChain(MockHandler mockHandler) {
    this.filters = initFilterList(mockHandler);
  }

  /**
   * @param mockHandler the {@link MockHandler} to invoke in this {@link FilterChain}
   * @param filters the {@link Filter}'s to invoke in this {@link FilterChain}
   * @since 4.0
   */
  public MockFilterChain(MockHandler mockHandler, Filter... filters) {
    Assert.notNull(filters, "filters cannot be null");
    Assert.noNullElements(filters, "filters cannot contain null values");
    this.filters = initFilterList(mockHandler, filters);
  }

  private static List<Filter> initFilterList(MockHandler mockHandler, Filter... filters) {
    Filter[] allFilters = ObjectUtils.addObjectToArray(filters, new MockFilterProxy(mockHandler));
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

  public HttpContext getContext() {
    return httpContext;
  }

  @Override
  public void doFilter(HttpContext context) throws Exception {
    this.httpContext = context;
    Assert.notNull(context, "Request is required");
    Assert.state(this.request == null, "This FilterChain has already been called!");

    if (this.iterator == null) {
      this.iterator = this.filters.iterator();
    }

    if (this.iterator.hasNext()) {
      Filter nextFilter = this.iterator.next();
      nextFilter.doFilter(context, this);
    }

    this.request = MockUtils.getMockRequest(context);
    this.response = MockUtils.getMockResponse(context);
  }

  /**
   * Reset the {@link MockFilterChain} allowing it to be invoked again.
   */
  public void reset() {
    this.request = null;
    this.response = null;
    this.iterator = null;
  }

  private static final class MockFilterProxy implements Filter {

    private final MockHandler delegateMockHandler;

    private MockFilterProxy(MockHandler mockHandler) {
      Assert.notNull(mockHandler, "mock cannot be null");
      this.delegateMockHandler = mockHandler;
    }

    @Override
    public void doFilter(HttpContext http, FilterChain chain) throws Exception {
      this.delegateMockHandler.service(MockUtils.getMockRequest(http), MockUtils.getMockResponse(http));
    }

    @Override
    public String toString() {
      return this.delegateMockHandler.toString();
    }
  }

}
