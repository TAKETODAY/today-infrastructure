/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.expression.lang;

import cn.taketoday.expression.ValueExpression;
import cn.taketoday.expression.VariableMapper;

/**
 * Creates a VariableMapper for the variables used in the expression.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/25 16:24
 */
public class VariableMapperFactory extends VariableMapper {

  private final VariableMapper target;
  private VariableMapper momento;

  public VariableMapperFactory(VariableMapper target) {
    if (target == null) {
      throw new NullPointerException("Target VariableMapper cannot be null");
    }
    this.target = target;
  }

  public VariableMapper create() {
    return this.momento;
  }

  public ValueExpression resolveVariable(String variable) {
    ValueExpression expr = this.target.resolveVariable(variable);
    if (expr != null) {
      if (this.momento == null) {
        this.momento = new VariableMapperImpl();
      }
      this.momento.setVariable(variable, expr);
    }
    return expr;
  }

  public ValueExpression setVariable(String variable, ValueExpression expression) {
    throw new UnsupportedOperationException("Cannot Set Variables on Factory");
  }
}
