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
import java.io.OutputStream;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.util.FastByteArrayOutputStream;

/**
 * Base implementation of {@link ClientHttpRequest} that buffers output
 * in a byte array before sending it over the wire.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractBufferingClientHttpRequest extends AbstractClientHttpRequest {

  private final FastByteArrayOutputStream bufferedOutput = new FastByteArrayOutputStream(1024);

  @Override
  protected OutputStream getBodyInternal(HttpHeaders headers) {
    return this.bufferedOutput;
  }

  @Override
  protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
    byte[] bytes = bufferedOutput.toByteArrayUnsafe();
    if (bytes.length > 0 && headers.getContentLength() < 0) {
      headers.setContentLength(bytes.length);
    }
    ClientHttpResponse result = executeInternal(headers, bytes);
    bufferedOutput.reset();
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
