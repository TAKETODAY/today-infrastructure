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

import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.Operation;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.CodeFlow;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.NumberUtils;

/**
 * Implements the modulus operator.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Giovanni Dall'Oglio Risso
 * @since 4.0
 */
public class OpModulus extends Operator {

  public OpModulus(int startPos, int endPos, SpelNodeImpl... operands) {
    super("%", startPos, endPos, operands);
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    Object leftOperand = getLeftOperand().getValueInternal(state).getValue();
    Object rightOperand = getRightOperand().getValueInternal(state).getValue();

    if (leftOperand instanceof Number leftNumber && rightOperand instanceof Number rightNumber) {
      if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
        BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
        BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
        return new TypedValue(leftBigDecimal.remainder(rightBigDecimal));
      }
      else if (leftNumber instanceof Double || rightNumber instanceof Double) {
        this.exitTypeDescriptor = "D";
        return new TypedValue(leftNumber.doubleValue() % rightNumber.doubleValue());
      }
      else if (leftNumber instanceof Float || rightNumber instanceof Float) {
        this.exitTypeDescriptor = "F";
        return new TypedValue(leftNumber.floatValue() % rightNumber.floatValue());
      }
      else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
        BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
        BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
        return new TypedValue(leftBigInteger.remainder(rightBigInteger));
      }
      else if (leftNumber instanceof Long || rightNumber instanceof Long) {
        this.exitTypeDescriptor = "J";
        return new TypedValue(leftNumber.longValue() % rightNumber.longValue());
      }
      else if (CodeFlow.isIntegerForNumericOp(leftNumber) || CodeFlow.isIntegerForNumericOp(rightNumber)) {
        this.exitTypeDescriptor = "I";
        return new TypedValue(leftNumber.intValue() % rightNumber.intValue());
      }
      else {
        // Unknown Number subtype -> best guess is double division
        return new TypedValue(leftNumber.doubleValue() % rightNumber.doubleValue());
      }
    }

    return state.operate(Operation.MODULUS, leftOperand, rightOperand);
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
        case 'I' -> mv.visitInsn(IREM);
        case 'J' -> mv.visitInsn(LREM);
        case 'F' -> mv.visitInsn(FREM);
        case 'D' -> mv.visitInsn(DREM);
        default -> throw new IllegalStateException(
                "Unrecognized exit type descriptor: '" + this.exitTypeDescriptor + "'");
      }
    }
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

}
