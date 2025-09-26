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

package infra.http.converter;

import org.jspecify.annotations.Nullable;

/**
 * Thrown by {@link HttpMessageConverter} implementations when the
 * {@link HttpMessageConverter#write} method fails.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class HttpMessageNotWritableException extends HttpMessageConversionException {

  /**
   * Create a new HttpMessageNotWritableException.
   *
   * @param msg the detail message
   */
  public HttpMessageNotWritableException(String msg) {
    super(msg);
  }

  /**
   * Create a new HttpMessageNotWritableException.
   *
   * @param msg the detail message
   * @param cause the root cause (if any)
   */
  public HttpMessageNotWritableException(String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

}
