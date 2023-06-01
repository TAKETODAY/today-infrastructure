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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StreamUtils;

/**
 * {@link ClientHttpRequest} implementation that uses standard JDK facilities to
 * execute streaming requests. Created via the {@link SimpleClientHttpRequestFactory}.
 *
 * @author Arjen Poutsma
 * @see SimpleClientHttpRequestFactory#createRequest(URI, HttpMethod)
 * @see cn.taketoday.http.client.support.HttpAccessor
 * @see cn.taketoday.web.client.RestTemplate
 * @since 4.0
 */
final class SimpleStreamingClientHttpRequest extends AbstractClientHttpRequest {

  private final HttpURLConnection connection;

  private final int chunkSize;

  @Nullable
  private OutputStream body;

  private final boolean outputStreaming;

  SimpleStreamingClientHttpRequest(HttpURLConnection connection, int chunkSize, boolean outputStreaming) {
    this.connection = connection;
    this.chunkSize = chunkSize;
    this.outputStreaming = outputStreaming;
  }

  @Override
  public String getMethodValue() {
    return this.connection.getRequestMethod();
  }

  @Override
  public URI getURI() {
    try {
      return this.connection.getURL().toURI();
    }
    catch (URISyntaxException ex) {
      throw new IllegalStateException("Could not get HttpURLConnection URI: " + ex.getMessage(), ex);
    }
  }

  @Override
  protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
    if (this.body == null) {
      if (this.outputStreaming) {
        long contentLength = headers.getContentLength();
        if (contentLength >= 0) {
          this.connection.setFixedLengthStreamingMode(contentLength);
        }
        else {
          this.connection.setChunkedStreamingMode(this.chunkSize);
        }
      }
      SimpleClientHttpRequest.addHeaders(this.connection, headers);
      this.connection.connect();
      this.body = this.connection.getOutputStream();
    }
    return StreamUtils.nonClosing(this.body);
  }

  @Override
  protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
    try {
      if (this.body != null) {
        this.body.close();
      }
      else {
        SimpleClientHttpRequest.addHeaders(this.connection, headers);
        this.connection.connect();
        // Immediately trigger the request in a no-output scenario as well
        this.connection.getResponseCode();
      }
    }
    catch (IOException ex) {
      // ignore
    }
    return new SimpleClientHttpResponse(this.connection);
  }

}
