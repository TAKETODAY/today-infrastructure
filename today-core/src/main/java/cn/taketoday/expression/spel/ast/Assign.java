/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

/**
 * Represents assignment. An alternative to calling {@code setValue}
 * for an expression which indicates an assign statement.
 *
 * <p>Example: 'someNumberProperty=42'
 *
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Assign extends SpelNodeImpl {

  public Assign(int startPos, int endPos, SpelNodeImpl... operands) {
    super(startPos, endPos, operands);
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    return this.children[0].setValueInternal(state, () -> this.children[1].getValueInternal(state));
  }

  @Override
  public String toStringAST() {
    return getChild(0).toStringAST() + "=" + getChild(1).toStringAST();
  }

}
