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

/**
 * Thrown to indicate that A files size exceeds the configured maximum.
 */
public class FileSizeLimitExceededException
        extends SizeException {

  /**
   * The exceptions UID, for serializing an instance.
   */
  private static final long serialVersionUID = 8150776562029630058L;

  /**
   * File name of the item, which caused the exception.
   */
  private String fileName;

  /**
   * Field name of the item, which caused the exception.
   */
  private String fieldName;

  /**
   * Constructs a {@code SizeExceededException} with
   * the specified detail message, and actual and permitted sizes.
   *
   * @param message The detail message.
   * @param actual The actual request size.
   * @param permitted The maximum permitted request size.
   */
  public FileSizeLimitExceededException(final String message, final long actual,
          final long permitted) {
    super(message, actual, permitted);
  }

  /**
   * Returns the file name of the item, which caused the
   * exception.
   *
   * @return File name, if known, or null.
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * Sets the file name of the item, which caused the
   * exception.
   *
   * @param pFileName the file name of the item, which caused the exception.
   */
  public void setFileName(final String pFileName) {
    fileName = pFileName;
  }

  /**
   * Returns the field name of the item, which caused the
   * exception.
   *
   * @return Field name, if known, or null.
   */
  public String getFieldName() {
    return fieldName;
  }

  /**
   * Sets the field name of the item, which caused the
   * exception.
   *
   * @param pFieldName the field name of the item,
   * which caused the exception.
   */
  public void setFieldName(final String pFieldName) {
    fieldName = pFieldName;
  }

}