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
package cn.taketoday.core.bytecode.transform.impl;

import java.lang.reflect.Modifier;

import cn.taketoday.core.bytecode.Label;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.Local;
import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.core.bytecode.core.CodeEmitter;
import cn.taketoday.core.bytecode.transform.ClassEmitterTransformer;
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
      emitter.loadThis();
      emitter.getField(CALLBACK_FIELD);
      emitter.returnValue();
      emitter.end_method();

      emitter = super.beginMethod(Opcodes.ACC_PUBLIC, ENABLED_SET);
      emitter.loadThis();
      emitter.loadArg(0);
      emitter.putField(CALLBACK_FIELD);
      emitter.returnValue();
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
    e.loadThis();
    e.getField(name);
    e.loadThis();
    e.invokeInterface(ENABLED, ENABLED_GET);
    Label intercept = e.newLabel();
    e.ifNonNull(intercept);
    e.returnValue();

    e.mark(intercept);
    Local result = e.newLocal(type);
    e.storeLocal(result);
    e.loadThis();
    e.invokeInterface(ENABLED, ENABLED_GET);
    e.loadThis();
    e.push(name);
    e.loadLocal(result);
    e.invokeInterface(CALLBACK, readCallbackSig(type));
    if (!type.isPrimitive()) {
      e.checkCast(type);
    }
    e.returnValue();
    e.end_method();
  }

  private void addWriteMethod(String name, Type type) {
    CodeEmitter e = super.beginMethod(Opcodes.ACC_PUBLIC, writeMethodSig(name, type.getDescriptor()));
    e.loadThis();
    e.dup();
    e.invokeInterface(ENABLED, ENABLED_GET);
    Label skip = e.newLabel();
    e.ifNull(skip);

    e.loadThis();
    e.invokeInterface(ENABLED, ENABLED_GET);
    e.loadThis();
    e.push(name);
    e.loadThis();
    e.getField(name);
    e.loadArg(0);
    e.invokeInterface(CALLBACK, writeCallbackSig(type));
    if (!type.isPrimitive()) {
      e.checkCast(type);
    }
    Label go = e.newLabel();
    e.goTo(go);
    e.mark(skip);
    e.loadArg(0);
    e.mark(go);
    e.putField(name);
    e.returnValue();
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
    return new MethodSignature("$today_read_" + name, "()" + desc);
  }

  private static MethodSignature writeMethodSig(String name, String desc) {
    return new MethodSignature("$today_write_" + name, "(" + desc + ")V");
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
    return switch (type.getSort()) {
      case Type.OBJECT, Type.ARRAY -> Type.TYPE_OBJECT;
      default -> type;
    };
  }

  private static String callbackName(Type type) {
    return (type == Type.TYPE_OBJECT) ? "Object" : StringUtils.capitalize(type.getClassName());
  }
}
