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

package infra.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import infra.core.AttributeAccessorSupport;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class MockHttpOutputMessage extends AttributeAccessorSupport implements HttpOutputMessage {

  private final HttpHeaders headers = HttpHeaders.forWritable();

  private final ByteArrayOutputStream body = new ByteArrayOutputStream();

  private boolean headersWritten = false;

  private final HttpHeaders writtenHeaders = HttpHeaders.forWritable();

  @Override
  public HttpHeaders getHeaders() {
    return (this.headersWritten ? headers.asReadOnly() : this.headers);
  }

  /**
   * Return a copy of the actual headers written at the time of the call to
   * getResponseBody, i.e. ignoring any further changes that may have been made to
   * the underlying headers, e.g. via a previously obtained instance.
   */
  public HttpHeaders getWrittenHeaders() {
    return writtenHeaders;
  }

  @Override
  public OutputStream getBody() throws IOException {
    writeHeaders();
    return body;
  }

  public byte[] getBodyAsBytes() {
    writeHeaders();
    return body.toByteArray();
  }

  /**
   * Return the body content interpreted as a UTF-8 string.
   */
  public String getBodyAsString() {
    return getBodyAsString(StandardCharsets.UTF_8);
  }

  public String getBodyAsString(Charset charset) {
    byte[] bytes = getBodyAsBytes();
    return new String(bytes, charset);
  }

  private void writeHeaders() {
    if (this.headersWritten) {
      return;
    }
    this.headersWritten = true;
    this.writtenHeaders.putAll(this.headers);
  }

}
