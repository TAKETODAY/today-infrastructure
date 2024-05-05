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
 * This exception is thrown if a request contains more files than the specified
 * limit.
 */
public class FileCountLimitExceededException extends FileUploadException {

  private static final long serialVersionUID = 2408766352570556046L;

  private final long limit;

  /**
   * Creates a new instance.
   *
   * @param message The detail message
   * @param limit The limit that was exceeded
   */
  public FileCountLimitExceededException(final String message, final long limit) {
    super(message);
    this.limit = limit;
  }

  /**
   * Retrieves the limit that was exceeded.
   *
   * @return The limit that was exceeded by the request
   */
  public long getLimit() {
    return limit;
  }
}
