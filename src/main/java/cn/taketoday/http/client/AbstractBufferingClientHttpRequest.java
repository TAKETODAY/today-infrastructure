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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import cn.taketoday.http.HttpHeaders;

/**
 * Base implementation of {@link ClientHttpRequest} that buffers output
 * in a byte array before sending it over the wire.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public abstract class AbstractBufferingClientHttpRequest extends AbstractClientHttpRequest {

  private ByteArrayOutputStream bufferedOutput = new ByteArrayOutputStream(1024);

  @Override
  protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
    return this.bufferedOutput;
  }

  @Override
  protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
    byte[] bytes = this.bufferedOutput.toByteArray();
    if (headers.getContentLength() < 0) {
      headers.setContentLength(bytes.length);
    }
    ClientHttpResponse result = executeInternal(headers, bytes);
    this.bufferedOutput = new ByteArrayOutputStream(0);
    return result;
  }

  /**
   * Abstract template method that writes the given headers and content to the HTTP request.
   *
   * @param headers the HTTP headers
   * @param bufferedOutput the body content
   * @return the response object for the executed request
   */
  protected abstract ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput)
          throws IOException;

}
