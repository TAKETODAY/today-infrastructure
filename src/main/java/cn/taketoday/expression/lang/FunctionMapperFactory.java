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

import java.lang.reflect.Method;

import cn.taketoday.expression.FunctionMapper;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Jacob Hookom [jacob@hookom.net]
 * @since 4.0 2021/12/25 16:29
 */
public class FunctionMapperFactory extends FunctionMapper {

  protected FunctionMapperImpl memento = null;
  protected FunctionMapper target;

  public FunctionMapperFactory(FunctionMapper mapper) {
    if (mapper == null) {
      throw new NullPointerException("FunctionMapper target cannot be null");
    }
    this.target = mapper;
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.el.FunctionMapper#resolveFunction(java.lang.String,
   * java.lang.String)
   */
  public Method resolveFunction(String prefix, String localName) {
    if (this.memento == null) {
      this.memento = new FunctionMapperImpl();
    }
    Method m = this.target.resolveFunction(prefix, localName);
    if (m != null) {
      this.memento.addFunction(prefix, localName, m);
    }
    return m;
  }

  public FunctionMapper create() {
    return this.memento;
  }

}
