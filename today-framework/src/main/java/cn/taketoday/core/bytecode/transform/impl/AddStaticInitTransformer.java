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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.core.CodeEmitter;
import cn.taketoday.core.bytecode.core.EmitUtils;
import cn.taketoday.core.bytecode.core.MethodInfo;
import cn.taketoday.core.bytecode.transform.ClassEmitterTransformer;

/**
 * @author Juozas Baliuka, Chris Nokleberg
 */
public class AddStaticInitTransformer extends ClassEmitterTransformer {

  private final MethodInfo info;

  public AddStaticInitTransformer(Method classInit) {
    info = MethodInfo.from(classInit);
    if (!Modifier.isStatic(info.getModifiers())) {
      throw new IllegalArgumentException(classInit + " is not static");
    }
    Type[] types = info.getSignature().getArgumentTypes();
    if (types.length != 1 || !types[0].equals(Type.TYPE_CLASS) || !info.getSignature().getReturnType().equals(Type.VOID_TYPE)) {
      throw new IllegalArgumentException(classInit + " illegal signature");
    }
  }

  @Override
  protected void init() {
    if (!Modifier.isInterface(getAccess())) {
      CodeEmitter e = getStaticHook();
      EmitUtils.loadClassThis(e);
      e.invoke(info);
    }
  }
}
