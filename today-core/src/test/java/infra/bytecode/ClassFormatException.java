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

package infra.bytecode;

/**
 * A {@link RuntimeException} thrown by {@link ClassFile} when a class file is malformed.
 *
 * @author Eric Bruneton
 */
public class ClassFormatException extends RuntimeException {

  private static final long serialVersionUID = -6426141818319882225L;

  /**
   * Constructs a new ClassFormatException instance.
   *
   * @param message the detailed message of this exception.
   */
  public ClassFormatException(final String message) {
    super(message);
  }

  /**
   * Constructs a new ClassFormatException instance.
   *
   * @param message the detailed message of this exception.
   * @param cause the cause of this exception.
   */
  public ClassFormatException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
