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
package cn.taketoday.web.mock.fileupload.util.mime;

/**
 * @since FileUpload 1.3
 */
final class ParseException extends Exception {

  /**
   * The UID to use when serializing this instance.
   */
  private static final long serialVersionUID = 5355281266579392077L;

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message.
   */
  ParseException(final String message) {
    super(message);
  }

}
