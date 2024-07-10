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

package cn.taketoday.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StreamUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link ClientHttpResponse} implementation that uses standard JDK facilities.
 * Obtained via {@link SimpleClientHttpRequest#execute()}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class SimpleClientHttpResponse implements ClientHttpResponse {

  private final HttpURLConnection connection;

  @Nullable
  private HttpHeaders headers;

  @Nullable
  private InputStream responseStream;

  SimpleClientHttpResponse(HttpURLConnection connection) {
    this.connection = connection;
  }

  @Override
  public HttpStatusCode getStatusCode() throws IOException {
    return HttpStatusCode.valueOf(connection.getResponseCode());
  }

  @Override
  public String getStatusText() throws IOException {
    String result = this.connection.getResponseMessage();
    return (result != null) ? result : "";
  }

  @Override
  public HttpHeaders getHeaders() {
    if (this.headers == null) {
      this.headers = HttpHeaders.forWritable();
      // Header field 0 is the status line for most HttpURLConnections, but not on GAE
      String name = this.connection.getHeaderFieldKey(0);
      if (StringUtils.isNotEmpty(name)) {
        this.headers.add(name, this.connection.getHeaderField(0));
      }
      int i = 1;
      while (true) {
        name = this.connection.getHeaderFieldKey(i);
        if (StringUtils.isEmpty(name)) {
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
    if (this.responseStream == null) {
      if (this.connection.getResponseCode() >= 400) {
        InputStream errorStream = this.connection.getErrorStream();
        this.responseStream = (errorStream != null) ? errorStream : InputStream.nullInputStream();
      }
      else {
        this.responseStream = this.connection.getInputStream();
      }
    }
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
