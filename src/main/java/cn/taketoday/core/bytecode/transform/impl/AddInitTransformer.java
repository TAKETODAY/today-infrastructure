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

import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.core.bytecode.core.CodeEmitter;
import cn.taketoday.core.bytecode.core.MethodInfo;
import cn.taketoday.core.bytecode.transform.ClassEmitterTransformer;

/**
 * @author Mark Hobson
 */
public class AddInitTransformer extends ClassEmitterTransformer {

  private final MethodInfo info;

  public AddInitTransformer(Method method) {
    info = MethodInfo.from(method);

    Type[] types = info.getSignature().getArgumentTypes();
    if (types.length != 1 || !types[0].equals(Type.TYPE_OBJECT) || !info.getSignature().getReturnType().equals(Type.VOID_TYPE)) {
      throw new IllegalArgumentException(method + " illegal signature");
    }
  }

  @Override
  public CodeEmitter beginMethod(int access, MethodSignature sig, Type... exceptions) {

    final CodeEmitter emitter = super.beginMethod(access, sig, exceptions);
    if (sig.getName().equals(MethodSignature.CONSTRUCTOR_NAME)) {
      return new CodeEmitter(emitter) {
        @Override
        public void visitInsn(int opcode) {
          if (opcode == Opcodes.RETURN) {
            loadThis();
            invoke(info);
          }
          super.visitInsn(opcode);
        }
      };
    }
    return emitter;
  }
}
