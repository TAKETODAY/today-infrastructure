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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import infra.expression.EvaluationException;
import infra.expression.TypedValue;
import infra.expression.spel.ExpressionState;
import infra.expression.spel.SpelNode;
import infra.expression.spel.support.SimpleEvaluationContext;
import infra.util.Assert;

/**
 * Represent a map in an expression, e.g. '{name:'foo',age:12}'
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class InlineMap extends SpelNodeImpl {

  private final boolean isConstant;

  private volatile @Nullable TypedValue constant;

  public InlineMap(int startPos, int endPos, SpelNodeImpl... args) {
    super(startPos, endPos, args);
    this.isConstant = determineIfConstant();
  }

  /**
   * Determine whether this map is structurally eligible to be a constant
   * value: whether all of its components are themselves constants or lists/maps
   * that contain only constants.
   * <p>The actual constant value is created lazily on the first call to
   * {@link #getValueInternal(ExpressionState)}.
   */
  private boolean determineIfConstant() {
    int childCount = getChildCount();
    for (int c = 0; c < childCount; c++) {
      SpelNode child = getChild(c);
      if (child instanceof Literal) {
        continue;
      }
      if (child instanceof InlineList inlineList && inlineList.isConstant()) {
        continue;
      }
      if (child instanceof InlineMap inlineMap && inlineMap.isConstant()) {
        continue;
      }
      if (c % 2 == 0 && child instanceof PropertyOrFieldReference) {
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
    result = createMap(expressionState);
    if (this.isConstant) {
      this.constant = result;
    }
    return result;
  }

  private TypedValue createMap(ExpressionState expressionState) throws EvaluationException {
    expressionState.trackOperation();
    Map<Object, Object> map = new LinkedHashMap<>();
    int childCount = getChildCount();
    for (int c = 0; c < childCount; c++) {
      SpelNode keyChild = getChild(c++);
      Object key;
      if (keyChild instanceof PropertyOrFieldReference reference) {
        key = reference.getName();
      }
      else {
        key = keyChild.getValue(expressionState);
      }
      Object value = getChild(c).getValue(expressionState);
      expressionState.trackOperation();
      map.put(key, value);
    }
    return new TypedValue(this.isConstant ? Collections.unmodifiableMap(map) : map);
  }

  @Override
  public String toStringAST() {
    StringBuilder sb = new StringBuilder("{");
    int count = getChildCount();
    for (int c = 0; c < count; c++) {
      if (c > 0) {
        sb.append(',');
      }
      sb.append(getChild(c++).toStringAST());
      sb.append(':');
      sb.append(getChild(c).toStringAST());
    }
    sb.append('}');
    return sb.toString();
  }

  /**
   * Return whether this map is structurally a constant value.
   * <p>Note that the resulting constant value is created lazily on the
   * first call to {@link #getValueInternal(ExpressionState)} or
   * {@link #getConstantValue()}.
   */
  public boolean isConstant() {
    return this.isConstant;
  }

  /**
   * Return the cached constant {@link Map} value for this inline map,
   * lazily creating it on first access.
   *
   * @see #isConstant()
   * @deprecated as of 5.0; this method was only intended for testing purposes
   */
  @SuppressWarnings("unchecked")
  @Deprecated(since = "5.0")
  public @Nullable Map<Object, Object> getConstantValue() {
    Assert.state(this.isConstant, "Not a constant");
    TypedValue result = this.constant;
    if (result == null) {
      result = createMap(new ExpressionState(SimpleEvaluationContext.forReadOnlyDataBinding().build()));
      this.constant = result;
    }
    return (Map<Object, Object>) result.getValue();
  }

}
