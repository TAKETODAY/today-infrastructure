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

import java.io.InputStream;
import java.io.Serial;

import infra.web.multipart.MultipartException;

/**
 * This exception is thrown, if an attempt is made to read data from the {@link InputStream}, which has been returned by
 * {@link FieldItemInput#getInputStream()}, after {@link java.util.Iterator#hasNext()} has been invoked on the iterator, which created the
 * {@link FieldItemInput}.
 */
public class ItemSkippedException extends MultipartException {

  @Serial
  private static final long serialVersionUID = 1;

  /**
   * Constructs an instance with a given detail message.
   *
   * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method)
   */
  ItemSkippedException(final String message) {
    super(message);
  }

}
