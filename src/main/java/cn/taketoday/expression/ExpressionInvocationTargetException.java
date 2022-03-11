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

/**
 * This exception wraps (as cause) a checked exception thrown by some method that SpEL
 * invokes. It differs from a SpelEvaluationException because this indicates the
 * occurrence of a checked exception that the invoked method was defined to throw.
 * SpelEvaluationExceptions are for handling (and wrapping) unexpected exceptions.
 *
 * @author Andy Clement
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ExpressionInvocationTargetException extends EvaluationException {

  public ExpressionInvocationTargetException(int position, String message, Throwable cause) {
    super(position, message, cause);
  }

  public ExpressionInvocationTargetException(int position, String message) {
    super(position, message);
  }

  public ExpressionInvocationTargetException(String expressionString, String message) {
    super(expressionString, message);
  }

  public ExpressionInvocationTargetException(String message, Throwable cause) {
    super(message, cause);
  }

  public ExpressionInvocationTargetException(String message) {
    super(message);
  }

}
