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
import java.math.RoundingMode;

import infra.bytecode.MethodVisitor;
import infra.bytecode.core.CodeFlow;
import infra.expression.EvaluationException;
import infra.expression.Operation;
import infra.expression.TypedValue;
import infra.expression.spel.ExpressionState;
import infra.lang.Assert;
import infra.util.NumberUtils;

/**
 * Implements division operator.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Giovanni Dall'Oglio Risso
 * @author Sam Brannen
 * @since 4.0
 */
public class OpDivide extends Operator {

  public OpDivide(int startPos, int endPos, SpelNodeImpl... operands) {
    super("/", startPos, endPos, operands);
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    Object leftOperand = getLeftOperand().getValueInternal(state).getValue();
    Object rightOperand = getRightOperand().getValueInternal(state).getValue();

    if (leftOperand instanceof Number leftNumber && rightOperand instanceof Number rightNumber) {
      if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
        BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
        BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
        int scale = Math.max(leftBigDecimal.scale(), rightBigDecimal.scale());
        return new TypedValue(leftBigDecimal.divide(rightBigDecimal, scale, RoundingMode.HALF_EVEN));
      }
      else if (leftNumber instanceof Double || rightNumber instanceof Double) {
        this.exitTypeDescriptor = "D";
        return new TypedValue(leftNumber.doubleValue() / rightNumber.doubleValue());
      }
      else if (leftNumber instanceof Float || rightNumber instanceof Float) {
        this.exitTypeDescriptor = "F";
        return new TypedValue(leftNumber.floatValue() / rightNumber.floatValue());
      }
      else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
        BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
        BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
        return new TypedValue(leftBigInteger.divide(rightBigInteger));
      }
      else if (leftNumber instanceof Long || rightNumber instanceof Long) {
        this.exitTypeDescriptor = "J";
        return new TypedValue(leftNumber.longValue() / rightNumber.longValue());
      }
      else if (CodeFlow.isIntegerForNumericOp(leftNumber) || CodeFlow.isIntegerForNumericOp(rightNumber)) {
        this.exitTypeDescriptor = "I";
        return new TypedValue(leftNumber.intValue() / rightNumber.intValue());
      }
      else {
        // Unknown Number subtypes -> best guess is double division
        return new TypedValue(leftNumber.doubleValue() / rightNumber.doubleValue());
      }
    }

    return state.operate(Operation.DIVIDE, leftOperand, rightOperand);
  }

  @Override
  public boolean isCompilable() {
    if (!getLeftOperand().isCompilable()) {
      return false;
    }
    if (this.children.length > 1) {
      if (!getRightOperand().isCompilable()) {
        return false;
      }
    }
    return (this.exitTypeDescriptor != null);
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    getLeftOperand().generateCode(mv, cf);
    String leftDesc = getLeftOperand().exitTypeDescriptor;
    String exitDesc = this.exitTypeDescriptor;
    Assert.state(exitDesc != null, "No exit type descriptor");
    char targetDesc = exitDesc.charAt(0);
    CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, leftDesc, targetDesc);
    if (this.children.length > 1) {
      cf.enterCompilationScope();
      getRightOperand().generateCode(mv, cf);
      String rightDesc = getRightOperand().exitTypeDescriptor;
      cf.exitCompilationScope();
      CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, rightDesc, targetDesc);
      switch (targetDesc) {
        case 'I' -> mv.visitInsn(IDIV);
        case 'J' -> mv.visitInsn(LDIV);
        case 'F' -> mv.visitInsn(FDIV);
        case 'D' -> mv.visitInsn(DDIV);
        default -> throw new IllegalStateException(
                "Unrecognized exit type descriptor: '" + this.exitTypeDescriptor + "'");
      }
    }
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

}
