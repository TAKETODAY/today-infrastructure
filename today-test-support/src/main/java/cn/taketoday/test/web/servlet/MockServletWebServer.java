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

package cn.taketoday.test.web.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.mock.web.MockSessionCookieConfig;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Base class for Mock {@code ServletWebServer} implementations. Reduces the amount of
 * code that would otherwise be duplicated in {@code spring-boot},
 * {@code autoconfigure} and {@code actuator}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 4.0
 */
public abstract class MockServletWebServer {

  private ServletContext servletContext;

  private final Initializer[] initializers;

  private final List<RegisteredServlet> registeredServlets = new ArrayList<>();

  private final List<RegisteredFilter> registeredFilters = new ArrayList<>();

  private final int port;

  public MockServletWebServer(Initializer[] initializers, int port) {
    this.initializers = initializers;
    this.port = port;
    initialize();
  }

  private void initialize() {
    try {
      this.servletContext = mock(ServletContext.class);
      lenient().doAnswer((invocation) -> {
        RegisteredServlet registeredServlet = new RegisteredServlet(invocation.getArgument(1));
        MockServletWebServer.this.registeredServlets.add(registeredServlet);
        return registeredServlet.getRegistration();
      }).when(this.servletContext).addServlet(anyString(), any(Servlet.class));
      lenient().doAnswer((invocation) -> {
        RegisteredFilter registeredFilter = new RegisteredFilter(invocation.getArgument(1));
        MockServletWebServer.this.registeredFilters.add(registeredFilter);
        return registeredFilter.getRegistration();
      }).when(this.servletContext).addFilter(anyString(), any(Filter.class));
      final SessionCookieConfig sessionCookieConfig = new MockSessionCookieConfig();
      given(this.servletContext.getSessionCookieConfig()).willReturn(sessionCookieConfig);
      final Map<String, String> initParameters = new HashMap<>();
      lenient().doAnswer((invocation) -> {
        initParameters.put(invocation.getArgument(0), invocation.getArgument(1));
        return null;
      }).when(this.servletContext).setInitParameter(anyString(), anyString());
      lenient().when(this.servletContext.getInitParameterNames())
              .thenReturn(Collections.enumeration(initParameters.keySet()));
      lenient().doAnswer((invocation) -> initParameters.get(invocation.getArgument(0))).when(this.servletContext)
              .getInitParameter(anyString());
      lenient().when(this.servletContext.getAttributeNames()).thenReturn(Collections.emptyEnumeration());
      for (Initializer initializer : this.initializers) {
        initializer.onStartup(this.servletContext);
      }
    }
    catch (ServletException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void stop() {
    this.servletContext = null;
    this.registeredServlets.clear();
  }

  public ServletContext getServletContext() {
    return this.servletContext;
  }

  public Servlet[] getServlets() {
    Servlet[] servlets = new Servlet[this.registeredServlets.size()];
    Arrays.setAll(servlets, (i) -> this.registeredServlets.get(i).getServlet());
    return servlets;
  }

  public RegisteredServlet getRegisteredServlet(int index) {
    return getRegisteredServlets().get(index);
  }

  public List<RegisteredServlet> getRegisteredServlets() {
    return this.registeredServlets;
  }

  public RegisteredFilter getRegisteredFilters(int index) {
    return getRegisteredFilters().get(index);
  }

  public List<RegisteredFilter> getRegisteredFilters() {
    return this.registeredFilters;
  }

  public int getPort() {
    return this.port;
  }

  /**
   * A registered servlet.
   */
  public static class RegisteredServlet {

    private final Servlet servlet;

    private final ServletRegistration.Dynamic registration;

    public RegisteredServlet(Servlet servlet) {
      this.servlet = servlet;
      this.registration = mock(ServletRegistration.Dynamic.class);
    }

    public ServletRegistration.Dynamic getRegistration() {
      return this.registration;
    }

    public Servlet getServlet() {
      return this.servlet;
    }

  }

  /**
   * A registered filter.
   */
  public static class RegisteredFilter {

    private final Filter filter;

    private final FilterRegistration.Dynamic registration;

    public RegisteredFilter(Filter filter) {
      this.filter = filter;
      this.registration = mock(FilterRegistration.Dynamic.class);
    }

    public FilterRegistration.Dynamic getRegistration() {
      return this.registration;
    }

    public Filter getFilter() {
      return this.filter;
    }

  }

  /**
   * Initializer (usually implement by adapting {@code Initializer}).
   */
  @FunctionalInterface
  protected interface Initializer {

    void onStartup(ServletContext context) throws ServletException;

  }

}
