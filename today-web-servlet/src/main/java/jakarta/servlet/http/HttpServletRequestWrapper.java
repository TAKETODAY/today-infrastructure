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

package jakarta.servlet.http;

import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequestWrapper;

/**
 * Provides a convenient implementation of the HttpServletRequest interface that can be subclassed by developers wishing
 * to adapt the request to a Servlet.
 *
 * <p>
 * This class implements the Wrapper or Decorator pattern. Methods default to calling through to the wrapped request
 * object.
 *
 * @see HttpServletRequest
 * @since Servlet 2.3
 */
public class HttpServletRequestWrapper extends ServletRequestWrapper implements HttpServletRequest {

  /**
   * Constructs a request object wrapping the given request.
   *
   * @param request the {@link HttpServletRequest} to be wrapped.
   * @throws IllegalArgumentException if the request is null
   */
  public HttpServletRequestWrapper(HttpServletRequest request) {
    super(request);
  }

  private HttpServletRequest _getHttpServletRequest() {
    return (HttpServletRequest) super.getRequest();
  }

  /**
   * The default behavior of this method is to return getAuthType() on the wrapped request object.
   */
  @Override
  public String getAuthType() {
    return this._getHttpServletRequest().getAuthType();
  }

  /**
   * The default behavior of this method is to return getCookies() on the wrapped request object.
   */
  @Override
  public Cookie[] getCookies() {
    return this._getHttpServletRequest().getCookies();
  }

  /**
   * The default behavior of this method is to return getDateHeader(String name) on the wrapped request object.
   */
  @Override
  public long getDateHeader(String name) {
    return this._getHttpServletRequest().getDateHeader(name);
  }

  /**
   * The default behavior of this method is to return getHeader(String name) on the wrapped request object.
   */
  @Override
  public String getHeader(String name) {
    return this._getHttpServletRequest().getHeader(name);
  }

  /**
   * The default behavior of this method is to return getHeaders(String name) on the wrapped request object.
   */
  @Override
  public Enumeration<String> getHeaders(String name) {
    return this._getHttpServletRequest().getHeaders(name);
  }

  /**
   * The default behavior of this method is to return getHeaderNames() on the wrapped request object.
   */
  @Override
  public Enumeration<String> getHeaderNames() {
    return this._getHttpServletRequest().getHeaderNames();
  }

  /**
   * The default behavior of this method is to return getIntHeader(String name) on the wrapped request object.
   */
  @Override
  public int getIntHeader(String name) {
    return this._getHttpServletRequest().getIntHeader(name);
  }

  /**
   * The default behavior of this method is to return getServletMapping() on the wrapped request object.
   *
   * @since Servlet 4.0
   */
  @Override
  public HttpServletMapping getHttpServletMapping() {
    return this._getHttpServletRequest().getHttpServletMapping();
  }

  /**
   * The default behavior of this method is to return getMethod() on the wrapped request object.
   */
  @Override
  public String getMethod() {
    return this._getHttpServletRequest().getMethod();
  }

  /**
   * The default behavior of this method is to return getPathInfo() on the wrapped request object.
   */
  @Override
  public String getPathInfo() {
    return this._getHttpServletRequest().getPathInfo();
  }

  /**
   * The default behavior of this method is to return getPathTranslated() on the wrapped request object.
   */
  @Override
  public String getPathTranslated() {
    return this._getHttpServletRequest().getPathTranslated();
  }

  /**
   * The default behavior of this method is to return getContextPath() on the wrapped request object.
   */
  @Override
  public String getContextPath() {
    return this._getHttpServletRequest().getContextPath();
  }

  /**
   * The default behavior of this method is to return getQueryString() on the wrapped request object.
   */
  @Override
  public String getQueryString() {
    return this._getHttpServletRequest().getQueryString();
  }

  /**
   * The default behavior of this method is to return getRemoteUser() on the wrapped request object.
   */
  @Override
  public String getRemoteUser() {
    return this._getHttpServletRequest().getRemoteUser();
  }

  /**
   * The default behavior of this method is to return isUserInRole(String role) on the wrapped request object.
   */
  @Override
  public boolean isUserInRole(String role) {
    return this._getHttpServletRequest().isUserInRole(role);
  }

  /**
   * The default behavior of this method is to return getUserPrincipal() on the wrapped request object.
   */
  @Override
  public java.security.Principal getUserPrincipal() {
    return this._getHttpServletRequest().getUserPrincipal();
  }

  /**
   * The default behavior of this method is to return getRequestedSessionId() on the wrapped request object.
   */
  @Override
  public String getRequestedSessionId() {
    return this._getHttpServletRequest().getRequestedSessionId();
  }

  /**
   * The default behavior of this method is to return getRequestURI() on the wrapped request object.
   */
  @Override
  public String getRequestURI() {
    return this._getHttpServletRequest().getRequestURI();
  }

  /**
   * The default behavior of this method is to return getRequestURL() on the wrapped request object.
   */
  @Override
  public StringBuffer getRequestURL() {
    return this._getHttpServletRequest().getRequestURL();
  }

  /**
   * The default behavior of this method is to return getServletPath() on the wrapped request object.
   */
  @Override
  public String getServletPath() {
    return this._getHttpServletRequest().getServletPath();
  }

  /**
   * The default behavior of this method is to return getSession(boolean create) on the wrapped request object.
   */
  @Override
  public HttpSession getSession(boolean create) {
    return this._getHttpServletRequest().getSession(create);
  }

  /**
   * The default behavior of this method is to return getSession() on the wrapped request object.
   */
  @Override
  public HttpSession getSession() {
    return this._getHttpServletRequest().getSession();
  }

  /**
   * The default behavior of this method is to return changeSessionId() on the wrapped request object.
   *
   * @since Servlet 3.1
   */
  @Override
  public String changeSessionId() {
    return this._getHttpServletRequest().changeSessionId();
  }

  /**
   * The default behavior of this method is to return isRequestedSessionIdValid() on the wrapped request object.
   */
  @Override
  public boolean isRequestedSessionIdValid() {
    return this._getHttpServletRequest().isRequestedSessionIdValid();
  }

  /**
   * The default behavior of this method is to return isRequestedSessionIdFromCookie() on the wrapped request object.
   */
  @Override
  public boolean isRequestedSessionIdFromCookie() {
    return this._getHttpServletRequest().isRequestedSessionIdFromCookie();
  }

  /**
   * The default behavior of this method is to return isRequestedSessionIdFromURL() on the wrapped request object.
   */
  @Override
  public boolean isRequestedSessionIdFromURL() {
    return this._getHttpServletRequest().isRequestedSessionIdFromURL();
  }

  /**
   * The default behavior of this method is to call authenticate on the wrapped request object.
   *
   * @since Servlet 3.0
   */
  @Override
  public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
    return this._getHttpServletRequest().authenticate(response);
  }

  /**
   * The default behavior of this method is to call login on the wrapped request object.
   *
   * @since Servlet 3.0
   */
  @Override
  public void login(String username, String password) throws ServletException {
    this._getHttpServletRequest().login(username, password);
  }

  /**
   * The default behavior of this method is to call login on the wrapped request object.
   *
   * @since Servlet 3.0
   */
  @Override
  public void logout() throws ServletException {
    this._getHttpServletRequest().logout();
  }

  /**
   * The default behavior of this method is to call getParts on the wrapped request object.
   *
   * <p>
   * Any changes to the returned <code>Collection</code> must not affect this <code>HttpServletRequestWrapper</code>.
   *
   * @since Servlet 3.0
   */
  @Override
  public Collection<Part> getParts() throws IOException, ServletException {
    return this._getHttpServletRequest().getParts();
  }

  /**
   * The default behavior of this method is to call getPart on the wrapped request object.
   *
   * @since Servlet 3.0
   */
  @Override
  public Part getPart(String name) throws IOException, ServletException {
    return this._getHttpServletRequest().getPart(name);
  }

  /**
   * Create an instance of <code>HttpUpgradeHandler</code> for a given class and uses it for the http protocol upgrade
   * processing.
   *
   * @since Servlet 3.1
   */
  @Override
  public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
    return this._getHttpServletRequest().upgrade(handlerClass);
  }

  /**
   * The default behavior of this method is to call newPushBuilder on the wrapped request object.
   *
   * @since Servlet 4.0
   */
  @Override
  public PushBuilder newPushBuilder() {
    return this._getHttpServletRequest().newPushBuilder();
  }

  /**
   * The default behavior of this method is to call getTrailerFields on the wrapped request object.
   *
   * @since Servlet 4.0
   */
  @Override
  public Map<String, String> getTrailerFields() {
    return this._getHttpServletRequest().getTrailerFields();
  }

  /**
   * The default behavior of this method is to call isTrailerFieldsReady on the wrapped request object.
   *
   * @since Servlet 4.0
   */
  @Override
  public boolean isTrailerFieldsReady() {
    return this._getHttpServletRequest().isTrailerFieldsReady();
  }
}
