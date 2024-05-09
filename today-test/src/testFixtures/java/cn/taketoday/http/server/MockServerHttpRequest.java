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

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.InvalidMediaTypeException;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.MockRequest;
import cn.taketoday.util.ArrayIterator;
import cn.taketoday.util.LinkedCaseInsensitiveMap;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.mock.MockUtils;
import cn.taketoday.mock.api.http.HttpMockRequest;

/**
 * {@link ServerHttpRequest} implementation that is based on a {@link HttpMockRequest}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0
 */
public class MockServerHttpRequest implements ServerHttpRequest {

  protected static final Charset FORM_CHARSET = StandardCharsets.UTF_8;

  private final HttpMockRequest mockRequest;

  @Nullable
  private URI uri;

  @Nullable
  private HttpHeaders headers;

  @Nullable
  private ServerHttpAsyncRequestControl asyncRequestControl;

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

  @Override
  public ServerHttpAsyncRequestControl getAsyncRequestControl(ServerHttpResponse response) {
    if (this.asyncRequestControl == null) {
      if (!(response instanceof MockServerHttpResponse servletServerResponse)) {
        throw new IllegalArgumentException(
                "Response must be a ServletServerHttpResponse: " + response.getClass());
      }
      this.asyncRequestControl = new MockServerHttpAsyncRequestControl(this, servletServerResponse);
    }
    return this.asyncRequestControl;
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
