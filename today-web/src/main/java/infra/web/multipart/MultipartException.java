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

import infra.http.converter.HttpMessageNotReadableException;

/**
 * Multipart cannot be parsed include
 * {@link infra.web.multipart.MultipartFile} and normal part
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2021/1/17 10:41
 */
public class MultipartException extends HttpMessageNotReadableException {

  /**
   * Constructor for MultipartException.
   *
   * @param message the detail message
   */
  public MultipartException(@Nullable String message) {
    super(message, null, null);
  }

  /**
   * Constructor for MultipartException.
   *
   * @param message the detail message
   * @param cause the root cause from the multipart parsing API in use
   */
  public MultipartException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause, null);
  }
}
