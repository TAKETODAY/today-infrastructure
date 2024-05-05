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

package cn.taketoday.mock.api.http;

import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;

import cn.taketoday.mock.api.MockException;
import cn.taketoday.mock.api.MockRequestWrapper;

/**
 * Provides a convenient implementation of the HttpServletRequest interface that can be subclassed by developers wishing
 * to adapt the request to a Servlet.
 *
 * <p>
 * This class implements the Wrapper or Decorator pattern. Methods default to calling through to the wrapped request
 * object.
 *
 * @see HttpMockRequest
 */
public class HttpMockRequestWrapper extends MockRequestWrapper implements HttpMockRequest {

  /**
   * Constructs a request object wrapping the given request.
   *
   * @param request the {@link HttpMockRequest} to be wrapped.
   * @throws IllegalArgumentException if the request is null
   */
  public HttpMockRequestWrapper(HttpMockRequest request) {
    super(request);
  }

  private HttpMockRequest _getHttpMockRequest() {
    return (HttpMockRequest) super.getRequest();
  }

  /**
   * The default behavior of this method is to return getAuthType() on the wrapped request object.
   */
  @Override
  public String getAuthType() {
    return this._getHttpMockRequest().getAuthType();
  }

  /**
   * The default behavior of this method is to return getCookies() on the wrapped request object.
   */
  @Override
  public Cookie[] getCookies() {
    return this._getHttpMockRequest().getCookies();
  }

  /**
   * The default behavior of this method is to return getDateHeader(String name) on the wrapped request object.
   */
  @Override
  public long getDateHeader(String name) {
    return this._getHttpMockRequest().getDateHeader(name);
  }

  /**
   * The default behavior of this method is to return getHeader(String name) on the wrapped request object.
   */
  @Override
  public String getHeader(String name) {
    return this._getHttpMockRequest().getHeader(name);
  }

  /**
   * The default behavior of this method is to return getHeaders(String name) on the wrapped request object.
   */
  @Override
  public Enumeration<String> getHeaders(String name) {
    return this._getHttpMockRequest().getHeaders(name);
  }

  /**
   * The default behavior of this method is to return getHeaderNames() on the wrapped request object.
   */
  @Override
  public Enumeration<String> getHeaderNames() {
    return this._getHttpMockRequest().getHeaderNames();
  }

  /**
   * The default behavior of this method is to return getIntHeader(String name) on the wrapped request object.
   */
  @Override
  public int getIntHeader(String name) {
    return this._getHttpMockRequest().getIntHeader(name);
  }

  /**
   * The default behavior of this method is to return getServletMapping() on the wrapped request object.
   */
  @Override
  public HttpMockMapping getHttpMapping() {
    return this._getHttpMockRequest().getHttpMapping();
  }

  /**
   * The default behavior of this method is to return getMethod() on the wrapped request object.
   */
  @Override
  public String getMethod() {
    return this._getHttpMockRequest().getMethod();
  }

  /**
   * The default behavior of this method is to return getPathInfo() on the wrapped request object.
   */
  @Override
  public String getPathInfo() {
    return this._getHttpMockRequest().getPathInfo();
  }

  /**
   * The default behavior of this method is to return getPathTranslated() on the wrapped request object.
   */
  @Override
  public String getPathTranslated() {
    return this._getHttpMockRequest().getPathTranslated();
  }

  /**
   * The default behavior of this method is to return getQueryString() on the wrapped request object.
   */
  @Override
  public String getQueryString() {
    return this._getHttpMockRequest().getQueryString();
  }

  /**
   * The default behavior of this method is to return getRemoteUser() on the wrapped request object.
   */
  @Override
  public String getRemoteUser() {
    return this._getHttpMockRequest().getRemoteUser();
  }

  /**
   * The default behavior of this method is to return isUserInRole(String role) on the wrapped request object.
   */
  @Override
  public boolean isUserInRole(String role) {
    return this._getHttpMockRequest().isUserInRole(role);
  }

  /**
   * The default behavior of this method is to return getUserPrincipal() on the wrapped request object.
   */
  @Override
  public java.security.Principal getUserPrincipal() {
    return this._getHttpMockRequest().getUserPrincipal();
  }

  /**
   * The default behavior of this method is to return getRequestedSessionId() on the wrapped request object.
   */
  @Override
  public String getRequestedSessionId() {
    return this._getHttpMockRequest().getRequestedSessionId();
  }

  /**
   * The default behavior of this method is to return getRequestURI() on the wrapped request object.
   */
  @Override
  public String getRequestURI() {
    return this._getHttpMockRequest().getRequestURI();
  }

  /**
   * The default behavior of this method is to return getRequestURL() on the wrapped request object.
   */
  @Override
  public StringBuffer getRequestURL() {
    return this._getHttpMockRequest().getRequestURL();
  }

  /**
   * The default behavior of this method is to return getSession(boolean create) on the wrapped request object.
   */
  @Override
  public HttpSession getSession(boolean create) {
    return this._getHttpMockRequest().getSession(create);
  }

  /**
   * The default behavior of this method is to return getSession() on the wrapped request object.
   */
  @Override
  public HttpSession getSession() {
    return this._getHttpMockRequest().getSession();
  }

  /**
   * The default behavior of this method is to return changeSessionId() on the wrapped request object.
   */
  @Override
  public String changeSessionId() {
    return this._getHttpMockRequest().changeSessionId();
  }

  /**
   * The default behavior of this method is to return isRequestedSessionIdValid() on the wrapped request object.
   */
  @Override
  public boolean isRequestedSessionIdValid() {
    return this._getHttpMockRequest().isRequestedSessionIdValid();
  }

  /**
   * The default behavior of this method is to return isRequestedSessionIdFromCookie() on the wrapped request object.
   */
  @Override
  public boolean isRequestedSessionIdFromCookie() {
    return this._getHttpMockRequest().isRequestedSessionIdFromCookie();
  }

  /**
   * The default behavior of this method is to return isRequestedSessionIdFromURL() on the wrapped request object.
   */
  @Override
  public boolean isRequestedSessionIdFromURL() {
    return this._getHttpMockRequest().isRequestedSessionIdFromURL();
  }

  /**
   * The default behavior of this method is to call authenticate on the wrapped request object.
   */
  @Override
  public boolean authenticate(HttpMockResponse response) throws IOException, MockException {
    return this._getHttpMockRequest().authenticate(response);
  }

  /**
   * The default behavior of this method is to call login on the wrapped request object.
   */
  @Override
  public void login(String username, String password) throws MockException {
    this._getHttpMockRequest().login(username, password);
  }

  /**
   * The default behavior of this method is to call login on the wrapped request object.
   */
  @Override
  public void logout() throws MockException {
    this._getHttpMockRequest().logout();
  }

  /**
   * The default behavior of this method is to call getParts on the wrapped request object.
   *
   * <p>
   * Any changes to the returned <code>Collection</code> must not affect this <code>HttpServletRequestWrapper</code>.
   */
  @Override
  public Collection<Part> getParts() throws IOException, MockException {
    return this._getHttpMockRequest().getParts();
  }

  /**
   * The default behavior of this method is to call getPart on the wrapped request object.
   */
  @Override
  public Part getPart(String name) throws IOException, MockException {
    return this._getHttpMockRequest().getPart(name);
  }

  /**
   * Create an instance of <code>HttpUpgradeHandler</code> for a given class and uses it for the http protocol upgrade
   * processing.
   */
  @Override
  public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, MockException {
    return this._getHttpMockRequest().upgrade(handlerClass);
  }

  /**
   * The default behavior of this method is to call newPushBuilder on the wrapped request object.
   */
  @Override
  public PushBuilder newPushBuilder() {
    return this._getHttpMockRequest().newPushBuilder();
  }

  /**
   * The default behavior of this method is to call getTrailerFields on the wrapped request object.
   */
  @Override
  public Map<String, String> getTrailerFields() {
    return this._getHttpMockRequest().getTrailerFields();
  }

  /**
   * The default behavior of this method is to call isTrailerFieldsReady on the wrapped request object.
   */
  @Override
  public boolean isTrailerFieldsReady() {
    return this._getHttpMockRequest().isTrailerFieldsReady();
  }
}
