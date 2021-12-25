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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.expression.ValueExpression;
import cn.taketoday.expression.VariableMapper;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class VariableMapperImpl extends VariableMapper implements Externalizable {

  @Serial
  private static final long serialVersionUID = 1L;

  private Map<String, ValueExpression> vars = new HashMap<>();

  public VariableMapperImpl() {
    super();
  }

  public ValueExpression resolveVariable(String variable) {
    return this.vars.get(variable);
  }

  public ValueExpression setVariable(
          String variable, ValueExpression expression) {
    return this.vars.put(variable, expression);
  }

  // Safe cast.
  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.vars = (Map<String, ValueExpression>) in.readObject();
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(this.vars);
  }
}
