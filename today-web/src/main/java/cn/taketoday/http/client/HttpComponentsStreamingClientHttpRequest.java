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

package cn.taketoday.http.client;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.function.Supplier;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.StreamingHttpOutputMessage;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * {@link ClientHttpRequest} implementation based on
 * Apache HttpComponents HttpClient in streaming mode.
 *
 * <p>Created via the {@link HttpComponentsClientHttpRequestFactory}.
 *
 * @author Arjen Poutsma
 * @see HttpComponentsClientHttpRequestFactory#createRequest(URI, cn.taketoday.http.HttpMethod)
 * @since 4.0
 */
final class HttpComponentsStreamingClientHttpRequest
        extends AbstractClientHttpRequest implements StreamingHttpOutputMessage {

  private final HttpClient httpClient;

  private final ClassicHttpRequest httpRequest;

  private final HttpContext httpContext;

  @Nullable
  private Body body;

  HttpComponentsStreamingClientHttpRequest(HttpClient client, ClassicHttpRequest request, HttpContext context) {
    this.httpClient = client;
    this.httpRequest = request;
    this.httpContext = context;
  }

  @Override
  public HttpMethod getMethod() {
    return HttpMethod.valueOf(this.httpRequest.getMethod());
  }

  @Override
  @Deprecated
  public String getMethodValue() {
    return this.httpRequest.getMethod();
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

  @Override
  public void setBody(Body body) {
    assertNotExecuted();
    this.body = body;
  }

  @Override
  protected OutputStream getBodyInternal(HttpHeaders headers) {
    throw new UnsupportedOperationException("getBody not supported");
  }

  @Override
  protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
    HttpComponentsClientHttpRequest.addHeaders(httpRequest, headers);

    if (this.body != null) {
      HttpEntity requestEntity = new StreamingHttpEntity(getHeaders(), this.body);
      httpRequest.setEntity(requestEntity);
    }
    HttpResponse httpResponse = httpClient.execute(this.httpRequest, this.httpContext);
    Assert.isInstanceOf(ClassicHttpResponse.class, httpResponse,
            "HttpResponse not an instance of ClassicHttpResponse");
    return new HttpComponentsClientHttpResponse((ClassicHttpResponse) httpResponse);
  }

  private record StreamingHttpEntity(HttpHeaders headers, Body body) implements HttpEntity {

    @Override
    public boolean isRepeatable() {
      return false;
    }

    @Override
    public boolean isChunked() {
      return false;
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
    @Nullable
    public String getContentEncoding() {
      return this.headers.getFirst(HttpHeaders.CONTENT_ENCODING);
    }

    @Override
    public InputStream getContent() throws IllegalStateException {
      throw new IllegalStateException("No content available");
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
      this.body.writeTo(outputStream);
    }

    @Override
    public boolean isStreaming() {
      return true;
    }

    @Override
    @Nullable
    public Supplier<List<? extends Header>> getTrailers() {
      return null;
    }

    @Override
    @Nullable
    public Set<String> getTrailerNames() {
      return null;
    }

    @Override
    public void close() throws IOException { }
  }

}
