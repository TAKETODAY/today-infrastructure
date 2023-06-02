/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import cn.taketoday.expression.spel.support.StandardOperatorOverloader;
import cn.taketoday.lang.Nullable;

/**
 * By default the mathematical operators {@link Operation} support simple types
 * like numbers. By providing an implementation of OperatorOverloader, a user
 * of the expression language can support these operations on other types.
 *
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface OperatorOverloader {

  StandardOperatorOverloader STANDARD = new StandardOperatorOverloader();

  /**
   * Return true if the operator overloader supports the specified operation
   * between the two operands and so should be invoked to handle it.
   *
   * @param operation the operation to be performed
   * @param leftOperand the left operand
   * @param rightOperand the right operand
   * @return true if the OperatorOverloader supports the specified operation
   * between the two operands
   * @throws EvaluationException if there is a problem performing the operation
   */
  boolean overridesOperation(Operation operation, @Nullable Object leftOperand, @Nullable Object rightOperand)
          throws EvaluationException;

  /**
   * Execute the specified operation on two operands, returning a result.
   * See {@link Operation} for supported operations.
   *
   * @param operation the operation to be performed
   * @param leftOperand the left operand
   * @param rightOperand the right operand
   * @return the result of performing the operation on the two operands
   * @throws EvaluationException if there is a problem performing the operation
   */
  Object operate(Operation operation, @Nullable Object leftOperand, @Nullable Object rightOperand)
          throws EvaluationException;

}
