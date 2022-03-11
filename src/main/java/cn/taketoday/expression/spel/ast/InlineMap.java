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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelNode;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Assert;

/**
 * Represent a map in an expression, e.g. '{name:'foo',age:12}'
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @since 4.0
 */
public class InlineMap extends SpelNodeImpl {

  // If the map is purely literals, it is a constant value and can be computed and cached
  @Nullable
  private TypedValue constant;

  public InlineMap(int startPos, int endPos, SpelNodeImpl... args) {
    super(startPos, endPos, args);
    checkIfConstant();
  }

  /**
   * If all the components of the map are constants, or lists/maps that themselves
   * contain constants, then a constant list can be built to represent this node.
   * This will speed up later getValue calls and reduce the amount of garbage created.
   */
  private void checkIfConstant() {
    boolean isConstant = true;
    for (int c = 0, max = getChildCount(); c < max; c++) {
      SpelNode child = getChild(c);
      if (!(child instanceof Literal)) {
        if (child instanceof InlineList inlineList) {
          if (!inlineList.isConstant()) {
            isConstant = false;
            break;
          }
        }
        else if (child instanceof InlineMap inlineMap) {
          if (!inlineMap.isConstant()) {
            isConstant = false;
            break;
          }
        }
        else if (!(c % 2 == 0 && child instanceof PropertyOrFieldReference)) {
          isConstant = false;
          break;
        }
      }
    }
    if (isConstant) {
      Map<Object, Object> constantMap = new LinkedHashMap<>();
      int childCount = getChildCount();
      for (int c = 0; c < childCount; c++) {
        SpelNode keyChild = getChild(c++);
        SpelNode valueChild = getChild(c);
        Object key = null;
        Object value = null;
        if (keyChild instanceof Literal literal) {
          key = literal.getLiteralValue().getValue();
        }
        else if (keyChild instanceof PropertyOrFieldReference propertyOrFieldReference) {
          key = propertyOrFieldReference.getName();
        }
        else {
          return;
        }
        if (valueChild instanceof Literal literal) {
          value = literal.getLiteralValue().getValue();
        }
        else if (valueChild instanceof InlineList inlineList) {
          value = inlineList.getConstantValue();
        }
        else if (valueChild instanceof InlineMap inlineMap) {
          value = inlineMap.getConstantValue();
        }
        constantMap.put(key, value);
      }
      this.constant = new TypedValue(Collections.unmodifiableMap(constantMap));
    }
  }

  @Override
  public TypedValue getValueInternal(ExpressionState expressionState) throws EvaluationException {
    if (this.constant != null) {
      return this.constant;
    }
    else {
      Map<Object, Object> returnValue = new LinkedHashMap<>();
      int childcount = getChildCount();
      for (int c = 0; c < childcount; c++) {
        // TODO allow for key being PropertyOrFieldReference like Indexer on maps
        SpelNode keyChild = getChild(c++);
        Object key = null;
        if (keyChild instanceof PropertyOrFieldReference reference) {
          key = reference.getName();
        }
        else {
          key = keyChild.getValue(expressionState);
        }
        Object value = getChild(c).getValue(expressionState);
        returnValue.put(key, value);
      }
      return new TypedValue(returnValue);
    }
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
   * Return whether this list is a constant value.
   */
  public boolean isConstant() {
    return this.constant != null;
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public Map<Object, Object> getConstantValue() {
    Assert.state(this.constant != null, "No constant");
    return (Map<Object, Object>) this.constant.getValue();
  }

}
