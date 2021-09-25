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
import java.util.List;

import cn.taketoday.asm.Opcodes;
import cn.taketoday.cglib.core.ClassEmitter;
import cn.taketoday.cglib.core.CodeEmitter;
import cn.taketoday.cglib.core.EmitUtils;
import cn.taketoday.cglib.core.MethodInfo;

/**
 * @author TODAY <br>
 * 2019-09-03 18:57
 */
@SuppressWarnings("all") final class NoOpGenerator implements CallbackGenerator {
  public static final NoOpGenerator INSTANCE = new NoOpGenerator();

  public static boolean isBridge(int access) {
    return (Opcodes.ACC_BRIDGE & access) != 0;
  }

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

  public void generateStatic(CodeEmitter e, Context context, List methods) { }
}
