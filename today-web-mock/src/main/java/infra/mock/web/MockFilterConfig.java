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

package infra.mock.web;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import infra.lang.Assert;
import infra.lang.Nullable;
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
