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

import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.core.bytecode.core.ClassEmitter;
import cn.taketoday.core.bytecode.core.CodeEmitter;
import cn.taketoday.core.bytecode.core.MethodInfo;

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
