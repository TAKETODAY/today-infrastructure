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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import infra.bytecode.MethodVisitor;
import infra.expression.EvaluationException;
import infra.expression.TypedValue;
import infra.expression.spel.CodeFlow;
import infra.expression.spel.ExpressionState;
import infra.expression.spel.SpelNode;
import infra.expression.spel.support.SimpleEvaluationContext;
import infra.util.Assert;

/**
 * Represent a list in an expression, e.g. '{1,2,3}'
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class InlineList extends SpelNodeImpl {

  private final boolean isConstant;

  private volatile @Nullable TypedValue constant;

  public InlineList(int startPos, int endPos, SpelNodeImpl... args) {
    super(startPos, endPos, args);
    this.isConstant = determineIfConstant();
  }

  /**
   * Determine whether this list is structurally eligible to be a constant
   * value: whether all of its components are themselves constants or lists
   * that contain only constants.
   * <p>The actual constant value is created lazily on the first call to
   * {@link #getValueInternal(ExpressionState)}.
   */
  private boolean determineIfConstant() {
    for (SpelNode child : this.children) {
      if (child instanceof Literal) {
        continue;
      }
      if (child instanceof InlineList inlineList && inlineList.isConstant()) {
        continue;
      }
      if (child instanceof OpMinus opMinus && opMinus.isNegativeNumberLiteral()) {
        continue;
      }
      return false;
    }
    return true;
  }

  @Override
  public TypedValue getValueInternal(ExpressionState expressionState) throws EvaluationException {
    TypedValue result = this.constant;
    if (result != null) {
      return result;
    }
    result = createList(expressionState);
    if (this.isConstant) {
      this.constant = result;
    }
    return result;
  }

  private TypedValue createList(ExpressionState expressionState) throws EvaluationException {
    int childCount = getChildCount();
    expressionState.trackOperation();
    ArrayList<Object> list = new ArrayList<>(childCount);
    for (SpelNodeImpl child : this.children) {
      expressionState.trackOperation();
      list.add(child.getValue(expressionState));
    }
    return new TypedValue(this.isConstant ? Collections.unmodifiableList(list) : list);
  }

  @Override
  public String toStringAST() {
    StringJoiner sj = new StringJoiner(",", "{", "}");
    // String ast matches input string, not the 'toString()' of the resultant collection, which would use []
    for (SpelNodeImpl child : children) {
      sj.add(child.toStringAST());
    }
    return sj.toString();
  }

  /**
   * Return whether this list is structurally a constant value.
   * <p>Note that the resulting constant value is created lazily on the
   * first call to {@link #getValueInternal(ExpressionState)} or
   * {@link #getConstantValue()}.
   */
  public boolean isConstant() {
    return this.isConstant;
  }

  /**
   * Return the cached constant {@link List} value for this inline list,
   * lazily creating it on first access.
   *
   * @see #isConstant()
   * @deprecated as of 5.0; this method was only intended for testing purposes
   */
  @SuppressWarnings("unchecked")
  @Deprecated(since = "5.0")
  public @Nullable List<Object> getConstantValue() {
    Assert.state(this.isConstant, "Not a constant");
    TypedValue result = this.constant;
    if (result == null) {
      result = createList(new ExpressionState(SimpleEvaluationContext.forReadOnlyDataBinding().build()));
      this.constant = result;
    }
    return (List<Object>) result.getValue();
  }

  @Override
  public boolean isCompilable() {
    return this.isConstant;
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow codeflow) {
    final String constantFieldName = "inlineList$" + codeflow.nextFieldId();
    final String className = codeflow.getClassName();

    codeflow.registerNewField((cw, cflow) ->
            cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                    constantFieldName, "Ljava/util/List;", null, null));

    codeflow.registerNewClinit((mVisitor, cflow) ->
            generateClinitCode(className, constantFieldName, mVisitor, cflow, false));

    mv.visitFieldInsn(GETSTATIC, className, constantFieldName, "Ljava/util/List;");
    codeflow.pushDescriptor("Ljava/util/List");
  }

  @SuppressWarnings("NullAway")
  void generateClinitCode(String clazzname, String constantFieldName, MethodVisitor mv, CodeFlow codeflow, boolean nested) {
    mv.visitTypeInsn(NEW, "java/util/ArrayList");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
    if (!nested) {
      mv.visitFieldInsn(PUTSTATIC, clazzname, constantFieldName, "Ljava/util/List;");
    }
    int childCount = getChildCount();
    for (int c = 0; c < childCount; c++) {
      if (!nested) {
        mv.visitFieldInsn(GETSTATIC, clazzname, constantFieldName, "Ljava/util/List;");
      }
      else {
        mv.visitInsn(DUP);
      }
      // Nested InlineList children are always constant (guaranteed by isCompilable/isConstant).
      // Thus, we call generateClinitCode() directly rather than generateCode() to avoid registering
      // a separate static field and clinit entry for each nested list. In other words, we build each
      // nested list inline within the current clinit sequence.
      if (this.children[c] instanceof InlineList inlineList) {
        inlineList.generateClinitCode(clazzname, constantFieldName, mv, codeflow, true);
      }
      else {
        this.children[c].generateCode(mv, codeflow);
        String lastDesc = codeflow.lastDescriptor();
        if (CodeFlow.isPrimitive(lastDesc)) {
          CodeFlow.insertBoxIfNecessary(mv, lastDesc.charAt(0));
        }
      }
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
      mv.visitInsn(POP);
    }
    // Wrap the mutable ArrayList in an unmodifiable list, matching the behavior
    // of the interpreted mode (see createList()). For the non-nested case, retrieve
    // the list from the static field first, then store the wrapped list back. For
    // the nested case, the list is already on the stack for the caller to use.
    if (!nested) {
      mv.visitFieldInsn(GETSTATIC, clazzname, constantFieldName, "Ljava/util/List;");
    }
    mv.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "unmodifiableList",
            "(Ljava/util/List;)Ljava/util/List;", false);
    if (!nested) {
      mv.visitFieldInsn(PUTSTATIC, clazzname, constantFieldName, "Ljava/util/List;");
    }
  }

}
