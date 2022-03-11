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
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.lang.Assert;

/**
 * Increment operator. Can be used in a prefix or postfix form. This will throw
 * appropriate exceptions if the operand in question does not support increment.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Giovanni Dall'Oglio Risso
 * @author Sam Brannen
 * @since 4.0
 */
public class OpInc extends Operator {

  private final boolean postfix;  // false means prefix

  public OpInc(int startPos, int endPos, boolean postfix, SpelNodeImpl... operands) {
    super("++", startPos, endPos, operands);
    this.postfix = postfix;
    Assert.notEmpty(operands, "Operands must not be empty");
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    SpelNodeImpl operand = getLeftOperand();
    ValueRef valueRef = operand.getValueRef(state);

    TypedValue typedValue = valueRef.getValue();
    Object value = typedValue.getValue();
    TypedValue returnValue = typedValue;
    TypedValue newValue = null;

    if (value instanceof Number op1) {
      if (op1 instanceof BigDecimal bigDecimal) {
        newValue = new TypedValue(bigDecimal.add(BigDecimal.ONE), typedValue.getTypeDescriptor());
      }
      else if (op1 instanceof Double) {
        newValue = new TypedValue(op1.doubleValue() + 1.0d, typedValue.getTypeDescriptor());
      }
      else if (op1 instanceof Float) {
        newValue = new TypedValue(op1.floatValue() + 1.0f, typedValue.getTypeDescriptor());
      }
      else if (op1 instanceof BigInteger bigInteger) {
        newValue = new TypedValue(bigInteger.add(BigInteger.ONE), typedValue.getTypeDescriptor());
      }
      else if (op1 instanceof Long) {
        newValue = new TypedValue(op1.longValue() + 1L, typedValue.getTypeDescriptor());
      }
      else if (op1 instanceof Integer) {
        newValue = new TypedValue(op1.intValue() + 1, typedValue.getTypeDescriptor());
      }
      else if (op1 instanceof Short) {
        newValue = new TypedValue(op1.shortValue() + (short) 1, typedValue.getTypeDescriptor());
      }
      else if (op1 instanceof Byte) {
        newValue = new TypedValue(op1.byteValue() + (byte) 1, typedValue.getTypeDescriptor());
      }
      else {
        // Unknown Number subtype -> best guess is double increment
        newValue = new TypedValue(op1.doubleValue() + 1.0d, typedValue.getTypeDescriptor());
      }
    }

    if (newValue == null) {
      try {
        newValue = state.operate(Operation.ADD, returnValue.getValue(), 1);
      }
      catch (SpelEvaluationException ex) {
        if (ex.getMessageCode() == SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES) {
          // This means the operand is not incrementable
          throw new SpelEvaluationException(operand.getStartPosition(),
                  SpelMessage.OPERAND_NOT_INCREMENTABLE, operand.toStringAST());
        }
        throw ex;
      }
    }

    // set the name value
    try {
      valueRef.setValue(newValue.getValue());
    }
    catch (SpelEvaluationException see) {
      // If unable to set the value the operand is not writable (e.g. 1++ )
      if (see.getMessageCode() == SpelMessage.SETVALUE_NOT_SUPPORTED) {
        throw new SpelEvaluationException(operand.getStartPosition(), SpelMessage.OPERAND_NOT_INCREMENTABLE);
      }
      else {
        throw see;
      }
    }

    if (!this.postfix) {
      // The return value is the new value, not the original value
      returnValue = newValue;
    }

    return returnValue;
  }

  @Override
  public String toStringAST() {
    return getLeftOperand().toStringAST() + "++";
  }

  @Override
  public SpelNodeImpl getRightOperand() {
    throw new IllegalStateException("No right operand");
  }

}
