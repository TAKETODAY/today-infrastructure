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

package infra.oxm;

/**
 * Exception thrown on marshalling validation failure.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ValidationFailureException extends XmlMappingException {

  /**
   * Construct a {@code ValidationFailureException} with the specified detail message.
   *
   * @param msg the detail message
   */
  public ValidationFailureException(String msg) {
    super(msg);
  }

  /**
   * Construct a {@code ValidationFailureException} with the specified detail message
   * and nested exception.
   *
   * @param msg the detail message
   * @param cause the nested exception
   */
  public ValidationFailureException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
