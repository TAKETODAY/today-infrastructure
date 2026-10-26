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

package infra.http;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import infra.util.StreamUtils;

/**
 * Represents an HTTP input message, consisting of {@linkplain #getHeaders() headers}
 * and a readable {@linkplain #getBody() body}.
 *
 * <p>Typically implemented by an HTTP request handle on the server side,
 * or an HTTP response handle on the client side.
 *
 * <p>Content access methods may consume the underlying stream; repeated reads
 * are not guaranteed. Their default implementations read the remaining body
 * without closing the stream. The message owner is responsible for resource
 * cleanup, including when reading fails. Implementations that open independent
 * streams must manage those streams themselves.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface HttpInputMessage extends HttpMessage {

  /**
   * Return the body of the message as an input stream.
   *
   * @return the input stream body (never {@code null})
   * @throws IOException in case of I/O errors
   */
  InputStream getBody() throws IOException;

  /**
   * Read the remaining message body as a byte array.
   * <p>The default implementation consumes the body without closing the stream.
   *
   * @return the remaining body content, or an empty array if no content remains
   * @throws IOException if reading the message body fails
   * @since 5.0
   */
  default byte[] getContentAsByteArray() throws IOException {
    return StreamUtils.copyToByteArray(getBody());
  }

  /**
   * Read the remaining message body as a UTF-8 string.
   * <p>The default implementation consumes the body without closing the stream.
   *
   * @return the remaining body content, or an empty string if no content remains
   * @throws IOException if reading the message body fails
   * @since 5.0
   */
  default String getContentAsString() throws IOException {
    return getContentAsString(StandardCharsets.UTF_8);
  }

  /**
   * Read the remaining message body as a string using the given charset,
   * regardless of the charset declared in the message headers.
   * <p>The default implementation consumes the body without closing the stream.
   *
   * @param charset the charset to use to decode the message body,
   * or {@code null} to use UTF-8 in the default implementation
   * @return the remaining body content, or an empty string if no content remains
   * @throws IOException if reading the message body fails
   * @since 5.0
   */
  default String getContentAsString(@Nullable Charset charset) throws IOException {
    return StreamUtils.copyToString(getBody(), charset != null ? charset : StandardCharsets.UTF_8);
  }

}
