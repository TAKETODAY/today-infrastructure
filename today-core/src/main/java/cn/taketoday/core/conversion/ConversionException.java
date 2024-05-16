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

package cn.taketoday.core.conversion;

import cn.taketoday.core.NestedRuntimeException;

/**
 * Base class for exceptions thrown by the conversion system.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-06-28 17:05:34
 */
public abstract class ConversionException extends NestedRuntimeException {

  /**
   * Construct a new conversion exception.
   *
   * @param message the exception message
   */
  public ConversionException(String message) {
    super(message);
  }

  /**
   * Construct a new conversion exception.
   *
   * @param message the exception message
   * @param cause the cause
   */
  public ConversionException(String message, Throwable cause) {
    super(message, cause);
  }

}
