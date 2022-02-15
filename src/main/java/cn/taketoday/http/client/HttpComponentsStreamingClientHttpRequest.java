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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.StreamingHttpOutputMessage;
import cn.taketoday.lang.Nullable;
import cn.taketoday.http.MediaType;

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
  private final HttpContext httpContext;
  private final HttpUriRequest httpRequest;

  @Nullable
  private Body body;

  HttpComponentsStreamingClientHttpRequest(HttpClient client, HttpUriRequest request, HttpContext context) {
    this.httpClient = client;
    this.httpRequest = request;
    this.httpContext = context;
  }

  @Override
  public String getMethodValue() {
    return this.httpRequest.getMethod();
  }

  @Override
  public URI getURI() {
    return this.httpRequest.getURI();
  }

  @Override
  public void setBody(Body body) {
    assertNotExecuted();
    this.body = body;
  }

  @Override
  protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
    throw new UnsupportedOperationException("getBody not supported");
  }

  @Override
  protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
    HttpComponentsClientHttpRequest.addHeaders(this.httpRequest, headers);

    if (this.body != null
            && this.httpRequest instanceof HttpEntityEnclosingRequest entityEnclosingRequest) {
      HttpEntity requestEntity = new StreamingHttpEntity(getHeaders(), this.body);
      entityEnclosingRequest.setEntity(requestEntity);
    }

    HttpResponse httpResponse = this.httpClient.execute(this.httpRequest, this.httpContext);
    return new HttpComponentsClientHttpResponse(httpResponse);
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
    public Header getContentType() {
      MediaType contentType = this.headers.getContentType();
      return contentType != null
             ? new BasicHeader(HttpHeaders.CONTENT_TYPE, contentType.toString())
             : null;
    }

    @Override
    @Nullable
    public Header getContentEncoding() {
      String contentEncoding = this.headers.getFirst(HttpHeaders.CONTENT_ENCODING);
      return contentEncoding != null
             ? new BasicHeader(HttpHeaders.CONTENT_ENCODING, contentEncoding)
             : null;
    }

    @Override
    public InputStream getContent() throws IOException, IllegalStateException {
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
    public void consumeContent() throws IOException {
      throw new UnsupportedOperationException();
    }
  }

}
