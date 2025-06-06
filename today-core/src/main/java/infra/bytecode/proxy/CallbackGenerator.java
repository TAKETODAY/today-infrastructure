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

import java.util.List;

import infra.bytecode.commons.MethodSignature;
import infra.bytecode.core.ClassEmitter;
import infra.bytecode.core.CodeEmitter;
import infra.bytecode.core.MethodInfo;

/**
 * @author TODAY <br>
 * 2018-11-08 15:09
 */
interface CallbackGenerator {

  void generate(ClassEmitter ce, Context context, List<MethodInfo> methods) throws Exception;

  void generateStatic(CodeEmitter e, Context context, List<MethodInfo> methods) throws Exception;

  interface Context {

    ClassLoader getClassLoader();

    CodeEmitter beginMethod(ClassEmitter ce, MethodInfo method);

    int getOriginalModifiers(MethodInfo method);

    int getIndex(MethodInfo method);

    void emitCallback(CodeEmitter ce, int index);

    MethodSignature getImplSignature(MethodInfo method);

    void emitLoadArgsAndInvoke(CodeEmitter e, MethodInfo method);
  }
}
