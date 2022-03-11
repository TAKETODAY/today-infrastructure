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

import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.lang.Nullable;

/**
 * Represents a dot separated sequence of strings that indicate a package qualified type
 * reference.
 *
 * <p>Example: "java.lang.String" as in the expression "new java.lang.String('hello')"
 *
 * @author Andy Clement
 * @since 4.0
 */
public class QualifiedIdentifier extends SpelNodeImpl {

  @Nullable
  private TypedValue value;

  public QualifiedIdentifier(int startPos, int endPos, SpelNodeImpl... operands) {
    super(startPos, endPos, operands);
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    // Cache the concatenation of child identifiers
    if (this.value == null) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < getChildCount(); i++) {
        Object value = this.children[i].getValueInternal(state).getValue();
        if (i > 0 && (value == null || !value.toString().startsWith("$"))) {
          sb.append('.');
        }
        sb.append(value);
      }
      this.value = new TypedValue(sb.toString());
    }
    return this.value;
  }

  @Override
  public String toStringAST() {
    StringBuilder sb = new StringBuilder();
    if (this.value != null) {
      sb.append(this.value.getValue());
    }
    else {
      for (int i = 0; i < getChildCount(); i++) {
        if (i > 0) {
          sb.append('.');
        }
        sb.append(getChild(i).toStringAST());
      }
    }
    return sb.toString();
  }

}
