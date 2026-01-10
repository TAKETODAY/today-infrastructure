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

import infra.lang.Assert;
import infra.mock.api.Filter;
import infra.mock.api.FilterChain;
import infra.mock.api.FilterConfig;
import infra.mock.api.MockApi;
import infra.mock.api.MockException;
import infra.mock.api.MockRequest;
import infra.mock.api.MockResponse;

/**
 * Implementation of the {@link FilterConfig} interface which
 * simply passes the call through to a given Filter/FilterChain combination
 * (indicating the next Filter in the chain along with the FilterChain that it is
 * supposed to work on) or to a given Web (indicating the end of the chain).
 *
 * @author Juergen Hoeller
 * @see Filter
 * @see MockApi
 * @see MockFilterChain
 * @since 4.0
 */
public class PassThroughFilterChain implements FilterChain {

  @Nullable
  private Filter filter;

  @Nullable
  private FilterChain nextFilterChain;

  @Nullable
  private MockApi mockApi;

  /**
   * Create a new PassThroughFilterChain that delegates to the given Filter,
   * calling it with the given FilterChain.
   *
   * @param filter the Filter to delegate to
   * @param nextFilterChain the FilterChain to use for that next Filter
   */
  public PassThroughFilterChain(Filter filter, FilterChain nextFilterChain) {
    Assert.notNull(filter, "Filter is required");
    Assert.notNull(nextFilterChain, "'FilterChain is required");
    this.filter = filter;
    this.nextFilterChain = nextFilterChain;
  }

  /**
   * Create a new PassThroughFilterChain that delegates to the given A Mock API.
   *
   * @param mockApi the Mock to delegate to
   */
  public PassThroughFilterChain(MockApi mockApi) {
    Assert.notNull(mockApi, "Mock API is required");
    this.mockApi = mockApi;
  }

  @Override
  public void doFilter(MockRequest request, MockResponse response) throws MockException, IOException {
    if (this.filter != null) {
      this.filter.doFilter(request, response, this.nextFilterChain);
    }
    else {
      Assert.state(this.mockApi != null, "Neither a Filter not a Mock API set");
      this.mockApi.service(request, response);
    }
  }

}
