/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.core.type.classreading;

import java.io.IOException;

/**
 * Exception that indicates an incompatible class format encountered
 * in a class file during metadata reading.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MetadataReaderFactory#getMetadataReader(Resource)
 * @see ClassFormatError
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ClassFormatException extends IOException {

  /**
   * Construct a new {@code ClassFormatException} with the
   * supplied message.
   *
   * @param message the detail message
   */
  public ClassFormatException(String message) {
    super(message);
  }

  /**
   * Construct a new {@code ClassFormatException} with the
   * supplied message and cause.
   *
   * @param message the detail message
   * @param cause the root cause
   */
  public ClassFormatException(String message, Throwable cause) {
    super(message, cause);
  }

}
