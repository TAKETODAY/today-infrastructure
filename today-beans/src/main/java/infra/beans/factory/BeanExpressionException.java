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

package infra.beans.factory;

import infra.beans.FatalBeanException;

/**
 * Exception that indicates an expression evaluation attempt having failed.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/25 15:05
 */
public class BeanExpressionException extends FatalBeanException {

  /**
   * Create a new BeanExpressionException with the specified message.
   *
   * @param msg the detail message
   */
  public BeanExpressionException(String msg) {
    super(msg);
  }

  /**
   * Create a new BeanExpressionException with the specified message
   * and root cause.
   *
   * @param msg the detail message
   * @param cause the root cause
   */
  public BeanExpressionException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
