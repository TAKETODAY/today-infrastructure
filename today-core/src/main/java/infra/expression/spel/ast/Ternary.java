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
import infra.expression.TypedValue;
import infra.expression.spel.ExpressionState;
import infra.expression.spel.SpelEvaluationException;
import infra.expression.spel.SpelMessage;
import infra.lang.Assert;
import infra.util.ObjectUtils;

/**
 * Represents a ternary expression, for example: "someCheck()?true:false".
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 4.0
 */
public class Ternary extends SpelNodeImpl {

  public Ternary(int startPos, int endPos, SpelNodeImpl... args) {
    super(startPos, endPos, args);
  }

  /**
   * Evaluate the condition and if true evaluate the first alternative, otherwise
   * evaluate the second alternative.
   *
   * @param state the expression state
   * @throws EvaluationException if the condition does not evaluate correctly to
   * a boolean or there is a problem executing the chosen alternative
   */
  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    Boolean value = this.children[0].getValue(state, Boolean.class);
    if (value == null) {
      throw new SpelEvaluationException(getChild(0).getStartPosition(),
              SpelMessage.TYPE_CONVERSION_ERROR, "null", "boolean");
    }
    TypedValue result = this.children[value ? 1 : 2].getValueInternal(state);
    computeExitTypeDescriptor();
    return result;
  }

  @Override
  public String toStringAST() {
    return "(" + getChild(0).toStringAST() + " ? "
            + getChild(1).toStringAST() + " : "
            + getChild(2).toStringAST() + ")";
  }

  private void computeExitTypeDescriptor() {
    if (this.exitTypeDescriptor == null && this.children[1].exitTypeDescriptor != null &&
            this.children[2].exitTypeDescriptor != null) {
      String leftDescriptor = this.children[1].exitTypeDescriptor;
      String rightDescriptor = this.children[2].exitTypeDescriptor;
      if (ObjectUtils.nullSafeEquals(leftDescriptor, rightDescriptor)) {
        this.exitTypeDescriptor = leftDescriptor;
      }
      else {
        // Use the easiest to compute common super type
        this.exitTypeDescriptor = "Ljava/lang/Object";
      }
    }
  }

  @Override
  public boolean isCompilable() {
    SpelNodeImpl condition = this.children[0];
    SpelNodeImpl left = this.children[1];
    SpelNodeImpl right = this.children[2];
    return (condition.isCompilable() && left.isCompilable() && right.isCompilable() &&
            CodeFlow.isBooleanCompatible(condition.exitTypeDescriptor) &&
            left.exitTypeDescriptor != null && right.exitTypeDescriptor != null);
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    // May reach here without it computed if all elements are literals
    computeExitTypeDescriptor();
    cf.enterCompilationScope();
    this.children[0].generateCode(mv, cf);
    String lastDesc = cf.lastDescriptor();
    Assert.state(lastDesc != null, "No last descriptor");
    if (!CodeFlow.isPrimitive(lastDesc)) {
      CodeFlow.insertUnboxInsns(mv, 'Z', lastDesc);
    }
    cf.exitCompilationScope();
    Label elseTarget = new Label();
    Label endOfIf = new Label();
    mv.visitJumpInsn(IFEQ, elseTarget);
    cf.enterCompilationScope();
    this.children[1].generateCode(mv, cf);
    if (!CodeFlow.isPrimitive(this.exitTypeDescriptor)) {
      lastDesc = cf.lastDescriptor();
      Assert.state(lastDesc != null, "No last descriptor");
      CodeFlow.insertBoxIfNecessary(mv, lastDesc.charAt(0));
    }
    cf.exitCompilationScope();
    mv.visitJumpInsn(GOTO, endOfIf);
    mv.visitLabel(elseTarget);
    cf.enterCompilationScope();
    this.children[2].generateCode(mv, cf);
    if (!CodeFlow.isPrimitive(this.exitTypeDescriptor)) {
      lastDesc = cf.lastDescriptor();
      Assert.state(lastDesc != null, "No last descriptor");
      CodeFlow.insertBoxIfNecessary(mv, lastDesc.charAt(0));
    }
    cf.exitCompilationScope();
    mv.visitLabel(endOfIf);
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

}
