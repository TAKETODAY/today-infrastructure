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

import org.jspecify.annotations.Nullable;

import infra.bytecode.MethodVisitor;
import infra.bytecode.Type;
import infra.bytecode.core.CodeFlow;
import infra.expression.EvaluationException;
import infra.expression.TypedValue;
import infra.expression.spel.ExpressionState;
import infra.expression.spel.SpelEvaluationException;
import infra.expression.spel.SpelMessage;
import infra.expression.spel.support.BooleanTypedValue;
import infra.lang.Assert;

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
