/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.mock.web;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

/**
 * Mock implementation of the {@link ServletConfig} interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 4.0
 */
public class MockServletConfig implements ServletConfig {

  private final ServletContext servletContext;

  private final String servletName;

  private final Map<String, String> initParameters = new LinkedHashMap<>();

  /**
   * Create a new MockServletConfig with a default {@link MockServletContext}.
   */
  public MockServletConfig() {
    this(null, "");
  }

  /**
   * Create a new MockServletConfig with a default {@link MockServletContext}.
   *
   * @param servletName the name of the servlet
   */
  public MockServletConfig(String servletName) {
    this(null, servletName);
  }

  /**
   * Create a new MockServletConfig.
   *
   * @param servletContext the ServletContext that the servlet runs in
   */
  public MockServletConfig(@Nullable ServletContext servletContext) {
    this(servletContext, "");
  }

  /**
   * Create a new MockServletConfig.
   *
   * @param servletContext the ServletContext that the servlet runs in
   * @param servletName the name of the servlet
   */
  public MockServletConfig(@Nullable ServletContext servletContext, String servletName) {
    this.servletContext = (servletContext != null ? servletContext : new MockServletContext());
    this.servletName = servletName;
  }

  @Override
  public String getServletName() {
    return this.servletName;
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
