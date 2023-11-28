/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.testfixture.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import jakarta.el.ELContext;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.PageContext;

/**
 * Mock implementation of the {@link PageContext} interface.
 * Only necessary for testing applications when testing custom JSP tags.
 *
 * <p>Note: Expects initialization via the constructor rather than via the
 * {@code PageContext.initialize} method. Does not support writing to a
 * JspWriter, request dispatching, or {@code handlePageException} calls.
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 */
public class MockPageContext extends PageContext {

  private final ServletContext servletContext;

  private final HttpServletRequest request;

  private final HttpServletResponse response;

  private final ServletConfig servletConfig;

  private final Map<String, Object> attributes = new LinkedHashMap<>();

  @Nullable
  private JspWriter out;

  /**
   * Create new MockPageContext with a default {@link MockServletContext},
   * {@link MockHttpServletRequest}, {@link MockHttpServletResponse},
   * {@link MockServletConfig}.
   */
  public MockPageContext() {
    this(null, null, null, null);
  }

  /**
   * Create new MockPageContext with a default {@link MockHttpServletRequest},
   * {@link MockHttpServletResponse}, {@link MockServletConfig}.
   *
   * @param servletContext the ServletContext that the JSP page runs in
   * (only necessary when actually accessing the ServletContext)
   */
  public MockPageContext(@Nullable ServletContext servletContext) {
    this(servletContext, null, null, null);
  }

  /**
   * Create new MockPageContext with a MockHttpServletResponse,
   * MockServletConfig.
   *
   * @param servletContext the ServletContext that the JSP page runs in
   * @param request the current HttpServletRequest
   * (only necessary when actually accessing the request)
   */
  public MockPageContext(@Nullable ServletContext servletContext, @Nullable HttpServletRequest request) {
    this(servletContext, request, null, null);
  }

  /**
   * Create new MockPageContext with a MockServletConfig.
   *
   * @param servletContext the ServletContext that the JSP page runs in
   * @param request the current HttpServletRequest
   * @param response the current HttpServletResponse
   * (only necessary when actually writing to the response)
   */
  public MockPageContext(@Nullable ServletContext servletContext, @Nullable HttpServletRequest request,
          @Nullable HttpServletResponse response) {

    this(servletContext, request, response, null);
  }

  /**
   * Create new MockServletConfig.
   *
   * @param servletContext the ServletContext that the JSP page runs in
   * @param request the current HttpServletRequest
   * @param response the current HttpServletResponse
   * @param servletConfig the ServletConfig (hardly ever accessed from within a tag)
   */
  public MockPageContext(@Nullable ServletContext servletContext, @Nullable HttpServletRequest request,
          @Nullable HttpServletResponse response, @Nullable ServletConfig servletConfig) {

    this.servletContext = (servletContext != null ? servletContext : new MockServletContext());
    this.request = (request != null ? request : new MockHttpServletRequest(servletContext));
    this.response = (response != null ? response : new MockHttpServletResponse());
    this.servletConfig = (servletConfig != null ? servletConfig : new MockServletConfig(servletContext));
  }

  @Override
  public void initialize(
          Servlet servlet, ServletRequest request, ServletResponse response,
          String errorPageURL, boolean needsSession, int bufferSize, boolean autoFlush) {

    throw new UnsupportedOperationException("Use appropriate constructor");
  }

  @Override
  public void release() {
  }

  @Override
  public void setAttribute(String name, @Nullable Object value) {
    Assert.notNull(name, "Attribute name is required");
    if (value != null) {
      this.attributes.put(name, value);
    }
    else {
      this.attributes.remove(name);
    }
  }

  @Override
  public void setAttribute(String name, @Nullable Object value, int scope) {
    Assert.notNull(name, "Attribute name is required");
    switch (scope) {
      case PageContext.PAGE_SCOPE -> setAttribute(name, value);
      case PageContext.REQUEST_SCOPE -> this.request.setAttribute(name, value);
      case PageContext.SESSION_SCOPE -> this.request.getSession().setAttribute(name, value);
      case PageContext.APPLICATION_SCOPE -> this.servletContext.setAttribute(name, value);
      default -> throw new IllegalArgumentException("Invalid scope: " + scope);
    }
  }

  @Override
  @Nullable
  public Object getAttribute(String name) {
    Assert.notNull(name, "Attribute name is required");
    return this.attributes.get(name);
  }

  @Override
  @Nullable
  public Object getAttribute(String name, int scope) {
    Assert.notNull(name, "Attribute name is required");
    return switch (scope) {
      case PageContext.PAGE_SCOPE -> getAttribute(name);
      case PageContext.REQUEST_SCOPE -> this.request.getAttribute(name);
      case PageContext.SESSION_SCOPE -> {
        HttpSession session = this.request.getSession(false);
        yield (session != null ? session.getAttribute(name) : null);
      }
      case PageContext.APPLICATION_SCOPE -> this.servletContext.getAttribute(name);
      default -> throw new IllegalArgumentException("Invalid scope: " + scope);
    };
  }

  @Override
  @Nullable
  public Object findAttribute(String name) {
    Object value = getAttribute(name);
    if (value == null) {
      value = getAttribute(name, PageContext.REQUEST_SCOPE);
      if (value == null) {
        value = getAttribute(name, PageContext.SESSION_SCOPE);
        if (value == null) {
          value = getAttribute(name, PageContext.APPLICATION_SCOPE);
        }
      }
    }
    return value;
  }

  @Override
  public void removeAttribute(String name) {
    Assert.notNull(name, "Attribute name is required");
    this.removeAttribute(name, PageContext.PAGE_SCOPE);
    this.removeAttribute(name, PageContext.REQUEST_SCOPE);
    this.removeAttribute(name, PageContext.SESSION_SCOPE);
    this.removeAttribute(name, PageContext.APPLICATION_SCOPE);
  }

  @Override
  public void removeAttribute(String name, int scope) {
    Assert.notNull(name, "Attribute name is required");
    switch (scope) {
      case PageContext.PAGE_SCOPE -> this.attributes.remove(name);
      case PageContext.REQUEST_SCOPE -> this.request.removeAttribute(name);
      case PageContext.SESSION_SCOPE -> this.request.getSession().removeAttribute(name);
      case PageContext.APPLICATION_SCOPE -> this.servletContext.removeAttribute(name);
      default -> throw new IllegalArgumentException("Invalid scope: " + scope);
    }
  }

  @Override
  public int getAttributesScope(String name) {
    if (getAttribute(name) != null) {
      return PageContext.PAGE_SCOPE;
    }
    else if (getAttribute(name, PageContext.REQUEST_SCOPE) != null) {
      return PageContext.REQUEST_SCOPE;
    }
    else if (getAttribute(name, PageContext.SESSION_SCOPE) != null) {
      return PageContext.SESSION_SCOPE;
    }
    else if (getAttribute(name, PageContext.APPLICATION_SCOPE) != null) {
      return PageContext.APPLICATION_SCOPE;
    }
    else {
      return 0;
    }
  }

  public Enumeration<String> getAttributeNames() {
    return Collections.enumeration(new LinkedHashSet<>(this.attributes.keySet()));
  }

  @Override
  public Enumeration<String> getAttributeNamesInScope(int scope) {
    return switch (scope) {
      case PageContext.PAGE_SCOPE -> getAttributeNames();
      case PageContext.REQUEST_SCOPE -> this.request.getAttributeNames();
      case PageContext.SESSION_SCOPE -> {
        HttpSession session = this.request.getSession(false);
        yield (session != null ? session.getAttributeNames() : Collections.<String>emptyEnumeration());
      }
      case PageContext.APPLICATION_SCOPE -> this.servletContext.getAttributeNames();
      default -> throw new IllegalArgumentException("Invalid scope: " + scope);
    };
  }

  @Override
  public JspWriter getOut() {
    if (this.out == null) {
      this.out = new MockJspWriter(this.response);
    }
    return this.out;
  }

  @Override
  @Deprecated
  @Nullable
  public jakarta.servlet.jsp.el.ExpressionEvaluator getExpressionEvaluator() {
    return null;
  }

  @Override
  @Nullable
  public ELContext getELContext() {
    return null;
  }

  @Override
  @Deprecated
  @Nullable
  public jakarta.servlet.jsp.el.VariableResolver getVariableResolver() {
    return null;
  }

  @Override
  public HttpSession getSession() {
    return this.request.getSession();
  }

  @Override
  public Object getPage() {
    return this;
  }

  @Override
  public ServletRequest getRequest() {
    return this.request;
  }

  @Override
  public ServletResponse getResponse() {
    return this.response;
  }

  @Override
  @Nullable
  public Exception getException() {
    return null;
  }

  @Override
  public ServletConfig getServletConfig() {
    return this.servletConfig;
  }

  @Override
  public ServletContext getServletContext() {
    return this.servletContext;
  }

  @Override
  public void forward(String path) throws ServletException, IOException {
    this.request.getRequestDispatcher(path).forward(this.request, this.response);
  }

  @Override
  public void include(String path) throws ServletException, IOException {
    this.request.getRequestDispatcher(path).include(this.request, this.response);
  }

  @Override
  public void include(String path, boolean flush) throws ServletException, IOException {
    this.request.getRequestDispatcher(path).include(this.request, this.response);
    if (flush) {
      this.response.flushBuffer();
    }
  }

  public byte[] getContentAsByteArray() {
    Assert.state(this.response instanceof MockHttpServletResponse, "MockHttpServletResponse required");
    return ((MockHttpServletResponse) this.response).getContentAsByteArray();
  }

  public String getContentAsString() throws UnsupportedEncodingException {
    Assert.state(this.response instanceof MockHttpServletResponse, "MockHttpServletResponse required");
    return ((MockHttpServletResponse) this.response).getContentAsString();
  }

  @Override
  public void handlePageException(Exception ex) throws ServletException, IOException {
    throw new ServletException("Page exception", ex);
  }

  @Override
  public void handlePageException(Throwable ex) throws ServletException, IOException {
    throw new ServletException("Page exception", ex);
  }

}
