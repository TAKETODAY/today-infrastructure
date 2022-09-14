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
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;

/**
 * {@link ClientHttpResponse} implementation based on
 * Apache HttpComponents HttpClient.
 *
 * <p>Created via the {@link HttpComponentsClientHttpRequest}.
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @see HttpComponentsClientHttpRequest#execute()
 * @since 4.0
 */
final class HttpComponentsClientHttpResponse extends AbstractClientHttpResponse {

  private final HttpResponse httpResponse;

  @Nullable
  private HttpHeaders headers;

  HttpComponentsClientHttpResponse(HttpResponse httpResponse) {
    this.httpResponse = httpResponse;
  }

  @Override
  public int getRawStatusCode() throws IOException {
    return this.httpResponse.getStatusLine().getStatusCode();
  }

  @Override
  public String getStatusText() throws IOException {
    return this.httpResponse.getStatusLine().getReasonPhrase();
  }

  @Override
  public HttpHeaders getHeaders() {
    if (this.headers == null) {
      this.headers = HttpHeaders.create();
      for (Header header : this.httpResponse.getAllHeaders()) {
        this.headers.add(header.getName(), header.getValue());
      }
    }
    return this.headers;
  }

  @Override
  public InputStream getBody() throws IOException {
    HttpEntity entity = this.httpResponse.getEntity();
    return (entity != null ? entity.getContent() : InputStream.nullInputStream());
  }

  @Override
  public void close() {
    // Release underlying connection back to the connection manager
    try {
      try {
        // Attempt to keep connection alive by consuming its remaining content
        EntityUtils.consume(this.httpResponse.getEntity());
      }
      finally {
        if (this.httpResponse instanceof Closeable) {
          ((Closeable) this.httpResponse).close();
        }
      }
    }
    catch (IOException ex) {
      // Ignore exception on close...
    }
  }

}
