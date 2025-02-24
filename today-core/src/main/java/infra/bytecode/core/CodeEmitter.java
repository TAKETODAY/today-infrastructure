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

package infra.bytecode.core;

import java.lang.reflect.Modifier;

import infra.bytecode.Label;
import infra.bytecode.MethodVisitor;
import infra.bytecode.Opcodes;
import infra.bytecode.Type;
import infra.bytecode.commons.GeneratorAdapter;
import infra.bytecode.commons.MethodSignature;

/**
 * @author Juozas Baliuka, Chris Nokleberg
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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

  public void super_invoke() {
    super_invoke(methodInfo.getSignature());
  }

  public void super_invoke(MethodSignature sig) {
    invokeInsn(Opcodes.INVOKESPECIAL, ce.getSuperType(), sig, false);
  }

  public void super_invoke_constructor() {
    invokeConstructor(ce.getSuperType());
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

  /**
   * Pushes a zero onto the stack if the argument is a primitive class, or a null
   * otherwise.
   */
  public void zero_or_null(Type type) {
    if (type.isPrimitive()) {
      switch (type.getSort()) {
        case Type.DOUBLE -> push(0d);
        case Type.LONG -> push(0L);
        case Type.FLOAT -> push(0f);
        case Type.VOID -> aconst_null();
        default -> push(0);
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
