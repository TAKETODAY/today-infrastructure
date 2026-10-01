/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.expression.spel.ast;

import java.math.BigDecimal;
import java.math.BigInteger;

import infra.expression.EvaluationException;
import infra.expression.Operation;
import infra.expression.TypedValue;
import infra.expression.spel.ExpressionState;
import infra.expression.spel.SpelEvaluationException;
import infra.expression.spel.SpelMessage;

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
      if (leftNumber instanceof BigDecimal leftBigDecimal) {
        int exponent = rightNumber.intValue();
        checkBigNumberPowerBits(state, leftBigDecimal.unscaledValue().bitLength(), exponent);
        return new TypedValue(leftBigDecimal.pow(exponent));
      }
      else if (leftNumber instanceof BigInteger leftBigInteger) {
        int exponent = rightNumber.intValue();
        checkBigNumberPowerBits(state, leftBigInteger.bitLength(), exponent);
        return new TypedValue(leftBigInteger.pow(exponent));
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

  private void checkBigNumberPowerBits(ExpressionState state, int baseBitLength, int exponent) {
    int limit = state.getConfiguration().getMaximumBigPowerBits();
    if ((long) baseBitLength * exponent > limit) {
      throw new SpelEvaluationException(getStartPosition(), SpelMessage.MAX_BIG_POWER_RESULT_EXCEEDED,
              baseBitLength, exponent, limit);
    }
  }

}
