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
package cn.taketoday.bytecode.tree.analysis;

import cn.taketoday.bytecode.ClassWriter;
import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.tree.MethodNode;
import cn.taketoday.bytecode.ClassFile;

/**
 * A builder of {@link MethodNode}, to construct test cases for unit tests.
 *
 * @author Eric Bruneton
 */
final class MethodNodeBuilder {

  private final MethodNode methodNode;

  MethodNodeBuilder() {
    this(10, 10);
  }

  MethodNodeBuilder(final int maxStack, final int maxLocals) {
    this("()V", maxStack, maxLocals);
  }

  MethodNodeBuilder(final String descriptor, final int maxStack, final int maxLocals) {
    methodNode = new MethodNode(Opcodes.ACC_PUBLIC, "m", descriptor, null, null);
    methodNode.maxStack = maxStack;
    methodNode.maxLocals = maxLocals;
    methodNode.visitCode();
  }

  MethodNodeBuilder insn(final int opcode) {
    methodNode.visitInsn(opcode);
    return this;
  }

  MethodNodeBuilder intInsn(final int opcode, final int operand) {
    methodNode.visitIntInsn(opcode, operand);
    return this;
  }

  MethodNodeBuilder typeInsn(final int opcode, final String operand) {
    methodNode.visitTypeInsn(opcode, operand);
    return this;
  }

  MethodNodeBuilder methodInsn(
          final int opcode,
          final String owner,
          final String name,
          final String descriptor,
          final boolean isInterface) {
    methodNode.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    return this;
  }

  MethodNodeBuilder multiANewArrayInsn(final String descriptor, final int numDimensions) {
    methodNode.visitMultiANewArrayInsn(descriptor, numDimensions);
    return this;
  }

  MethodNodeBuilder nop() {
    methodNode.visitInsn(Opcodes.NOP);
    return this;
  }

  MethodNodeBuilder push() {
    methodNode.visitInsn(Opcodes.ICONST_0);
    return this;
  }

  MethodNodeBuilder pop() {
    methodNode.visitInsn(Opcodes.POP);
    return this;
  }

  MethodNodeBuilder iconst_0() {
    methodNode.visitInsn(Opcodes.ICONST_0);
    return this;
  }

  MethodNodeBuilder istore(final int var) {
    methodNode.visitVarInsn(Opcodes.ISTORE, var);
    return this;
  }

  MethodNodeBuilder aload(final int var) {
    methodNode.visitVarInsn(Opcodes.ALOAD, var);
    return this;
  }

  MethodNodeBuilder iload(final int var) {
    methodNode.visitVarInsn(Opcodes.ILOAD, var);
    return this;
  }

  MethodNodeBuilder astore(final int var) {
    methodNode.visitVarInsn(Opcodes.ASTORE, var);
    return this;
  }

  MethodNodeBuilder ret(final int var) {
    methodNode.visitVarInsn(Opcodes.RET, var);
    return this;
  }

  MethodNodeBuilder athrow() {
    methodNode.visitInsn(Opcodes.ATHROW);
    return this;
  }

  MethodNodeBuilder aconst_null() {
    methodNode.visitInsn(Opcodes.ACONST_NULL);
    return this;
  }

  MethodNodeBuilder areturn() {
    methodNode.visitInsn(Opcodes.ARETURN);
    return this;
  }

  MethodNodeBuilder vreturn() {
    methodNode.visitInsn(Opcodes.RETURN);
    return this;
  }

  MethodNodeBuilder label(final Label label) {
    methodNode.visitLabel(label);
    return this;
  }

  MethodNodeBuilder iinc(final int var, final int increment) {
    methodNode.visitIincInsn(var, increment);
    return this;
  }

  MethodNodeBuilder go(final Label label) {
    methodNode.visitJumpInsn(Opcodes.GOTO, label);
    return this;
  }

  MethodNodeBuilder jsr(final Label label) {
    methodNode.visitJumpInsn(Opcodes.JSR, label);
    return this;
  }

  MethodNodeBuilder ifnonnull(final Label label) {
    methodNode.visitJumpInsn(Opcodes.IFNONNULL, label);
    return this;
  }

  MethodNodeBuilder ifne(final Label label) {
    methodNode.visitJumpInsn(Opcodes.IFNE, label);
    return this;
  }

  MethodNodeBuilder trycatch(final Label start, final Label end, final Label handler) {
    return trycatch(start, end, handler, null);
  }

  MethodNodeBuilder trycatch(
          final Label start, final Label end, final Label handler, final String type) {
    methodNode.visitTryCatchBlock(start, end, handler, type);
    return this;
  }

  MethodNodeBuilder localVariable(
          final String name,
          final String descriptor,
          final String signature,
          final Label start,
          final Label end,
          final int index) {
    methodNode.visitLocalVariable(name, descriptor, signature, start, end, index);
    return this;
  }

  MethodNode build() {
    methodNode.visitEnd();
    return methodNode;
  }

  static ClassFile buildClassWithMethod(final MethodNode methodNode) {
    ClassWriter classWriter = new ClassWriter(0);
    classWriter.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, "C", null, "java/lang/Object", null);
    MethodVisitor methodVisitor =
            classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
    methodVisitor.visitCode();
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
    methodVisitor.visitMethodInsn(
            Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    methodVisitor.visitInsn(Opcodes.RETURN);
    methodVisitor.visitMaxs(1, 1);
    methodVisitor.visitEnd();
    methodNode.accept(classWriter);
    return new ClassFile(classWriter.toByteArray());
  }
}
