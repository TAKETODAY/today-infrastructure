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
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ArrayIterator;
import cn.taketoday.util.InvalidMediaTypeException;
import cn.taketoday.util.LinkedCaseInsensitiveMap;
import cn.taketoday.util.MediaType;
import cn.taketoday.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;

/**
 * {@link ServerHttpRequest} implementation that is based on a {@link HttpServletRequest}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ServletServerHttpRequest implements ServerHttpRequest {

  protected static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
  protected static final Charset FORM_CHARSET = StandardCharsets.UTF_8;

  private final HttpServletRequest servletRequest;

  @Nullable
  private URI uri;

  @Nullable
  private HttpHeaders headers;

  @Nullable
  private ServerHttpAsyncRequestControl asyncRequestControl;

  /**
   * Construct a new instance of the ServletServerHttpRequest based on the
   * given {@link HttpServletRequest}.
   *
   * @param servletRequest the servlet request
   */
  public ServletServerHttpRequest(HttpServletRequest servletRequest) {
    Assert.notNull(servletRequest, "HttpServletRequest must not be null");
    this.servletRequest = servletRequest;
  }

  /**
   * Returns the {@code HttpServletRequest} this object is based on.
   */
  public HttpServletRequest getServletRequest() {
    return this.servletRequest;
  }

  @Override
  @Nullable
  public HttpMethod getMethod() {
    return HttpMethod.from(this.servletRequest.getMethod());
  }

  @Override
  public String getMethodValue() {
    return this.servletRequest.getMethod();
  }

  @Override
  public URI getURI() {
    if (this.uri == null) {
      String urlString = null;
      boolean hasQuery = false;
      try {
        StringBuffer url = this.servletRequest.getRequestURL();
        String query = this.servletRequest.getQueryString();
        hasQuery = StringUtils.hasText(query);
        if (hasQuery) {
          url.append('?').append(query);
        }
        urlString = url.toString();
        this.uri = new URI(urlString);
      }
      catch (URISyntaxException ex) {
        if (!hasQuery) {
          throw new IllegalStateException(
                  "Could not resolve HttpServletRequest as URI: " + urlString, ex);
        }
        // Maybe a malformed query string... try plain request URL
        try {
          urlString = this.servletRequest.getRequestURL().toString();
          this.uri = new URI(urlString);
        }
        catch (URISyntaxException ex2) {
          throw new IllegalStateException(
                  "Could not resolve HttpServletRequest as URI: " + urlString, ex2);
        }
      }
    }
    return this.uri;
  }

  @Override
  public HttpHeaders getHeaders() {
    if (this.headers == null) {
      this.headers = HttpHeaders.create();

      for (Enumeration<String> names = this.servletRequest.getHeaderNames(); names.hasMoreElements(); ) {
        String headerName = names.nextElement();
        this.headers.addAll(headerName, this.servletRequest.getHeaders(headerName));
      }

      // HttpServletRequest exposes some headers as properties:
      // we should include those if not already present
      try {
        MediaType contentType = this.headers.getContentType();
        if (contentType == null) {
          String requestContentType = this.servletRequest.getContentType();
          if (StringUtils.isNotEmpty(requestContentType)) {
            contentType = MediaType.parseMediaType(requestContentType);
            if (contentType.isConcrete()) {
              this.headers.setContentType(contentType);
            }
          }
        }
        if (contentType != null && contentType.getCharset() == null) {
          String requestEncoding = this.servletRequest.getCharacterEncoding();
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
        int requestContentLength = this.servletRequest.getContentLength();
        if (requestContentLength != -1) {
          this.headers.setContentLength(requestContentLength);
        }
      }
    }

    return this.headers;
  }

  @Override
  public Principal getPrincipal() {
    return this.servletRequest.getUserPrincipal();
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    return new InetSocketAddress(this.servletRequest.getLocalName(), this.servletRequest.getLocalPort());
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return new InetSocketAddress(this.servletRequest.getRemoteHost(), this.servletRequest.getRemotePort());
  }

  @Override
  public InputStream getBody() throws IOException {
    if (isFormPost(this.servletRequest)) {
      return getBodyFromServletRequestParameters(this.servletRequest);
    }
    else {
      return this.servletRequest.getInputStream();
    }
  }

  @Override
  public ServerHttpAsyncRequestControl getAsyncRequestControl(ServerHttpResponse response) {
    if (this.asyncRequestControl == null) {
      if (!(response instanceof ServletServerHttpResponse servletServerResponse)) {
        throw new IllegalArgumentException(
                "Response must be a ServletServerHttpResponse: " + response.getClass());
      }
      this.asyncRequestControl = new ServletServerHttpAsyncRequestControl(this, servletServerResponse);
    }
    return this.asyncRequestControl;
  }

  private static boolean isFormPost(HttpServletRequest request) {
    String contentType = request.getContentType();
    return (contentType != null && contentType.contains(FORM_CONTENT_TYPE) &&
            HttpMethod.POST.matches(request.getMethod()));
  }

  /**
   * Use {@link jakarta.servlet.ServletRequest#getParameterMap()} to reconstruct the
   * body of a form 'POST' providing a predictable outcome as opposed to reading
   * from the body, which can fail if any other code has used the ServletRequest
   * to access a parameter, thus causing the input stream to be "consumed".
   */
  private static InputStream getBodyFromServletRequestParameters(HttpServletRequest request) throws IOException {
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

    return new ByteArrayInputStream(bos.toByteArray());
  }

}
