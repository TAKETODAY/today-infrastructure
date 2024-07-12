/*
 * Copyright 2017 - 2024 the original author or authors.
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
package cn.taketoday.bytecode.proxy;

import java.util.List;

import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.core.ClassEmitter;
import cn.taketoday.bytecode.core.CodeEmitter;
import cn.taketoday.bytecode.core.MethodInfo;
import cn.taketoday.bytecode.commons.MethodSignature;

/**
 * @author TODAY <br>
 * 2019-09-03 19:15
 */
final class FixedValueGenerator implements CallbackGenerator {

  public static final FixedValueGenerator INSTANCE = new FixedValueGenerator();

  private static final Type FIXED_VALUE = Type.forClass(FixedValue.class);
  private static final MethodSignature LOAD_OBJECT = MethodSignature.from("Object loadObject()");

  public void generate(final ClassEmitter ce, final Context context, final List<MethodInfo> methods) {

    for (final MethodInfo method : methods) {

      final CodeEmitter e = context.beginMethod(ce, method);
      context.emitCallback(e, context.getIndex(method));

      e.invokeInterface(FIXED_VALUE, LOAD_OBJECT);
      e.unbox_or_zero(e.getReturnType());
      e.returnValue();
      e.end_method();
    }
  }

  public void generateStatic(CodeEmitter e, Context context, List<MethodInfo> methods) {

  }
}
