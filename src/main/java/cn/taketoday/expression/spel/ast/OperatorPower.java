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

import java.math.BigDecimal;
import java.math.BigInteger;

import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.Operation;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.util.NumberUtils;

/**
 * The power operator.
 *
 * @author Andy Clement
 * @author Giovanni Dall'Oglio Risso
 * @since 4.0
 */
public class OperatorPower extends Operator {

  public OperatorPower(int startPos, int endPos, SpelNodeImpl... operands) {
    super("^", startPos, endPos, operands);
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    SpelNodeImpl leftOp = getLeftOperand();
    SpelNodeImpl rightOp = getRightOperand();

    Object leftOperand = leftOp.getValueInternal(state).getValue();
    Object rightOperand = rightOp.getValueInternal(state).getValue();

    if (leftOperand instanceof Number leftNumber && rightOperand instanceof Number rightNumber) {
      if (leftNumber instanceof BigDecimal) {
        BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
        return new TypedValue(leftBigDecimal.pow(rightNumber.intValue()));
      }
      else if (leftNumber instanceof BigInteger) {
        BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
        return new TypedValue(leftBigInteger.pow(rightNumber.intValue()));
      }
      else if (leftNumber instanceof Double || rightNumber instanceof Double) {
        return new TypedValue(Math.pow(leftNumber.doubleValue(), rightNumber.doubleValue()));
      }
      else if (leftNumber instanceof Float || rightNumber instanceof Float) {
        return new TypedValue(Math.pow(leftNumber.floatValue(), rightNumber.floatValue()));
      }

      double d = Math.pow(leftNumber.doubleValue(), rightNumber.doubleValue());
      if (d > Integer.MAX_VALUE || leftNumber instanceof Long || rightNumber instanceof Long) {
        return new TypedValue((long) d);
      }
      else {
        return new TypedValue((int) d);
      }
    }

    return state.operate(Operation.POWER, leftOperand, rightOperand);
  }

}
