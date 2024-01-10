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

package cn.taketoday.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.StreamUtils;

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

  default boolean supportsZeroCopy() {
    return false;
  }

  /**
   * Use the given {@link File} to write the body of the message to the underlying
   * HTTP layer.
   *
   * @param file the file to transfer
   */
  default void sendFile(File file) {
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
  default void sendFile(File file, long position, long count) {
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
  default void sendFile(Path file, long position, long count) {
    try (InputStream inputStream = Files.newInputStream(file)) {
      StreamUtils.copyRange(inputStream, getBody(), position, count);
    }
    catch (IOException e) {
      throw ExceptionUtils.sneakyThrow(e);
    }
  }

}
