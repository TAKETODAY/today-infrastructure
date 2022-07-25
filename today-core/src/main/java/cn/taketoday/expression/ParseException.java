/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.expression;

import cn.taketoday.lang.Nullable;

/**
 * Represent an exception that occurs during expression parsing.
 *
 * @author Andy Clement
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
  public ParseException(int position, String message, Throwable cause) {
    super(position, message, cause);
  }

  /**
   * Create a new expression parsing exception.
   *
   * @param position the position in the expression string where the problem occurred
   * @param message description of the problem that occurred
   */
  public ParseException(int position, String message) {
    super(position, message);
  }

}
