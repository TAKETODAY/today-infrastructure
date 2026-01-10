/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.http.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.http.DefaultHttpHeaders;
import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.lang.Assert;
import org.jspecify.annotations.Nullable;
import infra.util.CollectionUtils;
import infra.mock.api.http.HttpMockResponse;

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
  private final HttpMockResponse mockResponse;

  @Nullable
  private HttpHeaders readOnlyHeaders;

  /**
   * Construct a new instance of the MockServerHttpResponse based on the given {@link HttpMockResponse}.
   *
   * @param mockResponse the servlet response
   */
  public MockServerHttpResponse(HttpMockResponse mockResponse) {
    Assert.notNull(mockResponse, "HttpMockResponse is required");
    this.mockResponse = mockResponse;
    this.headers = new MockResponseHttpHeaders();
  }

  /**
   * Return the {@code HttpMockResponse} this object is based on.
   */
  public HttpMockResponse getResponse() {
    return this.mockResponse;
  }

  @Override
  public void setStatusCode(HttpStatusCode status) {
    Assert.notNull(status, "HttpStatus is required");
    this.mockResponse.setStatus(status.value());
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
    return this.mockResponse.getOutputStream();
  }

  @Override
  public void flush() throws IOException {
    writeHeaders();
    if (this.bodyUsed) {
      this.mockResponse.flushBuffer();
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
          mockResponse.addHeader(headerName, headerValue);
        }
      }

      // HttpMockResponse exposes some headers as properties: we should include those if not already present
      MediaType contentTypeHeader = this.headers.getContentType();
      if (mockResponse.getContentType() == null && contentTypeHeader != null) {
        mockResponse.setContentType(contentTypeHeader.toString());
      }
      if (mockResponse.getCharacterEncoding() == null
              && contentTypeHeader != null && contentTypeHeader.getCharset() != null) {
        mockResponse.setCharacterEncoding(contentTypeHeader.getCharset().name());
      }
      long contentLength = headers.getContentLength();
      if (contentLength != -1) {
        mockResponse.setContentLengthLong(contentLength);
      }
      this.headersWritten = true;
    }
  }

  /**
   * Extends HttpHeaders with the ability to look up headers already present in
   * the underlying HttpMockResponse.
   *
   * <p>The intent is merely to expose what is available through the HttpMockResponse
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
    public String getFirst(String name) {
      if (name.equalsIgnoreCase(CONTENT_TYPE)) {
        // Content-Type is written as an override so check super first
        String value = super.getFirst(name);
        return (value != null ? value : mockResponse.getContentType());
      }
      else {
        String value = mockResponse.getHeader(name);
        return value != null ? value : super.getFirst(name);
      }
    }

    @Override
    public List<String> get(Object name) {
      Assert.isInstanceOf(String.class, name, "Key must be a String-based header name");

      String headerName = (String) name;
      if (headerName.equalsIgnoreCase(CONTENT_TYPE)) {
        // Content-Type is written as an override so don't merge
        String value = getFirst(headerName);
        return (value != null ? Collections.singletonList(value) : null);
      }

      Collection<String> values1 = mockResponse.getHeaders(headerName);
      if (headersWritten) {
        return new ArrayList<>(values1);
      }
      boolean isEmpty1 = CollectionUtils.isEmpty(values1);

      List<String> values2 = super.get(name);
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
