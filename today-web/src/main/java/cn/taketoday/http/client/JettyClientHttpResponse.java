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

import org.eclipse.jetty.client.api.Response;

import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.util.MultiValueMap;

/**
 * {@link ClientHttpResponse} implementation based on based on Jetty's
 * {@link org.eclipse.jetty.client.HttpClient}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JettyClientHttpResponse implements ClientHttpResponse {

  private final Response response;

  private final InputStream body;

  private final HttpHeaders headers;

  public JettyClientHttpResponse(Response response, InputStream inputStream) {
    this.response = response;
    this.body = inputStream;

    MultiValueMap<String, String> headers = new JettyHeadersAdapter(response.getHeaders());
    this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
  }

  @Override
  public HttpStatusCode getStatusCode() throws IOException {
    return HttpStatusCode.valueOf(this.response.getStatus());
  }

  @Override
  public int getRawStatusCode() throws IOException {
    return response.getStatus();
  }

  @Override
  public String getStatusText() throws IOException {
    return this.response.getReason();
  }

  @Override
  public HttpHeaders getHeaders() {
    return this.headers;
  }

  @Override
  public InputStream getBody() throws IOException {
    return this.body;
  }

  @Override
  public void close() {
    try {
      this.body.close();
    }
    catch (IOException ignored) { }
  }
}
