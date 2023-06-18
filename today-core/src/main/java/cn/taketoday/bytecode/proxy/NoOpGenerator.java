/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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
package cn.taketoday.bytecode.proxy;

import java.lang.reflect.Modifier;
import java.util.List;

import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.core.ClassEmitter;
import cn.taketoday.bytecode.core.CodeEmitter;
import cn.taketoday.bytecode.core.EmitUtils;
import cn.taketoday.bytecode.core.MethodInfo;

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
