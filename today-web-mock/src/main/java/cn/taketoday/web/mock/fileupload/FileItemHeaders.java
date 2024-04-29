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
package cn.taketoday.web.mock.fileupload;

import java.util.Iterator;

/**
 * <p> This class provides support for accessing the headers for a file or form
 * item that was received within a {@code multipart/form-data} POST
 * request.</p>
 *
 * @since FileUpload 1.2.1
 */
public interface FileItemHeaders {

  /**
   * Returns the value of the specified part header as a {@code String}.
   *
   * If the part did not include a header of the specified name, this method
   * return {@code null}.  If there are multiple headers with the same
   * name, this method returns the first header in the item.  The header
   * name is case insensitive.
   *
   * @param name a {@code String} specifying the header name
   * @return a {@code String} containing the value of the requested
   * header, or {@code null} if the item does not have a header
   * of that name
   */
  String getHeader(String name);

  /**
   * <p>
   * Returns all the values of the specified item header as an
   * {@code Iterator} of {@code String} objects.
   * </p>
   * <p>
   * If the item did not include any headers of the specified name, this
   * method returns an empty {@code Iterator}. The header name is
   * case insensitive.
   * </p>
   *
   * @param name a {@code String} specifying the header name
   * @return an {@code Iterator} containing the values of the
   * requested header. If the item does not have any headers of
   * that name, return an empty {@code Iterator}
   */
  Iterator<String> getHeaders(String name);

  /**
   * <p>
   * Returns an {@code Iterator} of all the header names.
   * </p>
   *
   * @return an {@code Iterator} containing all of the names of
   * headers provided with this file item. If the item does not have
   * any headers return an empty {@code Iterator}
   */
  Iterator<String> getHeaderNames();

}
