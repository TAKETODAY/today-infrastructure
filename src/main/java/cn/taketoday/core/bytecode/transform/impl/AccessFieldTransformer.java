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

import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.core.bytecode.core.CodeEmitter;
import cn.taketoday.core.bytecode.transform.ClassEmitterTransformer;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY <br>
 * 2019-09-04 19:57
 */
public class AccessFieldTransformer extends ClassEmitterTransformer {

  private final Callback callback;

  public AccessFieldTransformer(Callback callback) {
    this.callback = callback;
  }

  public interface Callback {
    String getPropertyName(Type owner, String fieldName);
  }

  @Override
  public void declare_field(int access, final String name, Type type, Object value) {
    super.declare_field(access, name, type, value);

    String property = StringUtils.capitalize(callback.getPropertyName(getClassType(), name));
    if (property != null) {
      CodeEmitter e;
      e = beginMethod(Opcodes.ACC_PUBLIC, new MethodSignature(type, "get" + property, Type.EMPTY_ARRAY));
      e.loadThis();
      e.getField(name);
      e.returnValue();
      e.end_method();

      e = beginMethod(Opcodes.ACC_PUBLIC, new MethodSignature(Type.VOID_TYPE, "set" + property, type));
      e.loadThis();
      e.loadArg(0);
      e.putField(name);
      e.returnValue();
      e.end_method();
    }
  }
}
