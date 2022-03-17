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

import java.util.List;

import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.core.bytecode.core.ClassEmitter;
import cn.taketoday.core.bytecode.core.CodeEmitter;
import cn.taketoday.core.bytecode.core.MethodInfo;

/**
 * @author TODAY <br>
 * 2019-09-03 19:15
 */
final class FixedValueGenerator implements CallbackGenerator {

  public static final FixedValueGenerator INSTANCE = new FixedValueGenerator();

  private static final Type FIXED_VALUE = Type.fromClass(FixedValue.class);
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
