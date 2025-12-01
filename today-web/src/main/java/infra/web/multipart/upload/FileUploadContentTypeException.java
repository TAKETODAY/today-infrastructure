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

/**
 * Signals that a request is not a multipart request.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class FileUploadContentTypeException extends FileUploadException {

  /**
   * The exceptions UID, for serializing an instance.
   */
  private static final long serialVersionUID = 2;

  /**
   * The guilty content type.
   */
  private String contentType;

  /**
   * Constructs an instance with the specified detail message.
   *
   * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method)
   * @param contentType The guilty content type.
   */
  public FileUploadContentTypeException(final String message, final String contentType) {
    super(message);
    this.contentType = contentType;
  }

  /**
   * Constructs an instance with the specified detail message and cause.
   *
   * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method)
   * @param cause the original cause
   */
  public FileUploadContentTypeException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Gets the content type.
   *
   * @return the content type.
   */
  public String getContentType() {
    return contentType;
  }
}
