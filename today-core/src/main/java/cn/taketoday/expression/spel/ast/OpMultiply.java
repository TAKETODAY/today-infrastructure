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

package cn.taketoday.expression.spel.ast;

import java.math.BigDecimal;
import java.math.BigInteger;

import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.core.CodeFlow;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.Operation;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.NumberUtils;

/**
 * Implements the {@code multiply} operator.
 *
 * <p>Conversions and promotions are handled as defined in
 * <a href="https://java.sun.com/docs/books/jls/third_edition/html/conversions.html">Section 5.6.2 of the
 * Java Language Specification</a>, with the addiction of {@code BigDecimal}/{@code BigInteger} management:
 *
 * <p>If any of the operands is of a reference type, unboxing conversion (Section 5.1.8)
 * is performed. Then:<br>
 * If either operand is of type {@code BigDecimal}, the other is converted to {@code BigDecimal}.<br>
 * If either operand is of type double, the other is converted to double.<br>
 * Otherwise, if either operand is of type float, the other is converted to float.<br>
 * If either operand is of type {@code BigInteger}, the other is converted to {@code BigInteger}.<br>
 * Otherwise, if either operand is of type long, the other is converted to long.<br>
 * Otherwise, both operands are converted to type int.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Giovanni Dall'Oglio Risso
 * @since 4.0
 */
public class OpMultiply extends Operator {

  /**
   * Maximum number of characters permitted in repeated text.
   */
  private static final int MAX_REPEATED_TEXT_SIZE = 256;

  public OpMultiply(int startPos, int endPos, SpelNodeImpl... operands) {
    super("*", startPos, endPos, operands);
  }

  /**
   * Implements the {@code multiply} operator directly here for certain types
   * of supported operands and otherwise delegates to any registered overloader
   * for types not supported here.
   * <p>Supported operand types:
   * <ul>
   * <li>numbers
   * <li>String and int ('abc' * 2 == 'abcabc')
   * </ul>
   */
  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    Object leftOperand = getLeftOperand().getValueInternal(state).getValue();
    Object rightOperand = getRightOperand().getValueInternal(state).getValue();

    if (leftOperand instanceof Number leftNumber && rightOperand instanceof Number rightNumber) {
      if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
        BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
        BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
        return new TypedValue(leftBigDecimal.multiply(rightBigDecimal));
      }
      else if (leftNumber instanceof Double || rightNumber instanceof Double) {
        this.exitTypeDescriptor = "D";
        return new TypedValue(leftNumber.doubleValue() * rightNumber.doubleValue());
      }
      else if (leftNumber instanceof Float || rightNumber instanceof Float) {
        this.exitTypeDescriptor = "F";
        return new TypedValue(leftNumber.floatValue() * rightNumber.floatValue());
      }
      else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
        BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
        BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
        return new TypedValue(leftBigInteger.multiply(rightBigInteger));
      }
      else if (leftNumber instanceof Long || rightNumber instanceof Long) {
        this.exitTypeDescriptor = "J";
        return new TypedValue(leftNumber.longValue() * rightNumber.longValue());
      }
      else if (CodeFlow.isIntegerForNumericOp(leftNumber) || CodeFlow.isIntegerForNumericOp(rightNumber)) {
        this.exitTypeDescriptor = "I";
        return new TypedValue(leftNumber.intValue() * rightNumber.intValue());
      }
      else {
        // Unknown Number subtypes -> best guess is double multiplication
        return new TypedValue(leftNumber.doubleValue() * rightNumber.doubleValue());
      }
    }

    if (leftOperand instanceof String text && rightOperand instanceof Integer count) {
      checkRepeatedTextSize(text, count);
      return new TypedValue(text.repeat(count));
    }

    return state.operate(Operation.MULTIPLY, leftOperand, rightOperand);
  }

  private void checkRepeatedTextSize(String text, int count) {
    if (text.length() * count > MAX_REPEATED_TEXT_SIZE) {
      throw new SpelEvaluationException(getStartPosition(),
              SpelMessage.MAX_REPEATED_TEXT_SIZE_EXCEEDED, MAX_REPEATED_TEXT_SIZE);
    }
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
        case 'I' -> mv.visitInsn(IMUL);
        case 'J' -> mv.visitInsn(LMUL);
        case 'F' -> mv.visitInsn(FMUL);
        case 'D' -> mv.visitInsn(DMUL);
        default -> throw new IllegalStateException(
                "Unrecognized exit type descriptor: '" + this.exitTypeDescriptor + "'");
      }
    }
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

}
