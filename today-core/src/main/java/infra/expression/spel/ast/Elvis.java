/*
 * Copyright 2017 - 2025 the original author or authors.
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

import java.util.Optional;

import infra.bytecode.Label;
import infra.bytecode.MethodVisitor;
import infra.bytecode.core.CodeFlow;
import infra.expression.EvaluationException;
import infra.expression.TypedValue;
import infra.expression.spel.ExpressionState;
import infra.lang.Assert;
import infra.util.ObjectUtils;

/**
 * Represents the Elvis operator {@code ?:}.
 *
 * <p>For the expression "{@code A ?: B}", if {@code A} is neither {@code null},
 * an empty {@link Optional}, nor an empty {@link String}, the value of the
 * expression is {@code A}, or {@code A.get()} for an {@code Optional}. If
 * {@code A} is {@code null}, an empty {@code Optional}, or an
 * empty {@code String}, the value of the expression is {@code B}.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class Elvis extends SpelNodeImpl {

  public Elvis(int startPos, int endPos, SpelNodeImpl... args) {
    super(startPos, endPos, args);
  }

  /**
   * If the left-hand operand is neither {@code null}, an empty
   * {@link Optional}, nor an empty {@link String}, return its value, or the
   * value contained in the {@code Optional}. If the left-hand operand is
   * {@code null}, an empty {@code Optional}, or an empty {@code String},
   * return the other value.
   *
   * @param state the expression state
   * @throws EvaluationException if the null/empty check does not evaluate correctly
   * or there is a problem evaluating the alternative
   */
  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    TypedValue leftHandTypedValue = this.children[0].getValueInternal(state);
    Object leftHandValue = leftHandTypedValue.getValue();

    if (leftHandValue instanceof Optional<?> optional) {
      // Compilation is currently not supported for Optional with the Elvis operator.
      this.exitTypeDescriptor = null;
      if (optional.isPresent()) {
        return new TypedValue(optional.get());
      }
      return this.children[1].getValueInternal(state);
    }

    // If this check is changed, the generateCode method will need changing too
    if (leftHandValue != null && !"".equals(leftHandValue)) {
      return leftHandTypedValue;
    }
    else {
      TypedValue result = this.children[1].getValueInternal(state);
      computeExitTypeDescriptor();
      return result;
    }
  }

  @Override
  public String toStringAST() {
    return "(" + getChild(0).toStringAST() + " ?: " + getChild(1).toStringAST() + ")";
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
        // Use the easiest to compute common supertype
        this.exitTypeDescriptor = "Ljava/lang/Object";
      }
    }
  }

}
