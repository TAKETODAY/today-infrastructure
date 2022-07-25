/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import java.lang.reflect.Method;
import java.util.HashSet;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.core.ClassEmitter;
import cn.taketoday.bytecode.core.CodeEmitter;
import cn.taketoday.bytecode.core.EmitUtils;
import cn.taketoday.bytecode.core.MethodInfo;
import cn.taketoday.bytecode.core.MethodWrapper;
import cn.taketoday.bytecode.commons.MethodSignature;

import static cn.taketoday.lang.Constant.SOURCE_FILE;

/**
 * @author Chris Nokleberg
 * @version $Id: MixinEmitter.java,v 1.9 2006/08/27 21:04:37 herbyderby Exp $
 */
class MixinEmitter extends ClassEmitter {

  private static final String FIELD_NAME = "today$Delegates";
  private static final Type MIXIN = Type.fromClass(Mixin.class);
  private static final MethodSignature CSTRUCT_OBJECT_ARRAY = MethodSignature.forConstructor("Object[]");

  private static final MethodSignature NEW_INSTANCE = new MethodSignature(MIXIN, "newInstance", Type.TYPE_OBJECT_ARRAY);

  public MixinEmitter(ClassVisitor v, String className, Class<?>[] classes, int[] route) {
    super(v);

    beginClass(Opcodes.JAVA_VERSION, Opcodes.ACC_PUBLIC, className, MIXIN, Type.getTypes(getInterfaces(classes)), SOURCE_FILE);
    EmitUtils.nullConstructor(this);
    EmitUtils.factoryMethod(this, NEW_INSTANCE);

    declare_field(Opcodes.ACC_PRIVATE, FIELD_NAME, Type.TYPE_OBJECT_ARRAY, null);

    CodeEmitter e = beginMethod(Opcodes.ACC_PUBLIC, CSTRUCT_OBJECT_ARRAY);
    e.loadThis();
    e.super_invoke_constructor();
    e.loadThis();
    e.loadArg(0);
    e.putField(FIELD_NAME);
    e.returnValue();
    e.end_method();

    final HashSet<Object> unique = new HashSet<>();
    final int accVarargs = Opcodes.ACC_VARARGS;

    for (int i = 0; i < classes.length; i++) {
      Method[] methods = getMethods(classes[i]);
      for (final Method method : methods) {
        if (unique.add(MethodWrapper.create(method))) {
          MethodInfo methodInfo = MethodInfo.from(method);
          int modifiers = Opcodes.ACC_PUBLIC;
          if ((methodInfo.getModifiers() & accVarargs) == accVarargs) {
            modifiers |= accVarargs;
          }
          e = EmitUtils.beginMethod(this, methodInfo, modifiers);
          e.loadThis();
          e.getField(FIELD_NAME);
          e.aaload((route != null) ? route[i] : i);
          e.checkCast(methodInfo.getClassInfo().getType());
          e.loadArgs();
          e.invoke(methodInfo);
          e.returnValue();
          e.end_method();
        }
      }
    }

    endClass();
  }

  protected Class<?>[] getInterfaces(Class<?>[] classes) {
    return classes;
  }

  protected Method[] getMethods(Class<?> type) {
    return type.getMethods();
  }
}
