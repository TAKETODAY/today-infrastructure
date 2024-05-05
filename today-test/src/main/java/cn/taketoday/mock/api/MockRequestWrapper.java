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

package cn.taketoday.mock.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * Provides a convenient implementation of the ServletRequest interface that can be subclassed by developers wishing to
 * adapt the request to a Servlet. This class implements the Wrapper or Decorator pattern. Methods default to calling
 * through to the wrapped request object.
 *
 * @see MockRequest
 * @since Servlet 2.3
 */
public class MockRequestWrapper implements MockRequest {

  private MockRequest request;

  /**
   * Creates a ServletRequest adaptor wrapping the given request object.
   *
   * @param request the {@link MockRequest} to be wrapped
   * @throws IllegalArgumentException if the request is null
   */
  public MockRequestWrapper(MockRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Request cannot be null");
    }
    this.request = request;
  }

  /**
   * Return the wrapped request object.
   *
   * @return the wrapped {@link MockRequest}
   */
  public MockRequest getRequest() {
    return this.request;
  }

  /**
   * Sets the request object being wrapped.
   *
   * @param request the {@link MockRequest} to be installed
   * @throws IllegalArgumentException if the request is null.
   */
  public void setRequest(MockRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Request cannot be null");
    }
    this.request = request;
  }

  /**
   * The default behavior of this method is to call getAttribute(String name) on the wrapped request object.
   */
  @Override
  public Object getAttribute(String name) {
    return this.request.getAttribute(name);
  }

  /**
   * The default behavior of this method is to return getAttributeNames() on the wrapped request object.
   */
  @Override
  public Enumeration<String> getAttributeNames() {
    return this.request.getAttributeNames();
  }

  /**
   * The default behavior of this method is to return getCharacterEncoding() on the wrapped request object.
   */
  @Override
  public String getCharacterEncoding() {
    return this.request.getCharacterEncoding();
  }

  /**
   * The default behavior of this method is to set the character encoding on the wrapped request object.
   */
  @Override
  public void setCharacterEncoding(String enc) throws UnsupportedEncodingException {
    this.request.setCharacterEncoding(enc);
  }

  /**
   * The default behavior of this method is to return getContentLength() on the wrapped request object.
   */
  @Override
  public int getContentLength() {
    return this.request.getContentLength();
  }

  /**
   * The default behavior of this method is to return getContentLengthLong() on the wrapped request object.
   *
   * @since Servlet 3.1
   */
  @Override
  public long getContentLengthLong() {
    return this.request.getContentLengthLong();
  }

  /**
   * The default behavior of this method is to return getContentType() on the wrapped request object.
   */
  @Override
  public String getContentType() {
    return this.request.getContentType();
  }

  /**
   * The default behavior of this method is to return getInputStream() on the wrapped request object.
   */
  @Override
  public MockInputStream getInputStream() throws IOException {
    return this.request.getInputStream();
  }

  /**
   * The default behavior of this method is to return getParameter(String name) on the wrapped request object.
   */
  @Override
  public String getParameter(String name) {
    return this.request.getParameter(name);
  }

  /**
   * The default behavior of this method is to return getParameterMap() on the wrapped request object.
   */
  @Override
  public Map<String, String[]> getParameterMap() {
    return this.request.getParameterMap();
  }

  /**
   * The default behavior of this method is to return getParameterNames() on the wrapped request object.
   */
  @Override
  public Enumeration<String> getParameterNames() {
    return this.request.getParameterNames();
  }

  /**
   * The default behavior of this method is to return getParameterValues(String name) on the wrapped request object.
   */
  @Override
  public String[] getParameterValues(String name) {
    return this.request.getParameterValues(name);
  }

  /**
   * The default behavior of this method is to return getProtocol() on the wrapped request object.
   */
  @Override
  public String getProtocol() {
    return this.request.getProtocol();
  }

  /**
   * The default behavior of this method is to return getScheme() on the wrapped request object.
   */
  @Override
  public String getScheme() {
    return this.request.getScheme();
  }

  /**
   * The default behavior of this method is to return getServerName() on the wrapped request object.
   */
  @Override
  public String getServerName() {
    return this.request.getServerName();
  }

  /**
   * The default behavior of this method is to return getServerPort() on the wrapped request object.
   */
  @Override
  public int getServerPort() {
    return this.request.getServerPort();
  }

  /**
   * The default behavior of this method is to return getReader() on the wrapped request object.
   */
  @Override
  public BufferedReader getReader() throws IOException {
    return this.request.getReader();
  }

  /**
   * The default behavior of this method is to return getRemoteAddr() on the wrapped request object.
   */
  @Override
  public String getRemoteAddr() {
    return this.request.getRemoteAddr();
  }

  /**
   * The default behavior of this method is to return getRemoteHost() on the wrapped request object.
   */
  @Override
  public String getRemoteHost() {
    return this.request.getRemoteHost();
  }

  /**
   * The default behavior of this method is to return setAttribute(String name, Object o) on the wrapped request object.
   */
  @Override
  public void setAttribute(String name, Object o) {
    this.request.setAttribute(name, o);
  }

  /**
   * The default behavior of this method is to call removeAttribute(String name) on the wrapped request object.
   */
  @Override
  public void removeAttribute(String name) {
    this.request.removeAttribute(name);
  }

  /**
   * The default behavior of this method is to return getLocale() on the wrapped request object.
   */
  @Override
  public Locale getLocale() {
    return this.request.getLocale();
  }

  /**
   * The default behavior of this method is to return getLocales() on the wrapped request object.
   */
  @Override
  public Enumeration<Locale> getLocales() {
    return this.request.getLocales();
  }

  /**
   * The default behavior of this method is to return isSecure() on the wrapped request object.
   */
  @Override
  public boolean isSecure() {
    return this.request.isSecure();
  }

  /**
   * The default behavior of this method is to return getRequestDispatcher(String path) on the wrapped request object.
   */
  @Override
  public RequestDispatcher getRequestDispatcher(String path) {
    return this.request.getRequestDispatcher(path);
  }

  /**
   * The default behavior of this method is to return getRemotePort() on the wrapped request object.
   *
   * @since Servlet 2.4
   */
  @Override
  public int getRemotePort() {
    return this.request.getRemotePort();
  }

  /**
   * The default behavior of this method is to return getLocalName() on the wrapped request object.
   *
   * @since Servlet 2.4
   */
  @Override
  public String getLocalName() {
    return this.request.getLocalName();
  }

  /**
   * The default behavior of this method is to return getLocalAddr() on the wrapped request object.
   *
   * @since Servlet 2.4
   */
  @Override
  public String getLocalAddr() {
    return this.request.getLocalAddr();
  }

  /**
   * The default behavior of this method is to return getLocalPort() on the wrapped request object.
   *
   * @since Servlet 2.4
   */
  @Override
  public int getLocalPort() {
    return this.request.getLocalPort();
  }

  /**
   * Gets the servlet context to which the wrapped servlet request was last dispatched.
   *
   * @return the servlet context to which the wrapped servlet request was last dispatched
   * @since Servlet 3.0
   */
  @Override
  public MockContext getMockContext() {
    return request.getMockContext();
  }

  /**
   * The default behavior of this method is to invoke {@link MockRequest#startAsync} on the wrapped request object.
   *
   * @return the (re)initialized AsyncContext
   * @throws IllegalStateException if the request is within the scope of a filter or servlet that does not support
   * asynchronous operations (that is, {@link #isAsyncSupported} returns false), or if this method is called again without
   * any asynchronous dispatch (resulting from one of the {@link AsyncContext#dispatch} methods), is called outside the
   * scope of any such dispatch, or is called again within the scope of the same dispatch, or if the response has already
   * been closed
   * @see MockRequest#startAsync
   * @since Servlet 3.0
   */
  @Override
  public AsyncContext startAsync() throws IllegalStateException {
    return request.startAsync();
  }

  /**
   * The default behavior of this method is to invoke {@link MockRequest#startAsync(MockRequest, MockResponse)}
   * on the wrapped request object.
   *
   * @param mockRequest the ServletRequest used to initialize the AsyncContext
   * @param mockResponse the ServletResponse used to initialize the AsyncContext
   * @return the (re)initialized AsyncContext
   * @throws IllegalStateException if the request is within the scope of a filter or servlet that does not support
   * asynchronous operations (that is, {@link #isAsyncSupported} returns false), or if this method is called again without
   * any asynchronous dispatch (resulting from one of the {@link AsyncContext#dispatch} methods), is called outside the
   * scope of any such dispatch, or is called again within the scope of the same dispatch, or if the response has already
   * been closed
   * @see MockRequest#startAsync(MockRequest, MockResponse)
   * @since Servlet 3.0
   */
  @Override
  public AsyncContext startAsync(MockRequest mockRequest, MockResponse mockResponse)
          throws IllegalStateException {
    return request.startAsync(mockRequest, mockResponse);
  }

  /**
   * Checks if the wrapped request has been put into asynchronous mode.
   *
   * @return true if this request has been put into asynchronous mode, false otherwise
   * @see MockRequest#isAsyncStarted
   * @since Servlet 3.0
   */
  @Override
  public boolean isAsyncStarted() {
    return request.isAsyncStarted();
  }

  /**
   * Checks if the wrapped request supports asynchronous operation.
   *
   * @return true if this request supports asynchronous operation, false otherwise
   * @see MockRequest#isAsyncSupported
   * @since Servlet 3.0
   */
  @Override
  public boolean isAsyncSupported() {
    return request.isAsyncSupported();
  }

  /**
   * Gets the AsyncContext that was created or reinitialized by the most recent invocation of {@link #startAsync} or
   * {@link #startAsync(MockRequest, MockResponse)} on the wrapped request.
   *
   * @return the AsyncContext that was created or reinitialized by the most recent invocation of {@link #startAsync} or
   * {@link #startAsync(MockRequest, MockResponse)} on the wrapped request
   * @throws IllegalStateException if this request has not been put into asynchronous mode, i.e., if neither
   * {@link #startAsync} nor {@link #startAsync(MockRequest, MockResponse)} has been called
   * @see MockRequest#getAsyncContext
   * @since Servlet 3.0
   */
  @Override
  public AsyncContext getAsyncContext() {
    return request.getAsyncContext();
  }

  /**
   * Checks (recursively) if this ServletRequestWrapper wraps the given {@link MockRequest} instance.
   *
   * @param wrapped the ServletRequest instance to search for
   * @return true if this ServletRequestWrapper wraps the given ServletRequest instance, false otherwise
   * @since Servlet 3.0
   */
  public boolean isWrapperFor(MockRequest wrapped) {
    if (request == wrapped) {
      return true;
    }
    else if (request instanceof MockRequestWrapper) {
      return ((MockRequestWrapper) request).isWrapperFor(wrapped);
    }
    else {
      return false;
    }
  }

  /**
   * Checks (recursively) if this ServletRequestWrapper wraps a {@link MockRequest} of the given class type.
   *
   * @param wrappedType the ServletRequest class type to search for
   * @return true if this ServletRequestWrapper wraps a ServletRequest of the given class type, false otherwise
   * @throws IllegalArgumentException if the given class does not implement {@link MockRequest}
   * @since Servlet 3.0
   */
  public boolean isWrapperFor(Class<?> wrappedType) {
    if (!MockRequest.class.isAssignableFrom(wrappedType)) {
      throw new IllegalArgumentException("Given class " + wrappedType.getName() + " not a subinterface of "
              + MockRequest.class.getName());
    }
    if (wrappedType.isAssignableFrom(request.getClass())) {
      return true;
    }
    else if (request instanceof MockRequestWrapper) {
      return ((MockRequestWrapper) request).isWrapperFor(wrappedType);
    }
    else {
      return false;
    }
  }

  /**
   * Gets the dispatcher type of the wrapped request.
   *
   * @return the dispatcher type of the wrapped request
   * @see MockRequest#getDispatcherType
   * @since Servlet 3.0
   */
  @Override
  public DispatcherType getDispatcherType() {
    return request.getDispatcherType();
  }

  /**
   * Gets the request ID for the wrapped request.
   *
   * @return the request ID for the wrapped request
   * @since Servlet 6.0
   */
  @Override
  public String getRequestId() {
    return request.getRequestId();
  }

  /**
   * Gets the protocol defined request ID, if any, for the wrapped request.
   *
   * @return the protocol defined request ID, if any, for the wrapped request
   * @since Servlet 6.0
   */
  @Override
  public String getProtocolRequestId() {
    return request.getProtocolRequestId();
  }

  /**
   * Gets the connection information for the wrapped request.
   *
   * @return the connection information for the wrapped request
   * @since Servlet 6.0
   */
  @Override
  public MockConnection getServletConnection() {
    return request.getServletConnection();
  }
}
