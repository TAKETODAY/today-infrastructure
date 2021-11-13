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
package cn.taketoday.core.bytecode.transform.impl;

import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.core.EmitUtils;
import cn.taketoday.core.bytecode.transform.ClassEmitterTransformer;

import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * @author TODAY
 */
public class AddPropertyTransformer extends ClassEmitterTransformer {
  private final String[] names;
  private final Type[] types;

  public AddPropertyTransformer(Map<String, Type> props) {
    int size = props.size();
    names = props.keySet().toArray(new String[size]);
    types = new Type[size];
    for (int i = 0; i < size; i++) {
      types[i] = props.get(names[i]);
    }
  }

  public AddPropertyTransformer(String[] names, Type[] types) {
    this.names = names;
    this.types = types;
  }

  @Override
  public void endClass() {
    if (!Modifier.isAbstract(getAccess())) {
      EmitUtils.addProperties(this, names, types);
    }
    super.endClass();
  }
}
