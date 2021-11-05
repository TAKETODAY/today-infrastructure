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

package cn.taketoday.web.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StreamUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.http.HttpHeaders;

/**
 * {@link ClientHttpResponse} implementation that uses standard JDK facilities.
 * Obtained via {@link SimpleBufferingClientHttpRequest#execute()} and
 * {@link SimpleStreamingClientHttpRequest#execute()}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 3.0
 */
final class SimpleClientHttpResponse extends AbstractClientHttpResponse {

  private final HttpURLConnection connection;

  @Nullable
  private HttpHeaders headers;

  @Nullable
  private InputStream responseStream;

  SimpleClientHttpResponse(HttpURLConnection connection) {
    this.connection = connection;
  }

  @Override
  public int getRawStatusCode() throws IOException {
    return this.connection.getResponseCode();
  }

  @Override
  public String getStatusText() throws IOException {
    String result = this.connection.getResponseMessage();
    return (result != null) ? result : "";
  }

  @Override
  public HttpHeaders getHeaders() {
    if (this.headers == null) {
      this.headers = new HttpHeaders();
      // Header field 0 is the status line for most HttpURLConnections, but not on GAE
      String name = this.connection.getHeaderFieldKey(0);
      if (StringUtils.hasLength(name)) {
        this.headers.add(name, this.connection.getHeaderField(0));
      }
      int i = 1;
      while (true) {
        name = this.connection.getHeaderFieldKey(i);
        if (!StringUtils.hasLength(name)) {
          break;
        }
        this.headers.add(name, this.connection.getHeaderField(i));
        i++;
      }
    }
    return this.headers;
  }

  @Override
  public InputStream getBody() throws IOException {
    InputStream errorStream = this.connection.getErrorStream();
    this.responseStream = (errorStream != null ? errorStream : this.connection.getInputStream());
    return this.responseStream;
  }

  @Override
  public void close() {
    try {
      if (this.responseStream == null) {
        getBody();
      }
      StreamUtils.drain(this.responseStream);
      this.responseStream.close();
    }
    catch (Exception ex) {
      // ignore
    }
  }

}
