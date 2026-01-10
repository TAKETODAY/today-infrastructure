/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
