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
package cn.taketoday.web.mock.fileupload.impl;

import java.io.IOException;

import cn.taketoday.web.mock.fileupload.FileUploadException;

/**
 * This exception is thrown for hiding an inner
 * {@link FileUploadException} in an {@link IOException}.
 */
public class FileUploadIOException extends IOException {

  /**
   * The exceptions UID, for serializing an instance.
   */
  private static final long serialVersionUID = -7047616958165584154L;

  /**
   * The exceptions cause; we overwrite the parent
   * classes field, which is available since Java
   * 1.4 only.
   */
  private final FileUploadException cause;

  /**
   * Creates a {@code FileUploadIOException} with the
   * given cause.
   *
   * @param pCause The exceptions cause, if any, or null.
   */
  public FileUploadIOException(final FileUploadException pCause) {
    // We're not doing super(pCause) cause of 1.3 compatibility.
    cause = pCause;
  }

  /**
   * Returns the exceptions cause.
   *
   * @return The exceptions cause, if any, or null.
   */
  @SuppressWarnings("sync-override") // Field is final
  @Override
  public Throwable getCause() {
    return cause;
  }

}