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

import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelNode;

/**
 * An 'identifier' {@link SpelNode}.
 *
 * @author Andy Clement
 * @since 4.0
 */
public class Identifier extends SpelNodeImpl {

  private final TypedValue id;

  public Identifier(String payload, int startPos, int endPos) {
    super(startPos, endPos);
    this.id = new TypedValue(payload);
  }

  @Override
  public String toStringAST() {
    return String.valueOf(this.id.getValue());
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) {
    return this.id;
  }

}
