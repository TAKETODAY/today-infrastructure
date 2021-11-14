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

import java.io.IOException;
import java.net.URI;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * {@link ClientHttpRequest} implementation based on OkHttp 3.x.
 *
 * <p>Created via the {@link OkHttp3ClientHttpRequestFactory}.
 *
 * @author Luciano Leggieri
 * @author Arjen Poutsma
 * @author Roy Clarkson
 * @since 4.0
 */
class OkHttp3ClientHttpRequest extends AbstractBufferingClientHttpRequest {

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
  protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] content) throws IOException {
    Request request = OkHttp3ClientHttpRequestFactory.buildRequest(headers, content, this.uri, this.method);
    return new OkHttp3ClientHttpResponse(this.client.newCall(request).execute());
  }

}
