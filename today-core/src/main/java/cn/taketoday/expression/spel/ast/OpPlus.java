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
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.Operation;
import cn.taketoday.expression.TypeConverter;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.NumberUtils;

/**
 * The plus operator will:
 * <ul>
 * <li>add numbers
 * <li>concatenate strings
 * </ul>
 *
 * <p>It can be used as a unary operator for numbers.
 * The standard promotions are performed when the operand types vary (double+int=double).
 * For other options it defers to the registered overloader.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Ivo Smid
 * @author Giovanni Dall'Oglio Risso
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class OpPlus extends Operator {

  /**
   * Maximum number of characters permitted in a concatenated string.
   */
  private static final int MAX_CONCATENATED_STRING_LENGTH = 100_000;

  public OpPlus(int startPos, int endPos, SpelNodeImpl... operands) {
    super("+", startPos, endPos, operands);
    Assert.notEmpty(operands, "Operands must not be empty");
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    SpelNodeImpl leftOp = getLeftOperand();

    if (this.children.length < 2) {  // if only one operand, then this is unary plus
      Object operandOne = leftOp.getValueInternal(state).getValue();
      if (operandOne instanceof Number) {
        if (operandOne instanceof Double) {
          this.exitTypeDescriptor = "D";
        }
        else if (operandOne instanceof Float) {
          this.exitTypeDescriptor = "F";
        }
        else if (operandOne instanceof Long) {
          this.exitTypeDescriptor = "J";
        }
        else if (operandOne instanceof Integer) {
          this.exitTypeDescriptor = "I";
        }
        return new TypedValue(operandOne);
      }
      return state.operate(Operation.ADD, operandOne, null);
    }

    TypedValue operandOneValue = leftOp.getValueInternal(state);
    Object leftOperand = operandOneValue.getValue();
    TypedValue operandTwoValue = getRightOperand().getValueInternal(state);
    Object rightOperand = operandTwoValue.getValue();

    if (leftOperand instanceof Number leftNumber && rightOperand instanceof Number rightNumber) {
      if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
        BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
        BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
        return new TypedValue(leftBigDecimal.add(rightBigDecimal));
      }
      else if (leftNumber instanceof Double || rightNumber instanceof Double) {
        this.exitTypeDescriptor = "D";
        return new TypedValue(leftNumber.doubleValue() + rightNumber.doubleValue());
      }
      else if (leftNumber instanceof Float || rightNumber instanceof Float) {
        this.exitTypeDescriptor = "F";
        return new TypedValue(leftNumber.floatValue() + rightNumber.floatValue());
      }
      else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
        BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
        BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
        return new TypedValue(leftBigInteger.add(rightBigInteger));
      }
      else if (leftNumber instanceof Long || rightNumber instanceof Long) {
        this.exitTypeDescriptor = "J";
        return new TypedValue(leftNumber.longValue() + rightNumber.longValue());
      }
      else if (CodeFlow.isIntegerForNumericOp(leftNumber) || CodeFlow.isIntegerForNumericOp(rightNumber)) {
        this.exitTypeDescriptor = "I";
        return new TypedValue(leftNumber.intValue() + rightNumber.intValue());
      }
      else {
        // Unknown Number subtypes -> best guess is double addition
        return new TypedValue(leftNumber.doubleValue() + rightNumber.doubleValue());
      }
    }

    if (leftOperand instanceof String leftString && rightOperand instanceof String rightString) {
      this.exitTypeDescriptor = "Ljava/lang/String";
      checkStringLength(leftString);
      checkStringLength(rightString);
      return concatenate(leftString, rightString);
    }

    if (leftOperand instanceof String leftString) {
      checkStringLength(leftString);
      String rightString = (rightOperand == null ? "null" : convertTypedValueToString(operandTwoValue, state));
      checkStringLength(rightString);
      return concatenate(leftString, rightString);
    }

    if (rightOperand instanceof String rightString) {
      checkStringLength(rightString);
      String leftString = (leftOperand == null ? "null" : convertTypedValueToString(operandOneValue, state));
      checkStringLength(leftString);
      return concatenate(leftString, rightString);
    }

    return state.operate(Operation.ADD, leftOperand, rightOperand);
  }

  private void checkStringLength(String string) {
    if (string.length() > MAX_CONCATENATED_STRING_LENGTH) {
      throw new SpelEvaluationException(getStartPosition(),
              SpelMessage.MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, MAX_CONCATENATED_STRING_LENGTH);
    }
  }

  private TypedValue concatenate(String leftString, String rightString) {
    String result = leftString + rightString;
    checkStringLength(result);
    return new TypedValue(result);
  }

  @Override
  public String toStringAST() {
    if (this.children.length < 2) {  // unary plus
      return "+" + getLeftOperand().toStringAST();
    }
    return super.toStringAST();
  }

  @Override
  public SpelNodeImpl getRightOperand() {
    if (this.children.length < 2) {
      throw new IllegalStateException("No right operand");
    }
    return this.children[1];
  }

  /**
   * Convert operand value to string using registered converter or using
   * {@code toString} method.
   *
   * @param value typed value to be converted
   * @param state expression state
   * @return {@code TypedValue} instance converted to {@code String}
   */
  private static String convertTypedValueToString(TypedValue value, ExpressionState state) {
    TypeConverter typeConverter = state.getEvaluationContext().getTypeConverter();
    TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(String.class);
    if (typeConverter.canConvert(value.getTypeDescriptor(), typeDescriptor)) {
      return String.valueOf(typeConverter.convertValue(value.getValue(),
              value.getTypeDescriptor(), typeDescriptor));
    }
    return String.valueOf(value.getValue());
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

  /**
   * Walk through a possible tree of nodes that combine strings and append
   * them all to the same (on stack) StringBuilder.
   */
  private void walk(MethodVisitor mv, CodeFlow cf, @Nullable SpelNodeImpl operand) {
    if (operand instanceof OpPlus plus) {
      walk(mv, cf, plus.getLeftOperand());
      walk(mv, cf, plus.getRightOperand());
    }
    else if (operand != null) {
      cf.enterCompilationScope();
      operand.generateCode(mv, cf);
      if (!"Ljava/lang/String".equals(cf.lastDescriptor())) {
        mv.visitTypeInsn(CHECKCAST, "java/lang/String");
      }
      cf.exitCompilationScope();
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
    }
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    if ("Ljava/lang/String".equals(this.exitTypeDescriptor)) {
      mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
      walk(mv, cf, getLeftOperand());
      walk(mv, cf, getRightOperand());
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
    }
    else {
      this.children[0].generateCode(mv, cf);
      String leftDesc = this.children[0].exitTypeDescriptor;
      String exitDesc = this.exitTypeDescriptor;
      Assert.state(exitDesc != null, "No exit type descriptor");
      char targetDesc = exitDesc.charAt(0);
      CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, leftDesc, targetDesc);
      if (this.children.length > 1) {
        cf.enterCompilationScope();
        this.children[1].generateCode(mv, cf);
        String rightDesc = this.children[1].exitTypeDescriptor;
        cf.exitCompilationScope();
        CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, rightDesc, targetDesc);
        switch (targetDesc) {
          case 'I' -> mv.visitInsn(IADD);
          case 'J' -> mv.visitInsn(LADD);
          case 'F' -> mv.visitInsn(FADD);
          case 'D' -> mv.visitInsn(DADD);
          default -> throw new IllegalStateException(
                  "Unrecognized exit type descriptor: '" + this.exitTypeDescriptor + "'");
        }
      }
    }
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

}
