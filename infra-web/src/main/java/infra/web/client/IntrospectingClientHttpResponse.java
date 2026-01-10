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

package infra.web.client;

import org.jspecify.annotations.Nullable;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.client.ClientHttpResponse;
import infra.http.client.ClientHttpResponseDecorator;

/**
 * Implementation of {@link ClientHttpResponse} that can not only check if
 * the response has a message body, but also if its length is 0 (i.e. empty)
 * by actually reading the input stream.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class IntrospectingClientHttpResponse extends ClientHttpResponseDecorator implements ClientHttpResponse {

  @Nullable
  private PushbackInputStream pushbackInputStream;

  public IntrospectingClientHttpResponse(ClientHttpResponse response) {
    super(response);
  }

  /**
   * Indicates whether the response might have a message body.
   * <p>Implementation returns {@code false} for:
   * <ul>
   * <li>a response status of {@code 1XX}, {@code 204} or {@code 304}</li>
   * <li>a {@code Content-Length} header of {@code 0}</li>
   * </ul>
   * <p>In other cases, the server could use a {@code Transfer-Encoding} header or just
   * write the body and close the response. Reading the message body is then the only way
   * to check for the presence of a body.
   *
   * @return {@code true} if the response might have a message body, {@code false} otherwise
   * @throws IOException in case of I/O errors
   * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3.3">RFC 7230 Section 3.3.3</a>
   */
  public boolean hasMessageBody() throws IOException {
    HttpStatusCode statusCode = getStatusCode();
    if (statusCode.is1xxInformational()
            || statusCode == HttpStatus.NO_CONTENT
            || statusCode == HttpStatus.NOT_MODIFIED) {
      return false;
    }
    return getContentLength() != 0;
  }

  /**
   * Indicates whether the response has an empty message body.
   * <p>Implementation tries to read the first bytes of the response stream:
   * <ul>
   * <li>if no bytes are available, the message body is empty</li>
   * <li>otherwise it is not empty and the stream is reset to its start for further reading</li>
   * </ul>
   *
   * @return {@code true} if the response has a zero-length message body, {@code false} otherwise
   * @throws IOException in case of I/O errors
   */
  public boolean hasEmptyMessageBody() throws IOException {
    InputStream body = delegate.getBody();
    // Per contract body shouldn't be null, but check anyway..
    if (body == null) {
      return true;
    }
    try {
      if (body.markSupported()) {
        body.mark(1);
        if (body.read() == -1) {
          return true;
        }
        else {
          body.reset();
          return false;
        }
      }
      else {
        this.pushbackInputStream = new PushbackInputStream(body);
        int b = this.pushbackInputStream.read();
        if (b == -1) {
          return true;
        }
        else {
          this.pushbackInputStream.unread(b);
          return false;
        }
      }
    }
    catch (EOFException exc) {
      return true;
    }
  }

  @Override
  public InputStream getBody() throws IOException {
    return this.pushbackInputStream != null ? this.pushbackInputStream : delegate.getBody();
  }

}
