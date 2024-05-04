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

package cn.taketoday.web.testfixture.servlet;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.mock.FilterConfig;
import cn.taketoday.web.mock.ServletContext;

/**
 * Mock implementation of the {@link cn.taketoday.web.mock.FilterConfig} interface.
 *
 * <p>Used for testing the web framework; also useful for testing
 * custom {@link cn.taketoday.web.mock.Filter} implementations.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/9/11 18:51
 */
public class MockFilterConfig implements FilterConfig {

  private final ServletContext servletContext;

  private final String filterName;

  private final Map<String, String> initParameters = new LinkedHashMap<>();

  /**
   * Create a new MockFilterConfig with a default {@link MockServletContext}.
   */
  public MockFilterConfig() {
    this(null, "");
  }

  /**
   * Create a new MockFilterConfig with a default {@link MockServletContext}.
   *
   * @param filterName the name of the filter
   */
  public MockFilterConfig(String filterName) {
    this(null, filterName);
  }

  /**
   * Create a new MockFilterConfig.
   *
   * @param servletContext the ServletContext that the servlet runs in
   */
  public MockFilterConfig(@Nullable ServletContext servletContext) {
    this(servletContext, "");
  }

  /**
   * Create a new MockFilterConfig.
   *
   * @param servletContext the ServletContext that the servlet runs in
   * @param filterName the name of the filter
   */
  public MockFilterConfig(@Nullable ServletContext servletContext, String filterName) {
    this.servletContext = (servletContext != null ? servletContext : new MockServletContext());
    this.filterName = filterName;
  }

  @Override
  public String getFilterName() {
    return this.filterName;
  }

  @Override
  public ServletContext getServletContext() {
    return this.servletContext;
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
