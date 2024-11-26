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

package infra.expression.spel.support;

import infra.expression.EvaluationException;
import infra.expression.Operation;
import infra.expression.OperatorOverloader;
import infra.lang.Nullable;

/**
 * Standard implementation of {@link OperatorOverloader}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class StandardOperatorOverloader implements OperatorOverloader {

  @Override
  public boolean overridesOperation(Operation operation,
          @Nullable Object leftOperand, @Nullable Object rightOperand) throws EvaluationException {

    return false;
  }

  @Override
  public Object operate(Operation operation,
          @Nullable Object leftOperand, @Nullable Object rightOperand) throws EvaluationException {

    throw new EvaluationException("No operation overloaded by default");
  }

}
