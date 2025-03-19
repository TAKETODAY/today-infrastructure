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

package infra.mock.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * Provides a convenient implementation of the ServletResponse interface that can be subclassed by developers wishing to
 * adapt the response from a Servlet. This class implements the Wrapper or Decorator pattern. Methods default to calling
 * through to the wrapped response object.
 *
 * @author Various
 * @see MockResponse
 */
public class MockResponseWrapper implements MockResponse {
  private MockResponse response;

  /**
   * Creates a ServletResponse adaptor wrapping the given response object.
   *
   * @param response the {@link MockResponse} to be wrapped
   * @throws IllegalArgumentException if the response is null.
   */
  public MockResponseWrapper(MockResponse response) {
    if (response == null) {
      throw new IllegalArgumentException("Response cannot be null");
    }
    this.response = response;
  }

  /**
   * Return the wrapped ServletResponse object.
   *
   * @return the wrapped {@link MockResponse}
   */
  public MockResponse getResponse() {
    return this.response;
  }

  /**
   * Sets the response being wrapped.
   *
   * @param response the {@link MockResponse} to be installed
   * @throws IllegalArgumentException if the response is null.
   */
  public void setResponse(MockResponse response) {
    if (response == null) {
      throw new IllegalArgumentException("Response cannot be null");
    }
    this.response = response;
  }

  /**
   * The default behavior of this method is to call setCharacterEncoding(String charset) on the wrapped response object.
   */
  @Override
  public void setCharacterEncoding(String charset) {
    this.response.setCharacterEncoding(charset);
  }

  /**
   * The default behavior of this method is to return getCharacterEncoding() on the wrapped response object.
   */
  @Override
  public String getCharacterEncoding() {
    return this.response.getCharacterEncoding();
  }

  /**
   * The default behavior of this method is to return getOutputStream() on the wrapped response object.
   */
  @Override
  public MockOutputStream getOutputStream() throws IOException {
    return this.response.getOutputStream();
  }

  /**
   * The default behavior of this method is to return getWriter() on the wrapped response object.
   */
  @Override
  public PrintWriter getWriter() throws IOException {
    return this.response.getWriter();
  }

  /**
   * The default behavior of this method is to call setContentLength(int len) on the wrapped response object.
   */
  @Override
  public void setContentLength(int len) {
    this.response.setContentLength(len);
  }

  /**
   * The default behavior of this method is to call setContentLengthLong(long len) on the wrapped response object.
   */
  @Override
  public void setContentLengthLong(long len) {
    this.response.setContentLengthLong(len);
  }

  /**
   * The default behavior of this method is to call setContentType(String type) on the wrapped response object.
   */
  @Override
  public void setContentType(String type) {
    this.response.setContentType(type);
  }

  /**
   * The default behavior of this method is to return getContentType() on the wrapped response object.
   */
  @Override
  public String getContentType() {
    return this.response.getContentType();
  }

  /**
   * The default behavior of this method is to call setBufferSize(int size) on the wrapped response object.
   */
  @Override
  public void setBufferSize(int size) {
    this.response.setBufferSize(size);
  }

  /**
   * The default behavior of this method is to return getBufferSize() on the wrapped response object.
   */
  @Override
  public int getBufferSize() {
    return this.response.getBufferSize();
  }

  /**
   * The default behavior of this method is to call flushBuffer() on the wrapped response object.
   */
  @Override
  public void flushBuffer() throws IOException {
    this.response.flushBuffer();
  }

  /**
   * The default behavior of this method is to return isCommitted() on the wrapped response object.
   */
  @Override
  public boolean isCommitted() {
    return this.response.isCommitted();
  }

  /**
   * The default behavior of this method is to call reset() on the wrapped response object.
   */
  @Override
  public void reset() {
    this.response.reset();
  }

  /**
   * The default behavior of this method is to call resetBuffer() on the wrapped response object.
   */
  @Override
  public void resetBuffer() {
    this.response.resetBuffer();
  }

  /**
   * The default behavior of this method is to call setLocale(Locale loc) on the wrapped response object.
   */
  @Override
  public void setLocale(Locale loc) {
    this.response.setLocale(loc);
  }

  /**
   * The default behavior of this method is to return getLocale() on the wrapped response object.
   */
  @Override
  public Locale getLocale() {
    return this.response.getLocale();
  }

  /**
   * Checks (recursively) if this ServletResponseWrapper wraps the given {@link MockResponse} instance.
   *
   * @param wrapped the ServletResponse instance to search for
   * @return true if this ServletResponseWrapper wraps the given ServletResponse instance, false otherwise
   */
  public boolean isWrapperFor(MockResponse wrapped) {
    if (response == wrapped) {
      return true;
    }
    else if (response instanceof MockResponseWrapper) {
      return ((MockResponseWrapper) response).isWrapperFor(wrapped);
    }
    else {
      return false;
    }
  }

  /**
   * Checks (recursively) if this ServletResponseWrapper wraps a {@link MockResponse} of the given class type.
   *
   * @param wrappedType the ServletResponse class type to search for
   * @return true if this ServletResponseWrapper wraps a ServletResponse of the given class type, false otherwise
   * @throws IllegalArgumentException if the given class does not implement {@link MockResponse}
   */
  public boolean isWrapperFor(Class<?> wrappedType) {
    if (!MockResponse.class.isAssignableFrom(wrappedType)) {
      throw new IllegalArgumentException("Given class " + wrappedType.getName() + " not a subinterface of "
              + MockResponse.class.getName());
    }
    if (wrappedType.isAssignableFrom(response.getClass())) {
      return true;
    }
    else if (response instanceof MockResponseWrapper) {
      return ((MockResponseWrapper) response).isWrapperFor(wrappedType);
    }
    else {
      return false;
    }
  }

}
