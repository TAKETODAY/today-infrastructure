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

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import infra.lang.Assert;
import infra.mock.api.Filter;
import infra.mock.api.FilterConfig;
import infra.mock.api.MockContext;

/**
 * Mock implementation of the {@link FilterConfig} interface.
 *
 * <p>Used for testing the web framework; also useful for testing
 * custom {@link Filter} implementations.
 *
 * @author Juergen Hoeller
 * @see MockFilterChain
 * @see PassThroughFilterChain
 * @since 4.0
 */
public class MockFilterConfig implements FilterConfig {

  private final MockContext mockContext;

  private final String filterName;

  private final Map<String, String> initParameters = new LinkedHashMap<>();

  /**
   * Create a new MockFilterConfig with a default {@link MockContextImpl}.
   */
  public MockFilterConfig() {
    this(null, "");
  }

  /**
   * Create a new MockFilterConfig with a default {@link MockContextImpl}.
   *
   * @param filterName the name of the filter
   */
  public MockFilterConfig(String filterName) {
    this(null, filterName);
  }

  /**
   * Create a new MockFilterConfig.
   *
   * @param mockContext the MockContext that the servlet runs in
   */
  public MockFilterConfig(@Nullable MockContext mockContext) {
    this(mockContext, "");
  }

  /**
   * Create a new MockFilterConfig.
   *
   * @param mockContext the MockContext that the servlet runs in
   * @param filterName the name of the filter
   */
  public MockFilterConfig(@Nullable MockContext mockContext, String filterName) {
    this.mockContext = (mockContext != null ? mockContext : new MockContextImpl());
    this.filterName = filterName;
  }

  @Override
  public String getFilterName() {
    return this.filterName;
  }

  @Override
  public MockContext getMockContext() {
    return this.mockContext;
  }

  public void addInitParameter(String name, String value) {
    Assert.notNull(name, "Parameter name is required");
    this.initParameters.put(name, value);
  }

  @Override
  public String getInitParameter(String name) {
    Assert.notNull(name, "Parameter name is required");
    return this.initParameters.get(name);
  }

  @Override
  public Enumeration<String> getInitParameterNames() {
    return Collections.enumeration(this.initParameters.keySet());
  }

}
