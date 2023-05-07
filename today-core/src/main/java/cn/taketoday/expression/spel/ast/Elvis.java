/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.core.CodeFlow;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ObjectUtils;

/**
 * Represents the Elvis operator <code>?:</code>. For an expression <code>a?:b</code> if <code>a</code> is neither null
 * nor an empty String, the value of the expression is <code>a</code>.
 * If <code>a</code> is null or the empty String, then the value of the expression is <code>b</code>.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 4.0
 */
public class Elvis extends SpelNodeImpl {

  public Elvis(int startPos, int endPos, SpelNodeImpl... args) {
    super(startPos, endPos, args);
  }

  /**
   * Evaluate the condition and if neither null nor an empty String, return it.
   * If it is null or an empty String, return the other value.
   *
   * @param state the expression state
   * @throws EvaluationException if the condition does not evaluate correctly
   * to a boolean or there is a problem executing the chosen alternative
   */
  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    TypedValue value = this.children[0].getValueInternal(state);
    // If this check is changed, the generateCode method will need changing too
    if (value.getValue() != null && !"".equals(value.getValue())) {
      return value;
    }
    else {
      TypedValue result = this.children[1].getValueInternal(state);
      computeExitTypeDescriptor();
      return result;
    }
  }

  @Override
  public String toStringAST() {
    return "(" + getChild(0).toStringAST() + " ?: "
            + getChild(1).toStringAST() + ")";
  }

  @Override
  public boolean isCompilable() {
    SpelNodeImpl condition = this.children[0];
    SpelNodeImpl ifNullValue = this.children[1];
    return (condition.isCompilable() && ifNullValue.isCompilable() &&
            condition.exitTypeDescriptor != null && ifNullValue.exitTypeDescriptor != null);
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    // exit type descriptor can be null if both components are literal expressions
    computeExitTypeDescriptor();
    cf.enterCompilationScope();
    this.children[0].generateCode(mv, cf);
    String lastDesc = cf.lastDescriptor();
    Assert.state(lastDesc != null, "No last descriptor");
    CodeFlow.insertBoxIfNecessary(mv, lastDesc.charAt(0));
    cf.exitCompilationScope();
    Label elseTarget = new Label();
    Label endOfIf = new Label();
    mv.visitInsn(DUP);
    mv.visitJumpInsn(IFNULL, elseTarget);
    // Also check if empty string, as per the code in the interpreted version
    mv.visitInsn(DUP);
    mv.visitLdcInsn("");
    mv.visitInsn(SWAP);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
    mv.visitJumpInsn(IFEQ, endOfIf);  // if not empty, drop through to elseTarget
    mv.visitLabel(elseTarget);
    mv.visitInsn(POP);
    cf.enterCompilationScope();
    this.children[1].generateCode(mv, cf);
    if (!CodeFlow.isPrimitive(this.exitTypeDescriptor)) {
      lastDesc = cf.lastDescriptor();
      Assert.state(lastDesc != null, "No last descriptor");
      CodeFlow.insertBoxIfNecessary(mv, lastDesc.charAt(0));
    }
    cf.exitCompilationScope();
    mv.visitLabel(endOfIf);
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

  private void computeExitTypeDescriptor() {
    if (this.exitTypeDescriptor == null && this.children[0].exitTypeDescriptor != null &&
            this.children[1].exitTypeDescriptor != null) {
      String conditionDescriptor = this.children[0].exitTypeDescriptor;
      String ifNullValueDescriptor = this.children[1].exitTypeDescriptor;
      if (ObjectUtils.nullSafeEquals(conditionDescriptor, ifNullValueDescriptor)) {
        this.exitTypeDescriptor = conditionDescriptor;
      }
      else {
        // Use the easiest to compute common super type
        this.exitTypeDescriptor = "Ljava/lang/Object";
      }
    }
  }

}
