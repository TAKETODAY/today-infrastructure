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

package cn.taketoday.expression.spel.ast;

import java.util.List;

import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypeComparator;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.expression.spel.support.BooleanTypedValue;

/**
 * Represents the {@code between} operator.
 *
 * <p>The left operand must be a single value, and the right operand must be a
 * 2-element list which defines a range from a lower bound to an upper bound.
 *
 * <p>This operator returns {@code true} if the left operand is greater than or
 * equal to the lower bound and less than or equal to the upper bound. Consequently,
 * {@code 1 between {1, 5}} evaluates to {@code true}, while {@code 1 between {5, 1}}
 * evaluates to {@code false}.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
    if (!(right instanceof List<?> list) || ((List<?>) right).size() != 2) {
      throw new SpelEvaluationException(getRightOperand().getStartPosition(),
              SpelMessage.BETWEEN_RIGHT_OPERAND_MUST_BE_TWO_ELEMENT_LIST);
    }

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
