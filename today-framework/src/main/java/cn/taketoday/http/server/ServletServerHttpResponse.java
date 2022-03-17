/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@link ServerHttpResponse} implementation that is based on a {@link HttpServletResponse}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.0
 */
public class ServletServerHttpResponse implements ServerHttpResponse {

  private boolean bodyUsed = false;
  private boolean headersWritten = false;

  private final HttpHeaders headers;
  private final HttpServletResponse servletResponse;

  @Nullable
  private HttpHeaders readOnlyHeaders;

  /**
   * Construct a new instance of the ServletServerHttpResponse based on the given {@link HttpServletResponse}.
   *
   * @param servletResponse the servlet response
   */
  public ServletServerHttpResponse(HttpServletResponse servletResponse) {
    Assert.notNull(servletResponse, "HttpServletResponse must not be null");
    this.servletResponse = servletResponse;
    this.headers = new ServletResponseHttpHeaders();
  }

  /**
   * Return the {@code HttpServletResponse} this object is based on.
   */
  public HttpServletResponse getServletResponse() {
    return this.servletResponse;
  }

  @Override
  public void setStatusCode(HttpStatus status) {
    Assert.notNull(status, "HttpStatus must not be null");
    this.servletResponse.setStatus(status.value());
  }

  @Override
  public HttpHeaders getHeaders() {
    if (this.readOnlyHeaders != null) {
      return this.readOnlyHeaders;
    }
    else if (this.headersWritten) {
      this.readOnlyHeaders = HttpHeaders.readOnlyHttpHeaders(this.headers);
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
      if (servletResponse.getContentType() == null && headers.getContentType() != null) {
        servletResponse.setContentType(headers.getContentType().toString());
      }
      if (servletResponse.getCharacterEncoding() == null
              && headers.getContentType() != null
              && headers.getContentType().getCharset() != null) {
        servletResponse.setCharacterEncoding(headers.getContentType().getCharset().name());
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
  private class ServletResponseHttpHeaders extends DefaultHttpHeaders {
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
        return (value != null ? value : servletResponse.getHeader(headerName));
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
        return Collections.singletonList(getFirst(headerName));
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
