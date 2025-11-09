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

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import infra.util.StreamUtils;

/**
 * Simple implementation of {@link ClientHttpResponse} that reads the response's body
 * into memory, thus allowing for multiple invocations of {@link #getBody()}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class BufferingClientHttpResponseWrapper extends ClientHttpResponseDecorator {

  private volatile byte @Nullable [] body;

  BufferingClientHttpResponseWrapper(ClientHttpResponse response) {
    super(response);
  }

  @Override
  public InputStream getBody() throws IOException {
    byte[] body = this.body;
    if (body == null) {
      synchronized(this) {
        body = this.body;
        if (body == null) {
          body = StreamUtils.copyToByteArray(delegate.getBody());
          this.body = body;
        }
      }
    }
    return new ByteArrayInputStream(body);
  }

}
