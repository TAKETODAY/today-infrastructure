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

package cn.taketoday.web.multipart;

import java.io.IOException;

import cn.taketoday.http.HttpHeaders;

/**
 * Representation for a part in a "multipart/form-data" request.
 *
 * <p>The origin of a multipart request may be a browser form in which case each
 * part is either a {@link FormData} or a {@link MultipartFile}.
 *
 * <p>Multipart requests may also be used outside of a browser for data of any
 * content type (e.g. JSON, PDF, etc).
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://tools.ietf.org/html/rfc7578">RFC 7578 (multipart/form-data)</a>
 * @see <a href="https://tools.ietf.org/html/rfc2183">RFC 2183 (Content-Disposition)</a>
 * @see <a href="https://www.w3.org/TR/html5/forms.html#multipart-form-data">HTML5 (multipart forms)</a>
 * @since 4.0 2022/4/28 22:04
 */
public interface Multipart {

  /**
   * Gets the name of this part.
   *
   * @return The name of this part as a {@code String}
   */
  String getName();

  /**
   * Return the form field value.
   */
  String getValue();

  /**
   * Determines whether or not a {@code Multipart} instance represents
   * a simple form field.
   *
   * @return {@code true} if the instance represents a simple form
   * field; {@code false} if it represents an uploaded file.
   */
  boolean isFormField();

  /**
   * Return the headers for the specified part of the multipart request.
   * <p>If the underlying implementation supports access to part headers,
   * then all headers are returned. Otherwise, e.g. for a file upload, the
   * returned headers may expose a 'Content-Type' if available.
   */
  HttpHeaders getHeaders();

  /**
   * Deletes the underlying storage for a file item, including deleting any
   * associated temporary disk file.
   *
   * @throws IOException if an error occurs.
   */
  void delete() throws IOException;

}
