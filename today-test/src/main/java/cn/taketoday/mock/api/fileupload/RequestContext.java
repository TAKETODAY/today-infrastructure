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
package cn.taketoday.mock.api.fileupload;

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>Abstracts access to the request information needed for file uploads. This
 * interface should be implemented for each type of request that may be
 * handled by FileUpload, such as servlets and portlets.</p>
 *
 * @since FileUpload 1.1
 */
public interface RequestContext {

  /**
   * Retrieve the character encoding for the request.
   *
   * @return The character encoding for the request.
   */
  String getCharacterEncoding();

  /**
   * Retrieve the content type of the request.
   *
   * @return The content type of the request.
   */
  String getContentType();

  /**
   * Retrieve the input stream for the request.
   *
   * @return The input stream for the request.
   * @throws IOException if a problem occurs.
   */
  InputStream getInputStream() throws IOException;

}
