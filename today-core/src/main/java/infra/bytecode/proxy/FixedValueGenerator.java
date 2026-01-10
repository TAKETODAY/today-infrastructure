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

import java.util.List;

import infra.bytecode.Type;
import infra.bytecode.commons.MethodSignature;
import infra.bytecode.core.ClassEmitter;
import infra.bytecode.core.CodeEmitter;
import infra.bytecode.core.MethodInfo;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 2019-09-03 19:15
 */
final class FixedValueGenerator implements CallbackGenerator {

  public static final FixedValueGenerator INSTANCE = new FixedValueGenerator();

  @Override
  public void generate(final ClassEmitter ce, final Context context, final List<MethodInfo> methods) {
    for (final MethodInfo method : methods) {

      final CodeEmitter e = context.beginMethod(ce, method);
      context.emitCallback(e, context.getIndex(method));

      e.invokeInterface(Type.forClass(FixedValue.class), MethodSignature.from("Object loadObject()"));
      e.unbox_or_zero(e.getReturnType());
      e.returnValue();
      e.end_method();
    }
  }

  public void generateStatic(CodeEmitter e, Context context, List<MethodInfo> methods) {

  }
}
