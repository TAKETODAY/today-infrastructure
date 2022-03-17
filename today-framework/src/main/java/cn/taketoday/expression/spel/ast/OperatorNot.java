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

import cn.taketoday.core.bytecode.Label;
import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.spel.CodeFlow;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.expression.spel.support.BooleanTypedValue;

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
