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
package cn.taketoday.cglib.core;

import java.lang.reflect.Modifier;

import cn.taketoday.asm.Label;
import cn.taketoday.asm.MethodVisitor;
import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.Type;
import cn.taketoday.asm.commons.GeneratorAdapter;
import cn.taketoday.asm.commons.MethodSignature;

/**
 * @author Juozas Baliuka, Chris Nokleberg
 */
public class CodeEmitter extends GeneratorAdapter {

  private final ClassEmitter ce;
  private final SimpleMethodInfo methodInfo;

  CodeEmitter(ClassEmitter ce, MethodVisitor mv, int access, MethodSignature sig, Type[] exceptionTypes) {
    super(access, sig, mv);
    this.ce = ce;
    this.methodInfo = new SimpleMethodInfo(
            ce.getClassInfo(), access, sig, exceptionTypes);
  }

  public CodeEmitter(CodeEmitter wrap) {
    super(wrap);
    this.ce = wrap.ce;
    this.methodInfo = wrap.methodInfo;
  }

  public boolean isStaticHook() {
    return false;
  }

  public MethodSignature getSignature() {
    return methodInfo.getSignature();
  }

  public MethodInfo getMethodInfo() {
    return methodInfo;
  }

  public ClassEmitter getClassEmitter() {
    return ce;
  }

  public void end_method() {
    visitMaxs(0, 0);
  }

  public Block begin_block() {
    return new Block(this);
  }

  public void catchException(Block block, Type exception) {
    if (block.getEnd() == null) {
      throw new IllegalStateException("end of block is unset");
    }
    mv.visitTryCatchBlock(block.getStart(), block.getEnd(), mark(), exception.getInternalName());
  }

  public void getField(String name) {
    ClassEmitter.FieldInfo info = ce.getFieldInfo(name);
    int opcode = Modifier.isStatic(info.access) ? Opcodes.GETSTATIC : Opcodes.GETFIELD;
    fieldInsn(opcode, ce.getClassType(), name, info.type);
  }

  public void putField(String name) {
    ClassEmitter.FieldInfo info = ce.getFieldInfo(name);
    int opcode = Modifier.isStatic(info.access) ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD;
    fieldInsn(opcode, ce.getClassType(), name, info.type);
  }

  public void super_getfield(String name, Type type) {
    fieldInsn(Opcodes.GETFIELD, ce.getSuperType(), name, type);
  }

  public void super_putfield(String name, Type type) {
    fieldInsn(Opcodes.PUTFIELD, ce.getSuperType(), name, type);
  }

  public void super_getstatic(String name, Type type) {
    fieldInsn(Opcodes.GETSTATIC, ce.getSuperType(), name, type);
  }

  public void super_putstatic(String name, Type type) {
    fieldInsn(Opcodes.PUTSTATIC, ce.getSuperType(), name, type);
  }

  public void super_invoke() {
    super_invoke(methodInfo.getSignature());
  }

  public void super_invoke(MethodSignature sig) {
    invokeInsn(Opcodes.INVOKESPECIAL, ce.getSuperType(), sig, false);
  }

  public void super_invoke_constructor() {
    invokeConstructor(ce.getSuperType());
  }

  public void invoke_constructor_this() {
    invokeConstructor(ce.getClassType());
  }

  public void invokeStatic(Type owner, MethodSignature sig, boolean isInterface) {
    invokeInsn(Opcodes.INVOKESTATIC, owner, sig, isInterface);
  }

  public void invoke_virtual_this(MethodSignature sig) {
    invokeVirtual(ce.getClassType(), sig);
  }

  public void invoke_static_this(MethodSignature sig) {
    invokeStatic(ce.getClassType(), sig);
  }

  public void invoke_constructor_this(MethodSignature sig) {
    invokeConstructor(ce.getClassType(), sig);
  }

  public void super_invoke_constructor(MethodSignature sig) {
    invokeConstructor(ce.getSuperType(), sig);
  }

  public void new_instance_this() {
    newInstance(ce.getClassType());
  }

  public void checkcast_this() {
    checkCast(ce.getClassType());
  }

  public void instance_of_this() {
    instanceOf(ce.getClassType());
  }

//  /**
//   * Toggles the integer on the top of the stack from 1 to 0 or vice versa
//   */
//  public void not() {
//    push(1);
//    math(XOR, Type.INT_TYPE);
//  }

  /**
   * Pushes a zero onto the stack if the argument is a primitive class, or a null
   * otherwise.
   */
  public void zero_or_null(Type type) {
    if (type.isPrimitive()) {
      switch (type.getSort()) {
        case Type.DOUBLE:
          push(0d);
          break;
        case Type.LONG:
          push(0L);
          break;
        case Type.FLOAT:
          push(0f);
          break;
        case Type.VOID:
          aconst_null();
        default:
          push(0);
      }
    }
    else {
      aconst_null();
    }
  }

  /**
   * Unboxes the object on the top of the stack. If the object is null, the
   * unboxed primitive value becomes zero.
   */
  public void unbox_or_zero(Type type) {
    if (type.isPrimitive()) {
      if (type != Type.VOID_TYPE) {
        Label nonNull = newLabel();
        Label end = newLabel();
        dup();
        ifNonNull(nonNull);
        pop();
        zero_or_null(type);
        goTo(end);
        mark(nonNull);
        unbox(type);
        mark(end);
      }
    }
    else {
      checkCast(type);
    }
  }

  @Override
  public void visitMaxs(int maxStack, int maxLocals) {
    if (!Modifier.isAbstract(methodInfo.getModifiers())) {
      super.visitMaxs(maxStack, maxLocals);
    }
  }

  public void invoke(MethodInfo method, Type virtualType) {
    ClassInfo classInfo = method.getClassInfo();
    Type type = classInfo.getType();
    MethodSignature sig = method.getSignature();
    if (MethodSignature.CONSTRUCTOR_NAME.equals(sig.getName())) {
      invokeConstructor(type, sig);
    }
    else if (Modifier.isStatic(method.getModifiers())) {
      invokeStatic(type, sig, Modifier.isInterface(classInfo.getModifiers()));
    }
    else if (Modifier.isInterface(classInfo.getModifiers())) {
      invokeInterface(type, sig);
    }
    else {
      invokeVirtual(virtualType, sig);
    }
  }

  public void invoke(MethodInfo method) {
    invoke(method, method.getClassInfo().getType());
  }

}
