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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.core.bytecode.core.Block;
import cn.taketoday.core.bytecode.core.CodeEmitter;
import cn.taketoday.core.bytecode.core.EmitUtils;
import cn.taketoday.core.bytecode.transform.ClassEmitterTransformer;

/**
 * @author Today <br>
 * 2018-11-08 15:07
 */
public class UndeclaredThrowableTransformer extends ClassEmitterTransformer {

  private final Type wrapper;

  public UndeclaredThrowableTransformer(Class<?> wrapper) {
    this.wrapper = Type.fromClass(wrapper);
    Constructor<?>[] cstructs = wrapper.getConstructors();
    for (final Constructor<?> cstruct : cstructs) {
      Class<?>[] types = cstruct.getParameterTypes();
      if (types.length == 1 && types[0].equals(Throwable.class)) {
        return;
      }
    }
    throw new IllegalArgumentException(
            wrapper + " does not have a single-arg constructor that takes a Throwable");
  }

  @Override
  public CodeEmitter beginMethod(int access, final MethodSignature sig, final Type... exceptions) {

    final CodeEmitter e = super.beginMethod(access, sig, exceptions);
    if (Modifier.isAbstract(access) || sig.equals(MethodSignature.SIG_STATIC)) {
      return e;
    }
    return new CodeEmitter(e) {

      private final Block handler = begin_block();

      @Override
      public void visitMaxs(int maxStack, int maxLocals) {
        handler.end();
        EmitUtils.wrapUndeclaredThrowable(this, handler, exceptions, wrapper);
        super.visitMaxs(maxStack, maxLocals);
      }
    };
  }
}
