/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.cglib.proxy;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.asm.Label;
import cn.taketoday.asm.Type;
import cn.taketoday.asm.commons.MethodSignature;
import cn.taketoday.cglib.core.ClassEmitter;
import cn.taketoday.cglib.core.CodeEmitter;
import cn.taketoday.cglib.core.MethodInfo;
import cn.taketoday.core.Constant;

import static cn.taketoday.asm.Opcodes.ACC_FINAL;
import static cn.taketoday.asm.Opcodes.ACC_PRIVATE;
import static cn.taketoday.asm.Opcodes.ACC_SYNCHRONIZED;

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
        e.load_this();
        e.dup();
        e.invoke_virtual_this(loadMethod(index));
        e.checkcast(method.getClassInfo().getType());
        e.load_args();
        e.invoke(method);
        e.return_value();
        e.end_method();
      }
    }

    for (final int index : indexes) {

      final String delegate = "today$LazyLoader" + index;

      ce.declare_field(ACC_PRIVATE, delegate, Type.TYPE_OBJECT, null);

      CodeEmitter e = ce.beginMethod(ACC_PRIVATE | ACC_SYNCHRONIZED | ACC_FINAL, loadMethod(index));

      e.load_this();
      e.getfield(delegate);
      e.dup();
      Label end = e.newLabel();
      e.ifNonNull(end);
      e.pop();
      e.load_this();
      context.emitCallback(e, index);
      e.invokeInterface(LAZY_LOADER, LOAD_OBJECT);
      e.dupX1();
      e.putfield(delegate);
      e.mark(end);
      e.return_value();
      e.end_method();
    }
  }

  private MethodSignature loadMethod(int index) {
    return new MethodSignature(Type.TYPE_OBJECT, "today$LoadPrivate" + index, Constant.TYPES_EMPTY_ARRAY);
  }

  public void generateStatic(CodeEmitter e, Context context, List<MethodInfo> methods) { }
}
