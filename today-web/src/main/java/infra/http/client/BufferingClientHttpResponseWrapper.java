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
