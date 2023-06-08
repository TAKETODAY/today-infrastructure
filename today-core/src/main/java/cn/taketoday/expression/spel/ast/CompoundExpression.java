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

import java.util.function.Supplier;

import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.core.CodeFlow;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelNode;

/**
 * Represents a DOT separated expression sequence, such as
 * {@code 'property1.property2.methodOne()'}.
 *
 * <p>May also contain array/collection/map indexers, such as
 * {@code property1[0].property2['key']}.
 *
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CompoundExpression extends SpelNodeImpl {

  public CompoundExpression(int startPos, int endPos, SpelNodeImpl... expressionComponents) {
    super(startPos, endPos, expressionComponents);
    if (expressionComponents.length < 2) {
      throw new IllegalStateException("Do not build compound expressions with less than two entries: " +
              expressionComponents.length);
    }
  }

  @Override
  protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
    SpelNodeImpl[] children = this.children;
    if (getChildCount() == 1) {
      return children[0].getValueRef(state);
    }

    SpelNodeImpl nextNode = children[0];
    try {
      TypedValue result = nextNode.getValueInternal(state);
      int cc = getChildCount();
      for (int i = 1; i < cc - 1; i++) {
        try {
          state.pushActiveContextObject(result);
          nextNode = children[i];
          result = nextNode.getValueInternal(state);
        }
        finally {
          state.popActiveContextObject();
        }
      }
      try {
        state.pushActiveContextObject(result);
        nextNode = children[cc - 1];
        return nextNode.getValueRef(state);
      }
      finally {
        state.popActiveContextObject();
      }
    }
    catch (SpelEvaluationException ex) {
      // Correct the position for the error before re-throwing
      ex.setPosition(nextNode.getStartPosition());
      throw ex;
    }
  }

  /**
   * Evaluates a compound expression. This involves evaluating each piece in turn and the
   * return value from each piece is the active context object for the subsequent piece.
   *
   * @param state the state in which the expression is being evaluated
   * @return the final value from the last piece of the compound expression
   */
  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    ValueRef ref = getValueRef(state);
    TypedValue result = ref.getValue();
    this.exitTypeDescriptor = this.children[this.children.length - 1].exitTypeDescriptor;
    return result;
  }

  @Override
  public TypedValue setValueInternal(ExpressionState state, Supplier<TypedValue> valueSupplier)
          throws EvaluationException {

    TypedValue typedValue = valueSupplier.get();
    getValueRef(state).setValue(typedValue.getValue());
    return typedValue;
  }

  @Override
  public boolean isWritable(ExpressionState state) throws EvaluationException {
    return getValueRef(state).isWritable();
  }

  @Override
  public String toStringAST() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < getChildCount(); i++) {
      sb.append(getChild(i).toStringAST());
      if (i < getChildCount() - 1) {
        SpelNode nextChild = getChild(i + 1);
        // Don't append a '.' if the next child is an Indexer.
        // For example, we want 'myVar[0]' instead of 'myVar.[0]'.
        if (!(nextChild instanceof Indexer)) {
          sb.append('.');
        }
      }
    }
    return sb.toString();
  }

  @Override
  public boolean isCompilable() {
    for (SpelNodeImpl child : this.children) {
      if (!child.isCompilable()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    for (SpelNodeImpl child : this.children) {
      child.generateCode(mv, cf);
    }
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

}
