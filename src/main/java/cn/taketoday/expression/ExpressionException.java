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
 * Super class for exceptions that can occur whilst processing expressions.
 *
 * @author Andy Clement
 * @author Phillip Webb
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ExpressionException extends RuntimeException {

  @Nullable
  protected final String expressionString;

  protected int position;  // -1 if not known; should be known in all reasonable cases

  /**
   * Construct a new expression exception.
   *
   * @param message a descriptive message
   */
  public ExpressionException(String message) {
    super(message);
    this.expressionString = null;
    this.position = 0;
  }

  /**
   * Construct a new expression exception.
   *
   * @param message a descriptive message
   * @param cause the underlying cause of this exception
   */
  public ExpressionException(String message, Throwable cause) {
    super(message, cause);
    this.expressionString = null;
    this.position = 0;
  }

  /**
   * Construct a new expression exception.
   *
   * @param expressionString the expression string
   * @param message a descriptive message
   */
  public ExpressionException(@Nullable String expressionString, String message) {
    super(message);
    this.expressionString = expressionString;
    this.position = -1;
  }

  /**
   * Construct a new expression exception.
   *
   * @param expressionString the expression string
   * @param position the position in the expression string where the problem occurred
   * @param message a descriptive message
   */
  public ExpressionException(@Nullable String expressionString, int position, String message) {
    super(message);
    this.expressionString = expressionString;
    this.position = position;
  }

  /**
   * Construct a new expression exception.
   *
   * @param position the position in the expression string where the problem occurred
   * @param message a descriptive message
   */
  public ExpressionException(int position, String message) {
    super(message);
    this.expressionString = null;
    this.position = position;
  }

  /**
   * Construct a new expression exception.
   *
   * @param position the position in the expression string where the problem occurred
   * @param message a descriptive message
   * @param cause the underlying cause of this exception
   */
  public ExpressionException(int position, String message, Throwable cause) {
    super(message, cause);
    this.expressionString = null;
    this.position = position;
  }

  /**
   * Return the expression string.
   */
  @Nullable
  public final String getExpressionString() {
    return this.expressionString;
  }

  /**
   * Return the position in the expression string where the problem occurred.
   */
  public final int getPosition() {
    return this.position;
  }

  /**
   * Return the exception message.
   * As of Spring 4.0, this method returns the same result as {@link #toDetailedString()}.
   *
   * @see #getSimpleMessage()
   * @see Throwable#getMessage()
   */
  @Override
  public String getMessage() {
    return toDetailedString();
  }

  /**
   * Return a detailed description of this exception, including the expression
   * String and position (if available) as well as the actual exception message.
   */
  public String toDetailedString() {
    if (this.expressionString != null) {
      StringBuilder output = new StringBuilder();
      output.append("Expression [");
      output.append(this.expressionString);
      output.append(']');
      if (this.position >= 0) {
        output.append(" @");
        output.append(this.position);
      }
      output.append(": ");
      output.append(getSimpleMessage());
      return output.toString();
    }
    else {
      return getSimpleMessage();
    }
  }

  /**
   * Return the exception simple message without including the expression
   * that caused the failure.
   *
   * @since 4.0
   */
  public String getSimpleMessage() {
    return super.getMessage();
  }

}
