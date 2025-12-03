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

package infra.web.multipart;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import infra.core.io.InputStreamSource;
import infra.http.HttpHeaders;

/**
 * Representation for a part in a "multipart/form-data" request.
 *
 * <p>The origin of a multipart request may be a browser form in which case each
 * part is either a {@code FormField} or a {@code MultipartFile}.
 *
 * <p>Multipart requests may also be used outside a browser for data of any
 * content type (e.g. JSON, PDF, etc).
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://tools.ietf.org/html/rfc7578">RFC 7578 (multipart/form-data)</a>
 * @see <a href="https://tools.ietf.org/html/rfc2183">RFC 2183 (Content-Disposition)</a>
 * @see <a href="https://www.w3.org/TR/html5/forms.html#multipart-form-data">HTML5 (multipart forms)</a>
 * @since 4.0 2022/4/28 22:04
 */
public interface Part extends InputStreamSource {

  /**
   * Gets the name of this part.
   *
   * @return The name of this part as a {@code String}
   */
  String getName();

  /**
   * Return the headers for the specified part of the multipart request.
   * <p>If the underlying implementation supports access to part headers,
   * then all headers are returned. Otherwise, e.g. for a file upload, the
   * returned headers may expose a 'Content-Type' if available.
   */
  HttpHeaders getHeaders();

  /**
   * Return the content type of the part.
   *
   * @return the content type, or {@code null} if not defined
   * (or no part has been chosen in the multipart form)
   */
  @Nullable
  default String getContentType() {
    return null;
  }

  /**
   * Determine the content length for this Part.
   */
  long getContentLength();

  /**
   * Returns the contents of this part as an array of bytes.
   * <p>
   * Note: this method will allocate a lot of memory,
   * if the data is currently stored on the file system.
   *
   * @return the contents of this part as an array of bytes.
   */
  byte[] getContentAsByteArray() throws IOException;

  /**
   * Returns the contents of this part as a string, using the specified
   * charset.
   *
   * @param charset the charset to use for decoding
   * @return the contents of this resource as a {@code String}
   * @throws OutOfMemoryError See {@link Files#readAllBytes(Path)}: If a string of the required size cannot be allocated,
   * for example the file is larger than {@code 2GB}. If so, you should use {@link #getReader()}.
   * @throws IOException if an I/O error occurs
   * @since 5.0
   */
  String getContentAsString(Charset charset) throws IOException;

  /**
   * Returns the contents of this part as a string, using the UTF-8.
   *
   * @return the contents of this resource as a {@code String}
   * @throws OutOfMemoryError See {@link Files#readAllBytes(Path)}: If a string of the required size cannot be allocated,
   * for example the file is larger than {@code 2GB}. If so, you should use {@link #getReader()}.
   * @since 5.0
   */
  String getContentAsString();

  /**
   * Determines whether or not a {@code Multipart} instance represents
   * a simple form field.
   *
   * @return {@code true} if the instance represents a simple form
   * field; {@code false} if it represents an uploaded file.
   */
  boolean isFormField();

  /**
   * Determine whether this part represents a file part.
   *
   * @since 4.0
   */
  boolean isFile();

  /**
   * Deletes the underlying storage for a file item, including deleting any
   * associated temporary disk file.
   *
   * @throws IOException if an error occurs.
   */
  void cleanup() throws IOException;

}
