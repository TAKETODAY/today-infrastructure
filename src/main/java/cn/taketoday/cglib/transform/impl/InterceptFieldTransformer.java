/*
 * Copyright 2003 The Apache Software Foundation
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
package cn.taketoday.cglib.transform.impl;

import java.lang.reflect.Modifier;

import cn.taketoday.asm.Label;
import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.Type;
import cn.taketoday.asm.commons.MethodSignature;
import cn.taketoday.cglib.core.CodeEmitter;
import cn.taketoday.asm.commons.Local;
import cn.taketoday.cglib.transform.ClassEmitterTransformer;
import cn.taketoday.util.StringUtils;

/**
 * @author Juozas Baliuka, Chris Nokleberg
 */
public class InterceptFieldTransformer extends ClassEmitterTransformer {

  private static final String CALLBACK_FIELD = "$todayReadWriteCallback";

  private static final Type ENABLED = Type.fromClass(InterceptFieldEnabled.class);
  private static final Type CALLBACK = Type.fromClass(InterceptFieldCallback.class);

  private static final MethodSignature ENABLED_SET = new MethodSignature(Type.VOID_TYPE, "setInterceptFieldCallback", CALLBACK);
  private static final MethodSignature ENABLED_GET = new MethodSignature(CALLBACK, "getInterceptFieldCallback");

  private final InterceptFieldFilter filter;

  public InterceptFieldTransformer(InterceptFieldFilter filter) {
    this.filter = filter;
  }

  public void beginClass(
          int version, int access, String className, Type superType, Type[] interfaces, String sourceFile) {
    if (!Modifier.isInterface(access)) {
      super.beginClass(version, access, className, superType, Type.add(interfaces, ENABLED), sourceFile);

      super.declare_field(Opcodes.ACC_PRIVATE | Opcodes.ACC_TRANSIENT, CALLBACK_FIELD, CALLBACK, null);

      CodeEmitter emitter = super.beginMethod(Opcodes.ACC_PUBLIC, ENABLED_GET);
      emitter.load_this();
      emitter.getField(CALLBACK_FIELD);
      emitter.return_value();
      emitter.end_method();

      emitter = super.beginMethod(Opcodes.ACC_PUBLIC, ENABLED_SET);
      emitter.load_this();
      emitter.load_arg(0);
      emitter.putField(CALLBACK_FIELD);
      emitter.return_value();
      emitter.end_method();
    }
    else {
      super.beginClass(version, access, className, superType, interfaces, sourceFile);
    }
  }

  public void declare_field(int access, String name, Type type, Object value) {
    super.declare_field(access, name, type, value);
    if (!Modifier.isStatic(access)) {
      if (filter.acceptRead(getClassType(), name)) {
        addReadMethod(name, type);
      }
      if (filter.acceptWrite(getClassType(), name)) {
        addWriteMethod(name, type);
      }
    }
  }

  private void addReadMethod(String name, Type type) {
    CodeEmitter e = super.beginMethod(Opcodes.ACC_PUBLIC, readMethodSig(name, type.getDescriptor()));
    e.load_this();
    e.getField(name);
    e.load_this();
    e.invokeInterface(ENABLED, ENABLED_GET);
    Label intercept = e.newLabel();
    e.ifNonNull(intercept);
    e.return_value();

    e.mark(intercept);
    Local result = e.newLocal(type);
    e.storeLocal(result);
    e.load_this();
    e.invokeInterface(ENABLED, ENABLED_GET);
    e.load_this();
    e.push(name);
    e.loadLocal(result);
    e.invokeInterface(CALLBACK, readCallbackSig(type));
    if (!type.isPrimitive()) {
      e.checkCast(type);
    }
    e.return_value();
    e.end_method();
  }

  private void addWriteMethod(String name, Type type) {
    CodeEmitter e = super.beginMethod(Opcodes.ACC_PUBLIC, writeMethodSig(name, type.getDescriptor()));
    e.load_this();
    e.dup();
    e.invokeInterface(ENABLED, ENABLED_GET);
    Label skip = e.newLabel();
    e.ifNull(skip);

    e.load_this();
    e.invokeInterface(ENABLED, ENABLED_GET);
    e.load_this();
    e.push(name);
    e.load_this();
    e.getField(name);
    e.load_arg(0);
    e.invokeInterface(CALLBACK, writeCallbackSig(type));
    if (!type.isPrimitive()) {
      e.checkCast(type);
    }
    Label go = e.newLabel();
    e.goTo(go);
    e.mark(skip);
    e.load_arg(0);
    e.mark(go);
    e.putField(name);
    e.return_value();
    e.end_method();
  }

  @Override
  public CodeEmitter beginMethod(int access, MethodSignature sig, Type... exceptions) {

    return new CodeEmitter(super.beginMethod(access, sig, exceptions)) {

      @Override
      public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        Type towner = Type.fromInternalName(owner);
        switch (opcode) {
          case Opcodes.GETFIELD:
            if (filter.acceptRead(towner, name)) {
              helper(towner, readMethodSig(name, desc));
              return;
            }
            break;
          case Opcodes.PUTFIELD:
            if (filter.acceptWrite(towner, name)) {
              helper(towner, writeMethodSig(name, desc));
              return;
            }
            break;
        }
        super.visitFieldInsn(opcode, owner, name, desc);
      }

      private void helper(Type owner, MethodSignature sig) {
        invokeVirtual(owner, sig);
      }
    };
  }

  private static MethodSignature readMethodSig(String name, String desc) {
    return new MethodSignature("$cglib_read_" + name, "()" + desc);
  }

  private static MethodSignature writeMethodSig(String name, String desc) {
    return new MethodSignature("$cglib_write_" + name, "(" + desc + ")V");
  }

  private static MethodSignature readCallbackSig(Type type) {
    Type remap = remap(type);
    return new MethodSignature(
            remap, "read" + callbackName(remap),
            Type.TYPE_OBJECT, Type.TYPE_STRING, remap
    );
  }

  private static MethodSignature writeCallbackSig(Type type) {
    Type remap = remap(type);
    return new MethodSignature(
            remap, "write" + callbackName(remap),
            Type.TYPE_OBJECT, Type.TYPE_STRING, remap, remap
    );
  }

  private static Type remap(Type type) {
    switch (type.getSort()) {
      case Type.OBJECT:
      case Type.ARRAY:
        return Type.TYPE_OBJECT;
      default:
        return type;
    }
  }

  private static String callbackName(Type type) {
    return (type == Type.TYPE_OBJECT) ? "Object" : StringUtils.capitalize(type.getClassName());
  }
}
