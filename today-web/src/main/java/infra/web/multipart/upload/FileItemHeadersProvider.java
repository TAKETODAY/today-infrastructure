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

package infra.web.multipart.upload;

import infra.http.HttpHeaders;

/**
 * Provides access to headers.
 *
 * @param <T> The FileItemHeadersProvider type.
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see FileItem
 * @see FileItemInput
 * @since 5.0
 */
public interface FileItemHeadersProvider<T extends FileItemHeadersProvider<T>> {

  /**
   * Gets the collection of headers defined locally within this item.
   *
   * @return the {@link HttpHeaders} present for this item.
   */
  HttpHeaders getHeaders();

  /**
   * Sets the headers read from within an item. Implementations of {@link FileItem} or {@link FileItemInput} should implement this interface to be able to get
   * the raw headers found within the item header block.
   *
   * @param headers the instance that holds onto the headers for this instance.
   * @return {@code this} instance.
   */
  T setHeaders(HttpHeaders headers);

}
