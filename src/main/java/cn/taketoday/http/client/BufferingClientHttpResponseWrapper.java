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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StreamUtils;

/**
 * Simple implementation of {@link ClientHttpResponse} that reads the response's body
 * into memory, thus allowing for multiple invocations of {@link #getBody()}.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
final class BufferingClientHttpResponseWrapper implements ClientHttpResponse {

  private final ClientHttpResponse response;

  @Nullable
  private byte[] body;

  BufferingClientHttpResponseWrapper(ClientHttpResponse response) {
    this.response = response;
  }

  @Override
  public HttpStatus getStatusCode() throws IOException {
    return this.response.getStatusCode();
  }

  @Override
  public int getRawStatusCode() throws IOException {
    return this.response.getRawStatusCode();
  }

  @Override
  public String getStatusText() throws IOException {
    return this.response.getStatusText();
  }

  @Override
  public HttpHeaders getHeaders() {
    return this.response.getHeaders();
  }

  @Override
  public InputStream getBody() throws IOException {
    if (this.body == null) {
      this.body = StreamUtils.copyToByteArray(this.response.getBody());
    }
    return new ByteArrayInputStream(this.body);
  }

  @Override
  public void close() {
    this.response.close();
  }

}
