/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.http.client;

import java.io.IOException;
import java.io.OutputStream;

import infra.http.HttpHeaders;
import infra.http.StreamingHttpOutputMessage;
import infra.util.FastByteArrayOutputStream;

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
    if (headers.getContentLength() < 0) {
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

  /**
   * Execute with the given request and body.
   *
   * @param request the request to execute with
   * @param bufferedOutput the body to write
   * @param bufferResponse whether to buffer the response
   * @return the resulting response
   * @throws IOException in case of I/O errors from execution
   * @since 5.0
   */
  protected ClientHttpResponse executeWithRequest(ClientHttpRequest request, byte[] bufferedOutput, boolean bufferResponse)
          throws IOException //
  {
    if (bufferedOutput.length > 0) {
      long contentLength = request.getContentLength();
      if (contentLength > -1 && contentLength != bufferedOutput.length) {
        request.getHeaders().setContentLength(bufferedOutput.length);
      }
      StreamingHttpOutputMessage.writeBody(request, bufferedOutput);
    }

    ClientHttpResponse response = request.execute();
    return bufferResponse ? new BufferingClientHttpResponseWrapper(response) : response;
  }

}
