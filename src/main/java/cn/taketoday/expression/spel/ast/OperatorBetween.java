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

package cn.taketoday.expression.spel.ast;

import java.util.List;

import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypeComparator;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.expression.spel.support.BooleanTypedValue;

/**
 * Represents the between operator. The left operand to between must be a single value and
 * the right operand must be a list - this operator returns true if the left operand is
 * between (using the registered comparator) the two elements in the list. The definition
 * of between being inclusive follows the SQL BETWEEN definition.
 *
 * @author Andy Clement
 * @since 4.0
 */
public class OperatorBetween extends Operator {

  public OperatorBetween(int startPos, int endPos, SpelNodeImpl... operands) {
    super("between", startPos, endPos, operands);
  }

  /**
   * Returns a boolean based on whether a value is in the range expressed. The first
   * operand is any value whilst the second is a list of two values - those two values
   * being the bounds allowed for the first operand (inclusive).
   *
   * @param state the expression state
   * @return true if the left operand is in the range specified, false otherwise
   * @throws EvaluationException if there is a problem evaluating the expression
   */
  @Override
  public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    Object left = getLeftOperand().getValueInternal(state).getValue();
    Object right = getRightOperand().getValueInternal(state).getValue();
    if (!(right instanceof List) || ((List<?>) right).size() != 2) {
      throw new SpelEvaluationException(getRightOperand().getStartPosition(),
              SpelMessage.BETWEEN_RIGHT_OPERAND_MUST_BE_TWO_ELEMENT_LIST);
    }

    List<?> list = (List<?>) right;
    Object low = list.get(0);
    Object high = list.get(1);
    TypeComparator comp = state.getTypeComparator();
    try {
      return BooleanTypedValue.forValue(comp.compare(left, low) >= 0 && comp.compare(left, high) <= 0);
    }
    catch (SpelEvaluationException ex) {
      ex.setPosition(getStartPosition());
      throw ex;
    }
  }

}
