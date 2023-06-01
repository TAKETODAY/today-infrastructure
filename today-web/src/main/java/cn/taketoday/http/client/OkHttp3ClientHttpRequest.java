/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.io.IOException;
import java.net.URI;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.BufferedSink;

/**
 * {@link ClientHttpRequest} implementation based on OkHttp 3.x.
 *
 * <p>Created via the {@link OkHttp3ClientHttpRequestFactory}.
 *
 * @author Luciano Leggieri
 * @author Arjen Poutsma
 * @author Roy Clarkson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class OkHttp3ClientHttpRequest extends AbstractStreamingClientHttpRequest {

  private final URI uri;
  private final HttpMethod method;
  private final OkHttpClient client;

  public OkHttp3ClientHttpRequest(OkHttpClient client, URI uri, HttpMethod method) {
    this.client = client;
    this.uri = uri;
    this.method = method;
  }

  @Override
  public HttpMethod getMethod() {
    return this.method;
  }

  @Override
  public String getMethodValue() {
    return this.method.name();
  }

  @Override
  public URI getURI() {
    return this.uri;
  }

  @Override
  protected ClientHttpResponse executeInternal(HttpHeaders headers, @Nullable Body body) throws IOException {

    RequestBody requestBody;
    if (body != null) {
      requestBody = new BodyRequestBody(headers, body);
    }
    else if (okhttp3.internal.http.HttpMethod.requiresRequestBody(getMethod().name())) {
      String header = headers.getFirst(HttpHeaders.CONTENT_TYPE);
      MediaType contentType = (header != null) ? MediaType.parse(header) : null;
      requestBody = RequestBody.create(contentType, new byte[0]);
    }
    else {
      requestBody = null;
    }
    Request.Builder builder = new Request.Builder()
            .url(this.uri.toURL());
    builder.method(this.method.name(), requestBody);
    headers.forEach((headerName, headerValues) -> {
      for (String headerValue : headerValues) {
        builder.addHeader(headerName, headerValue);
      }
    });
    Request request = builder.build();
    return new OkHttp3ClientHttpResponse(this.client.newCall(request).execute());
  }

  private static class BodyRequestBody extends RequestBody {

    private final HttpHeaders headers;

    private final Body body;

    public BodyRequestBody(HttpHeaders headers, Body body) {
      this.headers = headers;
      this.body = body;
    }

    @Override
    public long contentLength() {
      return this.headers.getContentLength();
    }

    @Nullable
    @Override
    public MediaType contentType() {
      String contentType = this.headers.getFirst(HttpHeaders.CONTENT_TYPE);
      if (StringUtils.hasText(contentType)) {
        return MediaType.parse(contentType);
      }
      else {
        return null;
      }
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
      this.body.writeTo(sink.outputStream());
    }

    @Override
    public boolean isOneShot() {
      return true;
    }
  }

}
