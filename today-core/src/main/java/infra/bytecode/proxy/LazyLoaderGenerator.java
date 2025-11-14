/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */
package infra.bytecode.proxy;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import infra.bytecode.Label;
import infra.bytecode.Opcodes;
import infra.bytecode.Type;
import infra.bytecode.commons.MethodSignature;
import infra.bytecode.core.ClassEmitter;
import infra.bytecode.core.CodeEmitter;
import infra.bytecode.core.MethodInfo;

/**
 * @author TODAY
 * @since 2019-09-03 19:17
 */
final class LazyLoaderGenerator implements CallbackGenerator {

  public static final LazyLoaderGenerator INSTANCE = new LazyLoaderGenerator();

  @Override
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

      ce.declare_field(Opcodes.ACC_PRIVATE, delegate, Type.TYPE_OBJECT, null);

      CodeEmitter e = ce.beginMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNCHRONIZED | Opcodes.ACC_FINAL, loadMethod(index));

      e.loadThis();
      e.getField(delegate);
      e.dup();
      Label end = e.newLabel();
      e.ifNonNull(end);
      e.pop();
      e.loadThis();
      context.emitCallback(e, index);
      e.invokeInterface(Type.forClass(LazyLoader.class), MethodSignature.from("Object loadObject()"));
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

  @Override
  public void generateStatic(CodeEmitter e, Context context, List<MethodInfo> methods) {
  }

}
