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
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.spel.CodeFlow;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.support.BooleanTypedValue;

/**
 * Implements the not-equal operator.
 *
 * @author Andy Clement
 * @since 4.0
 */
public class OpNE extends Operator {

  public OpNE(int startPos, int endPos, SpelNodeImpl... operands) {
    super("!=", startPos, endPos, operands);
    this.exitTypeDescriptor = "Z";
  }

  @Override
  public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    Object leftValue = getLeftOperand().getValueInternal(state).getValue();
    Object rightValue = getRightOperand().getValueInternal(state).getValue();
    this.leftActualDescriptor = CodeFlow.toDescriptorFromObject(leftValue);
    this.rightActualDescriptor = CodeFlow.toDescriptorFromObject(rightValue);
    return BooleanTypedValue.forValue(!equalityCheck(state.getEvaluationContext(), leftValue, rightValue));
  }

  // This check is different to the one in the other numeric operators (OpLt/etc)
  // because we allow simple object comparison
  @Override
  public boolean isCompilable() {
    SpelNodeImpl left = getLeftOperand();
    SpelNodeImpl right = getRightOperand();
    if (!left.isCompilable() || !right.isCompilable()) {
      return false;
    }

    String leftDesc = left.exitTypeDescriptor;
    String rightDesc = right.exitTypeDescriptor;
    DescriptorComparison dc = DescriptorComparison.checkNumericCompatibility(leftDesc,
            rightDesc, this.leftActualDescriptor, this.rightActualDescriptor);
    return (!dc.areNumbers || dc.areCompatible);
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    cf.loadEvaluationContext(mv);
    String leftDesc = getLeftOperand().exitTypeDescriptor;
    String rightDesc = getRightOperand().exitTypeDescriptor;
    boolean leftPrim = CodeFlow.isPrimitive(leftDesc);
    boolean rightPrim = CodeFlow.isPrimitive(rightDesc);

    cf.enterCompilationScope();
    getLeftOperand().generateCode(mv, cf);
    cf.exitCompilationScope();
    if (leftPrim) {
      CodeFlow.insertBoxIfNecessary(mv, leftDesc.charAt(0));
    }
    cf.enterCompilationScope();
    getRightOperand().generateCode(mv, cf);
    cf.exitCompilationScope();
    if (rightPrim) {
      CodeFlow.insertBoxIfNecessary(mv, rightDesc.charAt(0));
    }

    String operatorClassName = Operator.class.getName().replace('.', '/');
    String evaluationContextClassName = EvaluationContext.class.getName().replace('.', '/');
    mv.visitMethodInsn(INVOKESTATIC, operatorClassName, "equalityCheck",
            "(L" + evaluationContextClassName + ";Ljava/lang/Object;Ljava/lang/Object;)Z", false);

    // Invert the boolean
    Label notZero = new Label();
    Label end = new Label();
    mv.visitJumpInsn(IFNE, notZero);
    mv.visitInsn(ICONST_1);
    mv.visitJumpInsn(GOTO, end);
    mv.visitLabel(notZero);
    mv.visitInsn(ICONST_0);
    mv.visitLabel(end);

    cf.pushDescriptor("Z");
  }

}
