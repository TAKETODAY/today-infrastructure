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

import infra.bytecode.Label;
import infra.bytecode.MethodVisitor;
import infra.expression.EvaluationException;
import infra.expression.TypedValue;
import infra.bytecode.core.CodeFlow;
import infra.expression.spel.ExpressionState;
import infra.expression.spel.SpelEvaluationException;
import infra.expression.spel.SpelMessage;
import infra.expression.spel.support.BooleanTypedValue;
import infra.lang.Nullable;

/**
 * Represents the boolean AND operation.
 *
 * @author Andy Clement
 * @author Mark Fisher
 * @author Oliver Becker
 * @since 4.0
 */
public class OpAnd extends Operator {

  public OpAnd(int startPos, int endPos, SpelNodeImpl... operands) {
    super("and", startPos, endPos, operands);
    this.exitTypeDescriptor = "Z";
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    if (!getBooleanValue(state, getLeftOperand())) {
      // no need to evaluate right operand
      return BooleanTypedValue.FALSE;
    }
    return BooleanTypedValue.forValue(getBooleanValue(state, getRightOperand()));
  }

  private boolean getBooleanValue(ExpressionState state, SpelNodeImpl operand) {
    try {
      Boolean value = operand.getValue(state, Boolean.class);
      assertValueNotNull(value);
      return value;
    }
    catch (SpelEvaluationException ex) {
      ex.setPosition(operand.getStartPosition());
      throw ex;
    }
  }

  private void assertValueNotNull(@Nullable Boolean value) {
    if (value == null) {
      throw new SpelEvaluationException(SpelMessage.TYPE_CONVERSION_ERROR, "null", "boolean");
    }
  }

  @Override
  public boolean isCompilable() {
    SpelNodeImpl left = getLeftOperand();
    SpelNodeImpl right = getRightOperand();
    return (left.isCompilable() && right.isCompilable() &&
            CodeFlow.isBooleanCompatible(left.exitTypeDescriptor) &&
            CodeFlow.isBooleanCompatible(right.exitTypeDescriptor));
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    // Pseudo: if (!leftOperandValue) { result=false; } else { result=rightOperandValue; }
    Label elseTarget = new Label();
    Label endOfIf = new Label();
    cf.enterCompilationScope();
    getLeftOperand().generateCode(mv, cf);
    cf.unboxBooleanIfNecessary(mv);
    cf.exitCompilationScope();
    mv.visitJumpInsn(IFNE, elseTarget);
    mv.visitLdcInsn(0); // FALSE
    mv.visitJumpInsn(GOTO, endOfIf);
    mv.visitLabel(elseTarget);
    cf.enterCompilationScope();
    getRightOperand().generateCode(mv, cf);
    cf.unboxBooleanIfNecessary(mv);
    cf.exitCompilationScope();
    mv.visitLabel(endOfIf);
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

}
