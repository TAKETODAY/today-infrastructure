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
import cn.taketoday.core.bytecode.core.CodeFlow;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.NumberUtils;

/**
 * The minus operator supports:
 * <ul>
 * <li>subtraction of numbers
 * <li>subtraction of an int from a string of one character
 * (effectively decreasing that character), so 'd'-3='a'
 * </ul>
 *
 * <p>It can be used as a unary operator for numbers.
 * The standard promotions are performed when the operand types vary (double-int=double).
 * For other options it defers to the registered overloader.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Giovanni Dall'Oglio Risso
 * @author Sam Brannen
 * @since 4.0
 */
public class OpMinus extends Operator {

  public OpMinus(int startPos, int endPos, SpelNodeImpl... operands) {
    super("-", startPos, endPos, operands);
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    SpelNodeImpl leftOp = getLeftOperand();

    if (this.children.length < 2) {  // if only one operand, then this is unary minus
      Object operand = leftOp.getValueInternal(state).getValue();
      if (operand instanceof Number number) {
        if (number instanceof BigDecimal bigDecimal) {
          return new TypedValue(bigDecimal.negate());
        }
        else if (number instanceof BigInteger bigInteger) {
          return new TypedValue(bigInteger.negate());
        }
        else if (number instanceof Double) {
          this.exitTypeDescriptor = "D";
          return new TypedValue(0 - number.doubleValue());
        }
        else if (number instanceof Float) {
          this.exitTypeDescriptor = "F";
          return new TypedValue(0 - number.floatValue());
        }
        else if (number instanceof Long) {
          this.exitTypeDescriptor = "J";
          return new TypedValue(0 - number.longValue());
        }
        else if (number instanceof Integer) {
          this.exitTypeDescriptor = "I";
          return new TypedValue(0 - number.intValue());
        }
        else if (number instanceof Short) {
          return new TypedValue(0 - number.shortValue());
        }
        else if (number instanceof Byte) {
          return new TypedValue(0 - number.byteValue());
        }
        else {
          // Unknown Number subtype -> best guess is double subtraction
          return new TypedValue(0 - number.doubleValue());
        }
      }
      return state.operate(Operation.SUBTRACT, operand, null);
    }

    Object left = leftOp.getValueInternal(state).getValue();
    Object right = getRightOperand().getValueInternal(state).getValue();

    if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
      if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
        BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
        BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
        return new TypedValue(leftBigDecimal.subtract(rightBigDecimal));
      }
      else if (leftNumber instanceof Double || rightNumber instanceof Double) {
        this.exitTypeDescriptor = "D";
        return new TypedValue(leftNumber.doubleValue() - rightNumber.doubleValue());
      }
      else if (leftNumber instanceof Float || rightNumber instanceof Float) {
        this.exitTypeDescriptor = "F";
        return new TypedValue(leftNumber.floatValue() - rightNumber.floatValue());
      }
      else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
        BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
        BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
        return new TypedValue(leftBigInteger.subtract(rightBigInteger));
      }
      else if (leftNumber instanceof Long || rightNumber instanceof Long) {
        this.exitTypeDescriptor = "J";
        return new TypedValue(leftNumber.longValue() - rightNumber.longValue());
      }
      else if (CodeFlow.isIntegerForNumericOp(leftNumber) || CodeFlow.isIntegerForNumericOp(rightNumber)) {
        this.exitTypeDescriptor = "I";
        return new TypedValue(leftNumber.intValue() - rightNumber.intValue());
      }
      else {
        // Unknown Number subtypes -> best guess is double subtraction
        return new TypedValue(leftNumber.doubleValue() - rightNumber.doubleValue());
      }
    }

    if (left instanceof String theString && right instanceof Integer theInteger && theString.length() == 1) {
      // Implements character - int (ie. b - 1 = a)
      return new TypedValue(Character.toString((char) (theString.charAt(0) - theInteger)));
    }

    return state.operate(Operation.SUBTRACT, left, right);
  }

  @Override
  public String toStringAST() {
    if (this.children.length < 2) {  // unary minus
      return "-" + getLeftOperand().toStringAST();
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
        case 'I' -> mv.visitInsn(ISUB);
        case 'J' -> mv.visitInsn(LSUB);
        case 'F' -> mv.visitInsn(FSUB);
        case 'D' -> mv.visitInsn(DSUB);
        default -> throw new IllegalStateException(
                "Unrecognized exit type descriptor: '" + this.exitTypeDescriptor + "'");
      }
    }
    else {
      switch (targetDesc) {
        case 'I' -> mv.visitInsn(INEG);
        case 'J' -> mv.visitInsn(LNEG);
        case 'F' -> mv.visitInsn(FNEG);
        case 'D' -> mv.visitInsn(DNEG);
        default -> throw new IllegalStateException(
                "Unrecognized exit type descriptor: '" + this.exitTypeDescriptor + "'");
      }
    }
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

}
