/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.http.client.ClientHttpResponseDecorator;
import cn.taketoday.lang.Nullable;

/**
 * Implementation of {@link ClientHttpResponse} that can not only check if
 * the response has a message body, but also if its length is 0 (i.e. empty)
 * by actually reading the input stream.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3.3">RFC 7230 Section 3.3.3</a>
 * @since 4.0
 */
class IntrospectingClientHttpResponse extends ClientHttpResponseDecorator implements ClientHttpResponse {

  @Nullable
  private PushbackInputStream pushbackInputStream;

  public IntrospectingClientHttpResponse(ClientHttpResponse response) {
    super(response);
  }

  /**
   * Indicates whether the response has a message body.
   * <p>Implementation returns {@code false} for:
   * <ul>
   * <li>a response status of {@code 1XX}, {@code 204} or {@code 304}</li>
   * <li>a {@code Content-Length} header of {@code 0}</li>
   * </ul>
   *
   * @return {@code true} if the response has a message body, {@code false} otherwise
   * @throws IOException in case of I/O errors
   */
  public boolean hasMessageBody() throws IOException {
    HttpStatusCode statusCode = getStatusCode();
    if (statusCode.is1xxInformational()
            || statusCode == HttpStatus.NO_CONTENT
            || statusCode == HttpStatus.NOT_MODIFIED) {
      return false;
    }
    return getHeaders().getContentLength() != 0;
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

  @Override
  public InputStream getBody() throws IOException {
    return this.pushbackInputStream != null ? this.pushbackInputStream : super.getBody();
  }

}
