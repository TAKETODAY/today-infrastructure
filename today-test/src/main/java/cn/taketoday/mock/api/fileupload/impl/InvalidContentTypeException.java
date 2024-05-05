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
package cn.taketoday.mock.api.fileupload.impl;

import cn.taketoday.mock.api.fileupload.FileUploadException;

/**
 * Thrown to indicate that the request is not a multipart request.
 */
public class InvalidContentTypeException
        extends FileUploadException {

  /**
   * The exceptions UID, for serializing an instance.
   */
  private static final long serialVersionUID = -9073026332015646668L;

  /**
   * Constructs a {@code InvalidContentTypeException} with no
   * detail message.
   */
  public InvalidContentTypeException() {
  }

  /**
   * Constructs an {@code InvalidContentTypeException} with
   * the specified detail message.
   *
   * @param message The detail message.
   */
  public InvalidContentTypeException(final String message) {
    super(message);
  }

  /**
   * Constructs an {@code InvalidContentTypeException} with
   * the specified detail message and cause.
   *
   * @param msg The detail message.
   * @param cause the original cause
   * @since FileUpload 1.3.1
   */
  public InvalidContentTypeException(final String msg, final Throwable cause) {
    super(msg, cause);
  }
}