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

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.MockConfig;
import cn.taketoday.mock.api.MockContext;

/**
 * Mock implementation of the {@link MockConfig} interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 4.0
 */
public class MockMockConfig implements MockConfig {

  private final MockContext mockContext;

  private final String mockName;

  private final Map<String, String> initParameters = new LinkedHashMap<>();

  /**
   * Create a new MockServletConfig with a default {@link MockContextImpl}.
   */
  public MockMockConfig() {
    this(null, "");
  }

  /**
   * Create a new MockServletConfig with a default {@link MockContextImpl}.
   *
   * @param mockName the name of the servlet
   */
  public MockMockConfig(String mockName) {
    this(null, mockName);
  }

  /**
   * Create a new MockServletConfig.
   *
   * @param mockContext the MockContext that the servlet runs in
   */
  public MockMockConfig(@Nullable MockContext mockContext) {
    this(mockContext, "");
  }

  /**
   * Create a new MockServletConfig.
   *
   * @param mockContext the MockContext that the servlet runs in
   * @param mockName the name of the servlet
   */
  public MockMockConfig(@Nullable MockContext mockContext, String mockName) {
    this.mockContext = (mockContext != null ? mockContext : new MockContextImpl());
    this.mockName = mockName;
  }

  @Override
  public String getMockName() {
    return this.mockName;
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
