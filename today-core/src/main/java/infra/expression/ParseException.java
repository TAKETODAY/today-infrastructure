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

package infra.expression;

import infra.lang.Nullable;

/**
 * Represent an exception that occurs during expression parsing.
 *
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ParseException extends ExpressionException {

  /**
   * Create a new expression parsing exception.
   *
   * @param expressionString the expression string that could not be parsed
   * @param position the position in the expression string where the problem occurred
   * @param message description of the problem that occurred
   */
  public ParseException(@Nullable String expressionString, int position, String message) {
    super(expressionString, position, message);
  }

  /**
   * Create a new expression parsing exception.
   *
   * @param position the position in the expression string where the problem occurred
   * @param message description of the problem that occurred
   * @param cause the underlying cause of this exception
   */
  public ParseException(int position, String message,@Nullable Throwable cause) {
    super(position, message, cause);
  }

}
