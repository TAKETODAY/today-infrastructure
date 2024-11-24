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
import infra.bytecode.core.CodeFlow;
import infra.expression.EvaluationException;
import infra.expression.spel.ExpressionState;
import infra.expression.spel.SpelEvaluationException;
import infra.expression.spel.SpelMessage;
import infra.expression.spel.support.BooleanTypedValue;

/**
 * Represents a NOT operation.
 *
 * @author Andy Clement
 * @author Mark Fisher
 * @author Oliver Becker
 * @since 4.0
 */
public class OperatorNot extends SpelNodeImpl {  // Not is a unary operator so does not extend BinaryOperator

  public OperatorNot(int startPos, int endPos, SpelNodeImpl operand) {
    super(startPos, endPos, operand);
    this.exitTypeDescriptor = "Z";
  }

  @Override
  public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    try {
      Boolean value = this.children[0].getValue(state, Boolean.class);
      if (value == null) {
        throw new SpelEvaluationException(SpelMessage.TYPE_CONVERSION_ERROR, "null", "boolean");
      }
      return BooleanTypedValue.forValue(!value);
    }
    catch (SpelEvaluationException ex) {
      ex.setPosition(getChild(0).getStartPosition());
      throw ex;
    }
  }

  @Override
  public String toStringAST() {
    return "!" + getChild(0).toStringAST();
  }

  @Override
  public boolean isCompilable() {
    SpelNodeImpl child = this.children[0];
    return (child.isCompilable() && CodeFlow.isBooleanCompatible(child.exitTypeDescriptor));
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    this.children[0].generateCode(mv, cf);
    cf.unboxBooleanIfNecessary(mv);
    Label elseTarget = new Label();
    Label endOfIf = new Label();
    mv.visitJumpInsn(IFNE, elseTarget);
    mv.visitInsn(ICONST_1); // TRUE
    mv.visitJumpInsn(GOTO, endOfIf);
    mv.visitLabel(elseTarget);
    mv.visitInsn(ICONST_0); // FALSE
    mv.visitLabel(endOfIf);
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

}
