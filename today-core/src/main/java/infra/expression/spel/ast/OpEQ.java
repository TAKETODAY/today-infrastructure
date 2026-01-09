/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.expression.spel.ast;

import infra.bytecode.MethodVisitor;
import infra.bytecode.core.CodeFlow;
import infra.expression.EvaluationContext;
import infra.expression.EvaluationException;
import infra.expression.spel.ExpressionState;
import infra.expression.spel.support.BooleanTypedValue;

/**
 * Implements the equality operator.
 *
 * @author Andy Clement
 * @since 4.0
 */
public class OpEQ extends Operator {

  public OpEQ(int startPos, int endPos, SpelNodeImpl... operands) {
    super("==", startPos, endPos, operands);
    this.exitTypeDescriptor = "Z";
  }

  @Override
  public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    Object left = getLeftOperand().getValueInternal(state).getValue();
    Object right = getRightOperand().getValueInternal(state).getValue();
    this.leftActualDescriptor = CodeFlow.toDescriptorFromObject(left);
    this.rightActualDescriptor = CodeFlow.toDescriptorFromObject(right);
    return BooleanTypedValue.forValue(equalityCheck(state.getEvaluationContext(), left, right));
  }

  // This check is different to the one in the other numeric operators (OpLt/etc)
  // because it allows for simple object comparison
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
  @SuppressWarnings("NullAway")
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
    cf.pushDescriptor("Z");
  }

}
