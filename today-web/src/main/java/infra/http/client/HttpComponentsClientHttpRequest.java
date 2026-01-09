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

package infra.http.client;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.function.Supplier;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.io.entity.NullEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.lang.VisibleForTesting;
import infra.util.StringUtils;

/**
 * {@link ClientHttpRequest} implementation based on
 * Apache HttpComponents HttpClient.
 *
 * <p>Created via the {@link HttpComponentsClientHttpRequestFactory}.
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HttpComponentsClientHttpRequestFactory#createRequest(URI, HttpMethod)
 * @since 4.0
 */
final class HttpComponentsClientHttpRequest extends AbstractStreamingClientHttpRequest {

  private final HttpClient httpClient;

  private final HttpContext httpContext;

  private final ClassicHttpRequest httpRequest;

  private final HttpMethod method;

  HttpComponentsClientHttpRequest(HttpClient client, ClassicHttpRequest request, HttpContext context) {
    this.httpClient = client;
    this.httpRequest = request;
    this.httpContext = context;
    this.method = HttpMethod.valueOf(httpRequest.getMethod());
  }

  @Override
  public HttpMethod getMethod() {
    return method;
  }

  @Override
  public URI getURI() {
    try {
      return this.httpRequest.getUri();
    }
    catch (URISyntaxException ex) {
      throw new IllegalStateException(ex.getMessage(), ex);
    }
  }

  @VisibleForTesting
  HttpContext getHttpContext() {
    return this.httpContext;
  }

  @Override
  protected ClientHttpResponse executeInternal(HttpHeaders headers, @Nullable Body body) throws IOException {
    addHeaders(httpRequest, headers);

    if (body != null) {
      this.httpRequest.setEntity(new BodyEntity(headers, body));
    }
    else if (!Method.isSafe(this.httpRequest.getMethod())) {
      this.httpRequest.setEntity(NullEntity.INSTANCE);
    }
    ClassicHttpResponse httpResponse = this.httpClient.executeOpen(null, this.httpRequest, this.httpContext);
    return new HttpComponentsClientHttpResponse(httpResponse);
  }

  /**
   * Add the given headers to the given HTTP request.
   *
   * @param httpRequest the request to add the headers to
   * @param headers the headers to add
   */
  static void addHeaders(ClassicHttpRequest httpRequest, HttpHeaders headers) {
    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      String headerName = entry.getKey();
      if (HttpHeaders.COOKIE.equalsIgnoreCase(headerName)) {  // RFC 6265
        String headerValue = StringUtils.collectionToDelimitedString(entry.getValue(), "; ");
        httpRequest.addHeader(headerName, headerValue);
      }
      else if (!HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(headerName)
              && !HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(headerName)) {
        for (String headerValue : entry.getValue()) {
          httpRequest.addHeader(headerName, headerValue);
        }
      }
    }
  }

  private static class BodyEntity implements HttpEntity {

    private final HttpHeaders headers;

    private final Body body;

    public BodyEntity(HttpHeaders headers, Body body) {
      this.headers = headers;
      this.body = body;
    }

    @Override
    public long getContentLength() {
      return this.headers.getContentLength();
    }

    @Override
    @Nullable
    public String getContentType() {
      return this.headers.getFirst(HttpHeaders.CONTENT_TYPE);
    }

    @Override
    public InputStream getContent() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void writeTo(OutputStream outStream) throws IOException {
      this.body.writeTo(outStream);
    }

    @Override
    public boolean isRepeatable() {
      return this.body.repeatable();
    }

    @Override
    public boolean isStreaming() {
      return false;
    }

    @Override
    @Nullable
    public Supplier<List<? extends Header>> getTrailers() {
      return null;
    }

    @Override
    @Nullable
    public String getContentEncoding() {
      return this.headers.getFirst(HttpHeaders.CONTENT_ENCODING);
    }

    @Override
    public boolean isChunked() {
      return false;
    }

    @Override
    @Nullable
    public Set<String> getTrailerNames() {
      return null;
    }

    @Override
    public void close() {

    }

  }

}
