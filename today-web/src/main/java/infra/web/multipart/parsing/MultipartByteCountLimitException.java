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
 * Signals that a file size exceeds the configured maximum.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class MultipartByteCountLimitException extends MultipartSizeException {

  @Serial
  private static final long serialVersionUID = 1;

  /**
   * File name of the item, which caused the exception.
   */
  private final String fileName;

  /**
   * Field name of the item, which caused the exception.
   */
  private final String fieldName;

  /**
   * Constructs an instance with the specified detail message, and actual and permitted sizes.
   *
   * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method)
   * @param actual The actual request size.
   * @param permitted The maximum permitted request size.
   * @param fileName File name of the item, which caused the exception.
   * @param fieldName Field name of the item, which caused the exception.
   */
  public MultipartByteCountLimitException(final String message, final long actual, final long permitted, final String fileName, final String fieldName) {
    super(message, permitted, actual);
    this.fileName = fileName;
    this.fieldName = fieldName;
  }

  /**
   * Gets the field name of the item, which caused the exception.
   *
   * @return Field name, if known, or null.
   */
  public String getFieldName() {
    return fieldName;
  }

  /**
   * Gets the file name of the item, which caused the exception.
   *
   * @return File name, if known, or null.
   */
  public String getFileName() {
    return fileName;
  }

}
