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
package infra.bytecode.proxy;

import java.lang.reflect.Modifier;
import java.util.List;

import infra.bytecode.Opcodes;
import infra.bytecode.core.ClassEmitter;
import infra.bytecode.core.CodeEmitter;
import infra.bytecode.core.EmitUtils;
import infra.bytecode.core.MethodInfo;

/**
 * @author TODAY <br>
 * 2019-09-03 18:57
 */
@SuppressWarnings("all")
final class NoOpGenerator implements CallbackGenerator {
  public static final NoOpGenerator INSTANCE = new NoOpGenerator();

  public static boolean isBridge(int access) {
    return (Opcodes.ACC_BRIDGE & access) != 0;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public void generate(ClassEmitter ce, Context context, List methods) {

    for (Object object : methods) {
      MethodInfo method = (MethodInfo) object;
      if (isBridge(method.getModifiers()) //
              || (
              Modifier.isProtected(context.getOriginalModifiers(method))
                      && Modifier.isPublic(method.getModifiers()))) {

        CodeEmitter e = EmitUtils.beginMethod(ce, method);
        e.loadThis();
        context.emitLoadArgsAndInvoke(e, method);
        e.returnValue();
        e.end_method();
      }
    }

  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void generateStatic(CodeEmitter e, Context context, List methods) { }
}
