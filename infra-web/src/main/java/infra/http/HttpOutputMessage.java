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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import infra.util.StreamUtils;

/**
 * Represents an HTTP output message, consisting of {@linkplain #getHeaders() headers}
 * and a writable {@linkplain #getBody() body}.
 *
 * <p>Typically implemented by an HTTP request handle on the client side,
 * or an HTTP response handle on the server side.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface HttpOutputMessage extends HttpMessage {

  /**
   * Return the body of the message as an output stream.
   *
   * @return the output stream body (never {@code null})
   * @throws IOException in case of I/O errors
   */
  OutputStream getBody() throws IOException;

  /**
   * Set the {@linkplain MediaType media type} of the body, as specified by the
   * {@code Content-Type} header.
   */
  default void setContentType(@Nullable MediaType mediaType) {
    getHeaders().setContentType(mediaType);
  }

  /**
   * Whether this message supports zero-copy file transfers.
   * <p>Default implementation returns {@code false}.
   *
   * @return {@code true} if this message supports zero-copy
   * file transfers, {@code false} otherwise
   */
  default boolean supportsZeroCopy() {
    return false;
  }

  /**
   * Use the given {@link File} to write the body of the message to the underlying
   * HTTP layer.
   *
   * @param file the file to transfer
   * @throws IOException in case of I/O errors
   */
  default void sendFile(File file) throws IOException {
    sendFile(file, 0, file.length());
  }

  /**
   * Use the given {@link File} to write the body of the message to the underlying
   * HTTP layer.
   *
   * @param file the file to transfer
   * @param position the position within the file from which the transfer is to begin
   * @param count the number of bytes to be transferred
   */
  default void sendFile(File file, long position, long count) throws IOException {
    sendFile(file.toPath(), position, count);
  }

  /**
   * Use the given {@link Path} to write the body of the message to the underlying
   * HTTP layer.
   *
   * @param file the file to transfer
   * @param position the position within the file from which the transfer is to begin
   * @param count the number of bytes to be transferred
   */
  default void sendFile(Path file, long position, long count) throws IOException {
    try (InputStream inputStream = Files.newInputStream(file)) {
      StreamUtils.copyRange(inputStream, getBody(), position, count);
    }
  }

}
