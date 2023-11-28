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

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.StreamingHttpOutputMessage;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.FastByteArrayOutputStream;

/**
 * Abstract base for {@link ClientHttpRequest} that also implement
 * {@link StreamingHttpOutputMessage}. Ensures that headers and
 * body are not written multiple times.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/1 22:47
 */
abstract class AbstractStreamingClientHttpRequest
        extends AbstractClientHttpRequest implements StreamingHttpOutputMessage {

  @Nullable
  private Body body;

  @Nullable
  private FastByteArrayOutputStream bodyStream;

  @Override
  protected final OutputStream getBodyInternal(HttpHeaders headers) {
    Assert.state(this.body == null, "Invoke either getBody or setBody; not both");

    if (this.bodyStream == null) {
      this.bodyStream = new FastByteArrayOutputStream(1024);
    }
    return this.bodyStream;
  }

  @Override
  public final void setBody(Body body) {
    Assert.notNull(body, "Body is required");
    assertNotExecuted();
    Assert.state(this.bodyStream == null, "Invoke either getBody or setBody; not both");

    this.body = body;
  }

  @Override
  protected final ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
    if (this.body == null && this.bodyStream != null) {
      this.body = outputStream -> this.bodyStream.writeTo(outputStream);
    }
    return executeInternal(headers, this.body);
  }

  /**
   * Abstract template method that writes the given headers and content to the HTTP request.
   *
   * @param headers the HTTP headers
   * @param body the HTTP body, may be {@code null} if no body was {@linkplain #setBody(Body) set}
   * @return the response object for the executed request
   */
  protected abstract ClientHttpResponse executeInternal(HttpHeaders headers, @Nullable Body body) throws IOException;

}
