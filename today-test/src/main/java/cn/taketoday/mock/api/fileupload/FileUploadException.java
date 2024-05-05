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

/**
 * Exception for errors encountered while processing the request.
 */
public class FileUploadException extends IOException {

  private static final long serialVersionUID = -4222909057964038517L;

  /**
   * Constructs a new {@code FileUploadException} without message.
   */
  public FileUploadException() {
    super();
  }

  /**
   * Constructs a new {@code FileUploadException} with specified detail
   * message.
   *
   * @param msg the error message.
   */
  public FileUploadException(final String msg) {
    super(msg);
  }

  /**
   * Creates a new {@code FileUploadException} with the given
   * detail message and cause.
   *
   * @param msg The exceptions detail message.
   * @param cause The exceptions cause.
   */
  public FileUploadException(final String msg, final Throwable cause) {
    super(msg, cause);
  }
}
