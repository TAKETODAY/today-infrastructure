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
   * Sets the length of the content body in the response , this method sets the
   * HTTP Content-Length header.
   *
   * @param length a long specifying the length of the content being returned to the
   * client; sets the Content-Length header
   * @since 5.0
   */
  default void setContentLength(long length) {
    getHeaders().setContentLength(length);
  }

  /**
   * Sets a response header with the given name and value. If the
   * header had already been set, the new value overwrites the
   * previous one.
   *
   * @param name the name of the header
   * @param value the header value If it contains octet string,
   * it should be encoded according to RFC 2047
   * (<a href="http://www.ietf.org/rfc/rfc2047.txt">RFC 2047</a>)
   * @see HttpHeaders#setOrRemove
   * @since 5.0
   */
  default void setHeader(String name, @Nullable String value) {
    getHeaders().setOrRemove(name, value);
  }

  /**
   * merge headers to response http-headers
   *
   * @since 5.0
   */
  default void setHeaders(@Nullable HttpHeaders headers) {
    getHeaders().setAll(headers);
  }

  /**
   * Add a response header with the given name and value.
   *
   * @param name the name of the header
   * @param value the header value If it contains octet string,
   * it should be encoded according to RFC 2047
   * (<a href="http://www.ietf.org/rfc/rfc2047.txt">RFC 2047</a>)
   * @see HttpHeaders#add(String, String)
   * @since 5.0
   */
  default void addHeader(String name, @Nullable String value) {
    getHeaders().add(name, value);
  }

  /**
   * merge headers to response http-headers
   *
   * @since 5.0
   */
  default void addHeaders(@Nullable HttpHeaders headers) {
    getHeaders().addAll(headers);
  }

  /**
   * Removes the header with the specified name from the response headers.
   *
   * @param name the name of the header to be removed
   * @since 5.0
   */
  default boolean removeHeader(String name) {
    return getHeaders().remove(name) != null;
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
