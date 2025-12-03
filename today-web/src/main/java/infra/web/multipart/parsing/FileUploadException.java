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

/**
 * Signals errors encountered while processing the request.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class FileUploadException extends RuntimeException {

  /**
   * Serial version UID, being used, if the exception is serialized.
   */
  @Serial
  private static final long serialVersionUID = 2;

  /**
   * Constructs an instance with a given detail message.
   *
   * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method)
   */
  public FileUploadException(final String message) {
    super(message);
  }

  /**
   * Constructs an instance with the given detail message and cause.
   *
   * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method)
   * @param cause The cause (which is saved for later retrieval by the {@link #getCause()} method). (A null value is permitted, and indicates that the cause
   * is nonexistent or unknown.)
   */
  public FileUploadException(final String message, final Throwable cause) {
    super(message, cause);
  }

}
