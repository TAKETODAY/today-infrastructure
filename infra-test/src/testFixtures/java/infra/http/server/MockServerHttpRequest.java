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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import infra.http.AbstractHttpRequest;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.InvalidMediaTypeException;
import infra.http.MediaType;
import infra.lang.Assert;
import org.jspecify.annotations.Nullable;
import infra.mock.api.MockRequest;
import infra.mock.api.http.HttpMockRequest;
import infra.util.ArrayIterator;
import infra.util.LinkedCaseInsensitiveMap;
import infra.util.StringUtils;
import infra.web.mock.MockUtils;

/**
 * {@link ServerHttpRequest} implementation that is based on a {@link HttpMockRequest}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0
 */
public class MockServerHttpRequest extends AbstractHttpRequest implements ServerHttpRequest {

  protected static final Charset FORM_CHARSET = StandardCharsets.UTF_8;

  private final HttpMockRequest mockRequest;

  @Nullable
  private URI uri;

  @Nullable
  private HttpHeaders headers;

  private final HttpMethod method;

  /**
   * Construct a new instance of the ServletServerHttpRequest based on the
   * given {@link HttpMockRequest}.
   *
   * @param mockRequest the servlet request
   */
  public MockServerHttpRequest(HttpMockRequest mockRequest) {
    Assert.notNull(mockRequest, "HttpServletRequest is required");
    this.mockRequest = mockRequest;
    this.method = HttpMethod.valueOf(mockRequest.getMethod());
  }

  /**
   * Returns the {@code HttpServletRequest} this object is based on.
   */
  public HttpMockRequest getRequest() {
    return this.mockRequest;
  }

  @Override
  public HttpMethod getMethod() {
    return method;
  }

  @Override
  public URI getURI() {
    if (this.uri == null) {
      this.uri = initURI(this.mockRequest);
    }
    return this.uri;
  }

  /**
   * Initialize a URI from the given Servlet request.
   *
   * @param servletRequest the request
   * @return the initialized URI
   */
  public static URI initURI(HttpMockRequest servletRequest) {
    String urlString = null;
    boolean hasQuery = false;
    try {
      StringBuffer url = servletRequest.getRequestURL();
      String query = servletRequest.getQueryString();
      hasQuery = StringUtils.hasText(query);
      if (hasQuery) {
        url.append('?').append(query);
      }
      urlString = url.toString();
      return new URI(urlString);
    }
    catch (URISyntaxException ex) {
      if (!hasQuery) {
        throw new IllegalStateException(
                "Could not resolve HttpServletRequest as URI: " + urlString, ex);
      }
      // Maybe a malformed query string... try plain request URL
      try {
        urlString = servletRequest.getRequestURL().toString();
        return new URI(urlString);
      }
      catch (URISyntaxException ex2) {
        throw new IllegalStateException(
                "Could not resolve HttpServletRequest as URI: " + urlString, ex2);
      }
    }
  }

  @Override
  public HttpHeaders getHeaders() {
    if (this.headers == null) {
      this.headers = HttpHeaders.forWritable();

      for (Enumeration<String> names = this.mockRequest.getHeaderNames(); names.hasMoreElements(); ) {
        String headerName = names.nextElement();
        this.headers.addAll(headerName, this.mockRequest.getHeaders(headerName));
      }

      // HttpServletRequest exposes some headers as properties:
      // we should include those if not already present
      try {
        MediaType contentType = this.headers.getContentType();
        if (contentType == null) {
          String requestContentType = this.mockRequest.getContentType();
          if (StringUtils.isNotEmpty(requestContentType)) {
            contentType = MediaType.parseMediaType(requestContentType);
            if (contentType.isConcrete()) {
              this.headers.setContentType(contentType);
            }
          }
        }
        if (contentType != null && contentType.getCharset() == null) {
          String requestEncoding = this.mockRequest.getCharacterEncoding();
          if (StringUtils.isNotEmpty(requestEncoding)) {
            Charset charSet = Charset.forName(requestEncoding);
            Map<String, String> params = new LinkedCaseInsensitiveMap<>();
            params.putAll(contentType.getParameters());
            params.put("charset", charSet.toString());
            MediaType mediaType = new MediaType(contentType.getType(), contentType.getSubtype(), params);
            this.headers.setContentType(mediaType);
          }
        }
      }
      catch (InvalidMediaTypeException ex) {
        // Ignore: simply not exposing an invalid content type in HttpHeaders...
      }

      if (this.headers.getContentLength() < 0) {
        int requestContentLength = this.mockRequest.getContentLength();
        if (requestContentLength != -1) {
          this.headers.setContentLength(requestContentLength);
        }
      }
    }

    return this.headers;
  }

  @Override
  public Principal getPrincipal() {
    return this.mockRequest.getUserPrincipal();
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    return new InetSocketAddress(this.mockRequest.getLocalAddr(), this.mockRequest.getLocalPort());
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return new InetSocketAddress(this.mockRequest.getRemoteHost(), this.mockRequest.getRemotePort());
  }

  @Override
  public InputStream getBody() throws IOException {
    if (MockUtils.isPostForm(this.mockRequest) && this.mockRequest.getQueryString() == null) {
      return getBodyFromRequestParameters(this.mockRequest);
    }
    else {
      return this.mockRequest.getInputStream();
    }
  }

  /**
   * Use {@link MockRequest#getParameterMap()} to reconstruct the
   * body of a form 'POST' providing a predictable outcome as opposed to reading
   * from the body, which can fail if any other code has used the ServletRequest
   * to access a parameter, thus causing the input stream to be "consumed".
   */
  private InputStream getBodyFromRequestParameters(HttpMockRequest request) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
    OutputStreamWriter writer = new OutputStreamWriter(bos, FORM_CHARSET);

    Map<String, String[]> form = request.getParameterMap();
    for (Iterator<Map.Entry<String, String[]>> entryIterator = form.entrySet().iterator(); entryIterator.hasNext(); ) {
      Map.Entry<String, String[]> entry = entryIterator.next();
      String name = entry.getKey();
      ArrayIterator<String> valueIterator = new ArrayIterator<>(entry.getValue());
      while (valueIterator.hasNext()) {
        String value = valueIterator.next();
        writer.write(URLEncoder.encode(name, FORM_CHARSET));
        if (value != null) {
          writer.write('=');
          writer.write(URLEncoder.encode(value, FORM_CHARSET));
          if (valueIterator.hasNext()) {
            writer.write('&');
          }
        }
      }
      if (entryIterator.hasNext()) {
        writer.append('&');
      }
    }
    writer.flush();

    byte[] bytes = bos.toByteArray();
    if (bytes.length > 0 && getHeaders().containsKey(HttpHeaders.CONTENT_LENGTH)) {
      getHeaders().setContentLength(bytes.length);
    }

    return new ByteArrayInputStream(bytes);
  }

}
