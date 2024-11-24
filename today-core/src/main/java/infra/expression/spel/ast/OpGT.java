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

package infra.expression.spel.ast;

import java.math.BigDecimal;
import java.math.BigInteger;

import infra.bytecode.MethodVisitor;
import infra.expression.EvaluationException;
import infra.bytecode.core.CodeFlow;
import infra.expression.spel.ExpressionState;
import infra.expression.spel.support.BooleanTypedValue;
import infra.util.NumberUtils;

/**
 * Implements the greater-than operator.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Giovanni Dall'Oglio Risso
 * @since 4.0
 */
public class OpGT extends Operator {

  public OpGT(int startPos, int endPos, SpelNodeImpl... operands) {
    super(">", startPos, endPos, operands);
    this.exitTypeDescriptor = "Z";
  }

  @Override
  public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    Object left = getLeftOperand().getValueInternal(state).getValue();
    Object right = getRightOperand().getValueInternal(state).getValue();

    this.leftActualDescriptor = CodeFlow.toDescriptorFromObject(left);
    this.rightActualDescriptor = CodeFlow.toDescriptorFromObject(right);

    if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
      if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
        BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
        BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
        return BooleanTypedValue.forValue(leftBigDecimal.compareTo(rightBigDecimal) > 0);
      }
      else if (leftNumber instanceof Double || rightNumber instanceof Double) {
        return BooleanTypedValue.forValue(leftNumber.doubleValue() > rightNumber.doubleValue());
      }
      else if (leftNumber instanceof Float || rightNumber instanceof Float) {
        return BooleanTypedValue.forValue(leftNumber.floatValue() > rightNumber.floatValue());
      }
      else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
        BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
        BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
        return BooleanTypedValue.forValue(leftBigInteger.compareTo(rightBigInteger) > 0);
      }
      else if (leftNumber instanceof Long || rightNumber instanceof Long) {
        return BooleanTypedValue.forValue(leftNumber.longValue() > rightNumber.longValue());
      }
      else if (leftNumber instanceof Integer || rightNumber instanceof Integer) {
        return BooleanTypedValue.forValue(leftNumber.intValue() > rightNumber.intValue());
      }
      else if (leftNumber instanceof Short || rightNumber instanceof Short) {
        return BooleanTypedValue.forValue(leftNumber.shortValue() > rightNumber.shortValue());
      }
      else if (leftNumber instanceof Byte || rightNumber instanceof Byte) {
        return BooleanTypedValue.forValue(leftNumber.byteValue() > rightNumber.byteValue());
      }
      else {
        // Unknown Number subtypes -> best guess is double comparison
        return BooleanTypedValue.forValue(leftNumber.doubleValue() > rightNumber.doubleValue());
      }
    }

    if (left instanceof CharSequence && right instanceof CharSequence) {
      left = left.toString();
      right = right.toString();
    }

    return BooleanTypedValue.forValue(state.getTypeComparator().compare(left, right) > 0);
  }

  @Override
  public boolean isCompilable() {
    return isCompilableOperatorUsingNumerics();
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    generateComparisonCode(mv, cf, IFLE, IF_ICMPLE);
  }

}
