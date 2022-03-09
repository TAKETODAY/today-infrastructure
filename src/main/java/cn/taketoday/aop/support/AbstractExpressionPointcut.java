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

package cn.taketoday.aop.support;

import java.io.Serializable;

import cn.taketoday.lang.Nullable;

/**
 * Abstract superclass for expression pointcuts,
 * offering location and expression properties.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setLocation
 * @see #setExpression
 * @since 4.0 2022/3/9 11:55
 */
@SuppressWarnings("serial")
public abstract class AbstractExpressionPointcut implements ExpressionPointcut, Serializable {

  @Nullable
  private String location;

  @Nullable
  private String expression;

  /**
   * Set the location for debugging.
   */
  public void setLocation(@Nullable String location) {
    this.location = location;
  }

  /**
   * Return location information about the pointcut expression
   * if available. This is useful in debugging.
   *
   * @return location information as a human-readable String,
   * or {@code null} if none is available
   */
  @Nullable
  public String getLocation() {
    return this.location;
  }

  public void setExpression(@Nullable String expression) {
    this.expression = expression;
    try {
      onSetExpression(expression);
    }
    catch (IllegalArgumentException ex) {
      // Fill in location information if possible.
      if (this.location != null) {
        throw new IllegalArgumentException("Invalid expression at location [" + this.location + "]: " + ex);
      }
      else {
        throw ex;
      }
    }
  }

  /**
   * Called when a new pointcut expression is set.
   * The expression should be parsed at this point if possible.
   * <p>This implementation is empty.
   *
   * @param expression the expression to set
   * @throws IllegalArgumentException if the expression is invalid
   * @see #setExpression
   */
  protected void onSetExpression(@Nullable String expression) throws IllegalArgumentException {

  }

  /**
   * Return this pointcut's expression.
   */
  @Override
  @Nullable
  public String getExpression() {
    return this.expression;
  }

}
