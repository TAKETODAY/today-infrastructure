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

package infra.mock.web;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import infra.lang.Assert;
import infra.mock.api.MockConfig;
import infra.mock.api.MockContext;

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
