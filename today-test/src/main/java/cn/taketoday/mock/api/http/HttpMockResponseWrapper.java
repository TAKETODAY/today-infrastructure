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
import java.util.Map;
import java.util.function.Supplier;

import cn.taketoday.mock.api.MockResponseWrapper;

/**
 * Provides a convenient implementation of the HttpServletResponse interface that can be subclassed by developers
 * wishing to adapt the response from a Servlet. This class implements the Wrapper or Decorator pattern. Methods default
 * to calling through to the wrapped response object.
 *
 * @author Various
 * @see HttpMockResponse
 */
public class HttpMockResponseWrapper extends MockResponseWrapper implements HttpMockResponse {

  /**
   * Constructs a response adaptor wrapping the given response.
   *
   * @param response the {@link HttpMockResponse} to be wrapped.
   * @throws IllegalArgumentException if the response is null
   */
  public HttpMockResponseWrapper(HttpMockResponse response) {
    super(response);
  }

  private HttpMockResponse _getHttpResponse() {
    return (HttpMockResponse) super.getResponse();
  }

  /**
   * The default behavior of this method is to call addCookie(Cookie cookie) on the wrapped response object.
   */
  @Override
  public void addCookie(Cookie cookie) {
    this._getHttpResponse().addCookie(cookie);
  }

  /**
   * The default behavior of this method is to call containsHeader(String name) on the wrapped response object.
   */
  @Override
  public boolean containsHeader(String name) {
    return this._getHttpResponse().containsHeader(name);
  }

  /**
   * The default behavior of this method is to call encodeURL(String url) on the wrapped response object.
   */
  @Override
  public String encodeURL(String url) {
    return this._getHttpResponse().encodeURL(url);
  }

  /**
   * The default behavior of this method is to return encodeRedirectURL(String url) on the wrapped response object.
   */
  @Override
  public String encodeRedirectURL(String url) {
    return this._getHttpResponse().encodeRedirectURL(url);
  }

  /**
   * The default behavior of this method is to call sendError(int sc, String msg) on the wrapped response object.
   */
  @Override
  public void sendError(int sc, String msg) throws IOException {
    this._getHttpResponse().sendError(sc, msg);
  }

  /**
   * The default behavior of this method is to call sendError(int sc) on the wrapped response object.
   */
  @Override
  public void sendError(int sc) throws IOException {
    this._getHttpResponse().sendError(sc);
  }

  /**
   * The default behavior of this method is to return sendRedirect(String location) on the wrapped response object.
   */
  @Override
  public void sendRedirect(String location) throws IOException {
    this._getHttpResponse().sendRedirect(location);
  }

  /**
   * The default behavior of this method is to call setDateHeader(String name, long date) on the wrapped response object.
   */
  @Override
  public void setDateHeader(String name, long date) {
    this._getHttpResponse().setDateHeader(name, date);
  }

  /**
   * The default behavior of this method is to call addDateHeader(String name, long date) on the wrapped response object.
   */
  @Override
  public void addDateHeader(String name, long date) {
    this._getHttpResponse().addDateHeader(name, date);
  }

  /**
   * The default behavior of this method is to return setHeader(String name, String value) on the wrapped response object.
   */
  @Override
  public void setHeader(String name, String value) {
    this._getHttpResponse().setHeader(name, value);
  }

  /**
   * The default behavior of this method is to return addHeader(String name, String value) on the wrapped response object.
   */
  @Override
  public void addHeader(String name, String value) {
    this._getHttpResponse().addHeader(name, value);
  }

  /**
   * The default behavior of this method is to call setIntHeader(String name, int value) on the wrapped response object.
   */
  @Override
  public void setIntHeader(String name, int value) {
    this._getHttpResponse().setIntHeader(name, value);
  }

  /**
   * The default behavior of this method is to call addIntHeader(String name, int value) on the wrapped response object.
   */
  @Override
  public void addIntHeader(String name, int value) {
    this._getHttpResponse().addIntHeader(name, value);
  }

  /**
   * The default behavior of this method is to call setStatus(int sc) on the wrapped response object.
   */
  @Override
  public void setStatus(int sc) {
    this._getHttpResponse().setStatus(sc);
  }

  /**
   * The default behaviour of this method is to call {@link HttpMockResponse#getStatus} on the wrapped response object.
   *
   * @return the current status code of the wrapped response
   */
  @Override
  public int getStatus() {
    return _getHttpResponse().getStatus();
  }

  /**
   * The default behaviour of this method is to call {@link HttpMockResponse#getHeader} on the wrapped response object.
   *
   * @param name the name of the response header whose value to return
   * @return the value of the response header with the given name, or <tt>null</tt> if no header with the given name has
   * been set on the wrapped response
   */
  @Override
  public String getHeader(String name) {
    return _getHttpResponse().getHeader(name);
  }

  /**
   * The default behaviour of this method is to call {@link HttpMockResponse#getHeaders} on the wrapped response
   * object.
   *
   * <p>
   * Any changes to the returned <code>Collection</code> must not affect this <code>HttpServletResponseWrapper</code>.
   *
   * @param name the name of the response header whose values to return
   * @return a (possibly empty) <code>Collection</code> of the values of the response header with the given name
   */
  @Override
  public Collection<String> getHeaders(String name) {
    return _getHttpResponse().getHeaders(name);
  }

  /**
   * The default behaviour of this method is to call {@link HttpMockResponse#getHeaderNames} on the wrapped response
   * object.
   *
   * <p>
   * Any changes to the returned <code>Collection</code> must not affect this <code>HttpServletResponseWrapper</code>.
   *
   * @return a (possibly empty) <code>Collection</code> of the names of the response headers
   */
  @Override
  public Collection<String> getHeaderNames() {
    return _getHttpResponse().getHeaderNames();
  }

  /**
   * The default behaviour of this method is to call {@link HttpMockResponse#setTrailerFields} on the wrapped response
   * object.
   *
   * @param supplier of trailer headers
   */
  @Override
  public void setTrailerFields(Supplier<Map<String, String>> supplier) {
    _getHttpResponse().setTrailerFields(supplier);
  }

  /**
   * The default behaviour of this method is to call {@link HttpMockResponse#getTrailerFields} on the wrapped response
   * object.
   *
   * @return supplier of trailer headers
   */
  @Override
  public Supplier<Map<String, String>> getTrailerFields() {
    return _getHttpResponse().getTrailerFields();
  }
}
