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
package cn.taketoday.core.bytecode.proxy;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.core.bytecode.Label;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.core.bytecode.core.ClassEmitter;
import cn.taketoday.core.bytecode.core.CodeEmitter;
import cn.taketoday.core.bytecode.core.MethodInfo;

import static cn.taketoday.core.bytecode.Opcodes.ACC_FINAL;
import static cn.taketoday.core.bytecode.Opcodes.ACC_PRIVATE;
import static cn.taketoday.core.bytecode.Opcodes.ACC_SYNCHRONIZED;

/**
 * @author TODAY <br>
 * 2019-09-03 19:17
 */
final class LazyLoaderGenerator implements CallbackGenerator {

  public static final LazyLoaderGenerator INSTANCE = new LazyLoaderGenerator();

  private static final MethodSignature LOAD_OBJECT = MethodSignature.from("Object loadObject()");
  private static final Type LAZY_LOADER = Type.fromClass(LazyLoader.class);

  public void generate(ClassEmitter ce, Context context, List<MethodInfo> methods) {

    final Set<Integer> indexes = new HashSet<>();

    for (final MethodInfo method : methods) {
      // ignore protected methods
      if (!Modifier.isProtected(method.getModifiers())) {
        int index = context.getIndex(method);
        indexes.add(index);
        CodeEmitter e = context.beginMethod(ce, method);
        e.loadThis();
        e.dup();
        e.invoke_virtual_this(loadMethod(index));
        e.checkCast(method.getClassInfo().getType());
        e.loadArgs();
        e.invoke(method);
        e.returnValue();
        e.end_method();
      }
    }

    for (final int index : indexes) {

      final String delegate = "today$LazyLoader" + index;

      ce.declare_field(ACC_PRIVATE, delegate, Type.TYPE_OBJECT, null);

      CodeEmitter e = ce.beginMethod(ACC_PRIVATE | ACC_SYNCHRONIZED | ACC_FINAL, loadMethod(index));

      e.loadThis();
      e.getField(delegate);
      e.dup();
      Label end = e.newLabel();
      e.ifNonNull(end);
      e.pop();
      e.loadThis();
      context.emitCallback(e, index);
      e.invokeInterface(LAZY_LOADER, LOAD_OBJECT);
      e.dupX1();
      e.putField(delegate);
      e.mark(end);
      e.returnValue();
      e.end_method();
    }
  }

  private MethodSignature loadMethod(int index) {
    return new MethodSignature(Type.TYPE_OBJECT, "today$LoadPrivate" + index, Type.EMPTY_ARRAY);
  }

  public void generateStatic(CodeEmitter e, Context context, List<MethodInfo> methods) { }
}
