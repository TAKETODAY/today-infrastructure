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

package cn.taketoday.http.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.http.DefaultHttpHeaders;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.mock.api.http.HttpMockResponse;

/**
 * {@link ServerHttpResponse} implementation that is based on a {@link HttpMockResponse}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Rossen Stoyanchev
 * @since 3.0
 */
public class MockServerHttpResponse implements ServerHttpResponse {

  private boolean bodyUsed = false;
  private boolean headersWritten = false;

  private final HttpHeaders headers;
  private final HttpMockResponse servletResponse;

  @Nullable
  private HttpHeaders readOnlyHeaders;

  /**
   * Construct a new instance of the ServletServerHttpResponse based on the given {@link HttpMockResponse}.
   *
   * @param servletResponse the servlet response
   */
  public MockServerHttpResponse(HttpMockResponse servletResponse) {
    Assert.notNull(servletResponse, "HttpServletResponse is required");
    this.servletResponse = servletResponse;
    this.headers = new MockResponseHttpHeaders();
  }

  /**
   * Return the {@code HttpServletResponse} this object is based on.
   */
  public HttpMockResponse getServletResponse() {
    return this.servletResponse;
  }

  @Override
  public void setStatusCode(HttpStatusCode status) {
    Assert.notNull(status, "HttpStatus is required");
    this.servletResponse.setStatus(status.value());
  }

  @Override
  public HttpHeaders getHeaders() {
    if (this.readOnlyHeaders != null) {
      return this.readOnlyHeaders;
    }
    else if (this.headersWritten) {
      this.readOnlyHeaders = headers.asReadOnly();
      return this.readOnlyHeaders;
    }
    else {
      return this.headers;
    }
  }

  @Override
  public OutputStream getBody() throws IOException {
    this.bodyUsed = true;
    writeHeaders();
    return this.servletResponse.getOutputStream();
  }

  @Override
  public void flush() throws IOException {
    writeHeaders();
    if (this.bodyUsed) {
      this.servletResponse.flushBuffer();
    }
  }

  @Override
  public void close() {
    writeHeaders();
  }

  private void writeHeaders() {
    if (!headersWritten) {
      HttpHeaders headers = getHeaders();
      for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
        String headerName = entry.getKey();
        for (String headerValue : entry.getValue()) {
          servletResponse.addHeader(headerName, headerValue);
        }
      }

      // HttpServletResponse exposes some headers as properties: we should include those if not already present
      MediaType contentTypeHeader = this.headers.getContentType();
      if (servletResponse.getContentType() == null && contentTypeHeader != null) {
        servletResponse.setContentType(contentTypeHeader.toString());
      }
      if (servletResponse.getCharacterEncoding() == null
              && contentTypeHeader != null && contentTypeHeader.getCharset() != null) {
        servletResponse.setCharacterEncoding(contentTypeHeader.getCharset().name());
      }
      long contentLength = headers.getContentLength();
      if (contentLength != -1) {
        servletResponse.setContentLengthLong(contentLength);
      }
      this.headersWritten = true;
    }
  }

  /**
   * Extends HttpHeaders with the ability to look up headers already present in
   * the underlying HttpServletResponse.
   *
   * <p>The intent is merely to expose what is available through the HttpServletResponse
   * i.e. the ability to look up specific header values by name. All other
   * map-related operations (e.g. iteration, removal, etc) apply only to values
   * added directly through HttpHeaders methods.
   */
  private class MockResponseHttpHeaders extends DefaultHttpHeaders {
    @Serial
    private static final long serialVersionUID = 3410708522401046302L;

    @Override
    public boolean containsKey(Object key) {
      return super.containsKey(key) || (get(key) != null);
    }

    @Override
    @Nullable
    public String getFirst(String headerName) {
      if (headerName.equalsIgnoreCase(CONTENT_TYPE)) {
        // Content-Type is written as an override so check super first
        String value = super.getFirst(headerName);
        return (value != null ? value : servletResponse.getContentType());
      }
      else {
        String value = servletResponse.getHeader(headerName);
        return value != null ? value : super.getFirst(headerName);
      }
    }

    @Override
    public List<String> get(Object key) {
      Assert.isInstanceOf(String.class, key, "Key must be a String-based header name");

      String headerName = (String) key;
      if (headerName.equalsIgnoreCase(CONTENT_TYPE)) {
        // Content-Type is written as an override so don't merge
        String value = getFirst(headerName);
        return (value != null ? Collections.singletonList(value) : null);
      }

      Collection<String> values1 = servletResponse.getHeaders(headerName);
      if (headersWritten) {
        return new ArrayList<>(values1);
      }
      boolean isEmpty1 = CollectionUtils.isEmpty(values1);

      List<String> values2 = super.get(key);
      boolean isEmpty2 = CollectionUtils.isEmpty(values2);

      if (isEmpty1 && isEmpty2) {
        return null;
      }

      ArrayList<String> values = new ArrayList<>();
      if (!isEmpty1) {
        values.addAll(values1);
      }
      if (!isEmpty2) {
        values.addAll(values2);
      }
      return values;
    }
  }

}
