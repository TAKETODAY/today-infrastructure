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

import java.lang.reflect.Method;
import java.util.HashSet;

import cn.taketoday.asm.ClassVisitor;
import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.Type;
import cn.taketoday.asm.commons.MethodSignature;
import cn.taketoday.cglib.core.ClassEmitter;
import cn.taketoday.cglib.core.CodeEmitter;
import cn.taketoday.cglib.core.EmitUtils;
import cn.taketoday.cglib.core.MethodInfo;
import cn.taketoday.cglib.core.MethodWrapper;

import static cn.taketoday.asm.Opcodes.ACC_PUBLIC;
import static cn.taketoday.asm.Opcodes.JAVA_VERSION;
import static cn.taketoday.core.Constant.SOURCE_FILE;

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

    beginClass(JAVA_VERSION, ACC_PUBLIC, className, MIXIN, Type.getTypes(getInterfaces(classes)), SOURCE_FILE);
    EmitUtils.nullConstructor(this);
    EmitUtils.factoryMethod(this, NEW_INSTANCE);

    declare_field(Opcodes.ACC_PRIVATE, FIELD_NAME, Type.TYPE_OBJECT_ARRAY, null);

    CodeEmitter e = beginMethod(ACC_PUBLIC, CSTRUCT_OBJECT_ARRAY);
    e.load_this();
    e.super_invoke_constructor();
    e.load_this();
    e.load_arg(0);
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
          int modifiers = ACC_PUBLIC;
          if ((methodInfo.getModifiers() & accVarargs) == accVarargs) {
            modifiers |= accVarargs;
          }
          e = EmitUtils.beginMethod(this, methodInfo, modifiers);
          e.load_this();
          e.getField(FIELD_NAME);
          e.aaload((route != null) ? route[i] : i);
          e.checkCast(methodInfo.getClassInfo().getType());
          e.load_args();
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
