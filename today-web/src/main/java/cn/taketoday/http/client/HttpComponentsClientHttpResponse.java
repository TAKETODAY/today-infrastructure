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

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Nullable;

/**
 * {@link ClientHttpResponse} implementation based on
 * Apache HttpComponents HttpClient.
 *
 * <p>Created via the {@link HttpComponentsClientHttpRequest}.
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HttpComponentsClientHttpRequest#execute()
 * @since 4.0
 */
final class HttpComponentsClientHttpResponse implements ClientHttpResponse {

  private final ClassicHttpResponse httpResponse;

  @Nullable
  private HttpHeaders headers;

  HttpComponentsClientHttpResponse(ClassicHttpResponse httpResponse) {
    this.httpResponse = httpResponse;
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return HttpStatusCode.valueOf(this.httpResponse.getCode());
  }

  @Override
  @Deprecated
  public int getRawStatusCode() {
    return this.httpResponse.getCode();
  }

  @Override
  public String getStatusText() {
    return this.httpResponse.getReasonPhrase();
  }

  @Override
  public HttpHeaders getHeaders() {
    if (this.headers == null) {
      this.headers = HttpHeaders.create();
      for (Header header : this.httpResponse.getHeaders()) {
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
        this.httpResponse.close();
      }
    }
    catch (IOException ex) {
      // Ignore exception on close...
    }
  }

}
