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

package infra.web.multipart.parsing;

import java.io.Serial;

import infra.web.multipart.MultipartException;

/**
 * Signals that a requests permitted size is exceeded.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class MultipartSizeException extends MultipartException {

  @Serial
  private static final long serialVersionUID = 1;

  /**
   * The actual size of the request.
   */
  private final long actual;

  /**
   * The maximum permitted size of the request.
   */
  private final long permitted;

  /**
   * Constructs an instance.
   *
   * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method)
   * @param permitted The requests size limit.
   * @param actual The actual values for the request.
   */
  public MultipartSizeException(final String message, final long permitted, final long actual) {
    super(message);
    this.permitted = permitted;
    this.actual = actual;
  }

  /**
   * Gets the actual size of the request.
   *
   * @return The actual size of the request.
   */
  public long getActualSize() {
    return actual;
  }

  /**
   * Gets the limit size of the request.
   *
   * @return The limit size of the request.
   */
  public long getPermitted() {
    return permitted;
  }

}
