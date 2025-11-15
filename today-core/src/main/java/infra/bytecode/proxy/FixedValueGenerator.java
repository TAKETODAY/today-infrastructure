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
