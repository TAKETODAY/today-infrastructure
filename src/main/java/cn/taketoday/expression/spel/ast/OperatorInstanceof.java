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

import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.CodeFlow;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.expression.spel.support.BooleanTypedValue;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * The operator 'instanceof' checks if an object is of the class specified in the right
 * hand operand, in the same way that {@code instanceof} does in Java.
 *
 * @author Andy Clement
 * @since 4.0
 */
public class OperatorInstanceof extends Operator {

  @Nullable
  private Class<?> type;

  public OperatorInstanceof(int startPos, int endPos, SpelNodeImpl... operands) {
    super("instanceof", startPos, endPos, operands);
  }

  /**
   * Compare the left operand to see it is an instance of the type specified as the
   * right operand. The right operand must be a class.
   *
   * @param state the expression state
   * @return {@code true} if the left operand is an instanceof of the right operand,
   * otherwise {@code false}
   * @throws EvaluationException if there is a problem evaluating the expression
   */
  @Override
  public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    SpelNodeImpl rightOperand = getRightOperand();
    TypedValue left = getLeftOperand().getValueInternal(state);
    TypedValue right = rightOperand.getValueInternal(state);
    Object leftValue = left.getValue();
    Object rightValue = right.getValue();
    BooleanTypedValue result;
    if (!(rightValue instanceof Class<?> rightClass)) {
      throw new SpelEvaluationException(getRightOperand().getStartPosition(),
              SpelMessage.INSTANCEOF_OPERATOR_NEEDS_CLASS_OPERAND,
              (rightValue == null ? "null" : rightValue.getClass().getName()));
    }
    if (leftValue == null) {
      result = BooleanTypedValue.FALSE;  // null is not an instanceof anything
    }
    else {
      result = BooleanTypedValue.forValue(rightClass.isAssignableFrom(leftValue.getClass()));
    }
    this.type = rightClass;
    if (rightOperand instanceof TypeReference) {
      // Can only generate bytecode where the right operand is a direct type reference,
      // not if it is indirect (for example when right operand is a variable reference)
      this.exitTypeDescriptor = "Z";
    }
    return result;
  }

  @Override
  public boolean isCompilable() {
    return (this.exitTypeDescriptor != null && getLeftOperand().isCompilable());
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    getLeftOperand().generateCode(mv, cf);
    CodeFlow.insertBoxIfNecessary(mv, cf.lastDescriptor());
    Assert.state(this.type != null, "No type available");
    if (this.type.isPrimitive()) {
      // always false - but left operand code always driven
      // in case it had side effects
      mv.visitInsn(POP);
      mv.visitInsn(ICONST_0); // value of false
    }
    else {
      mv.visitTypeInsn(INSTANCEOF, Type.getInternalName(this.type));
    }
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

}
