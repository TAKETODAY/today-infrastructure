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
import java.io.InputStream;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StreamUtils;
import kotlin.Pair;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * {@link ClientHttpResponse} implementation based on OkHttp 3.x.
 *
 * @author Luciano Leggieri
 * @author Arjen Poutsma
 * @author Roy Clarkson
 * @since 4.0
 */
class OkHttp3ClientHttpResponse extends AbstractClientHttpResponse {

  private final Response response;

  @Nullable
  private volatile HttpHeaders headers;

  public OkHttp3ClientHttpResponse(Response response) {
    Assert.notNull(response, "Response must not be null");
    this.response = response;
  }

  @Override
  public int getRawStatusCode() {
    return this.response.code();
  }

  @Override
  public String getStatusText() {
    return this.response.message();
  }

  @Override
  public InputStream getBody() throws IOException {
    ResponseBody body = this.response.body();
    return body != null ? body.byteStream() : StreamUtils.emptyInput();
  }

  @Override
  public HttpHeaders getHeaders() {
    HttpHeaders headers = this.headers;
    if (headers == null) {
      headers = HttpHeaders.create();
      for (Pair<? extends String, ? extends String> header : response.headers()) {
        headers.add(header.getFirst(), header.getSecond());
      }
      this.headers = headers;
    }
    return headers;
  }

  @Override
  public void close() {
    ResponseBody body = this.response.body();
    if (body != null) {
      body.close();
    }
  }

}
