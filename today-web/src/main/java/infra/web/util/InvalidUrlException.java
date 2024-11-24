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

package infra.web.util;

import java.io.Serial;

/**
 * Thrown when a URL string cannot be parsed.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class InvalidUrlException extends IllegalArgumentException {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Construct a {@code InvalidUrlException} with the specified detail message.
   *
   * @param msg the detail message
   */
  public InvalidUrlException(String msg) {
    super(msg);
  }

  /**
   * Construct a {@code InvalidUrlException} with the specified detail message
   * and nested exception.
   *
   * @param msg the detail message
   * @param cause the nested exception
   */
  public InvalidUrlException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
