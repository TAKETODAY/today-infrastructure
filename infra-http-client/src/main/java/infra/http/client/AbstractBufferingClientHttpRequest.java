/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
        request.setContentLength(bufferedOutput.length);
      }
      StreamingHttpOutputMessage.writeBody(request, bufferedOutput);
    }

    ClientHttpResponse response = request.execute();
    return bufferResponse ? new BufferingClientHttpResponseWrapper(response) : response;
  }

}
