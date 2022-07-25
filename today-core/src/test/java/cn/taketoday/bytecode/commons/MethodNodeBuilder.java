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
package cn.taketoday.bytecode.commons;

import cn.taketoday.bytecode.ClassWriter;
import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.tree.MethodNode;
import cn.taketoday.bytecode.ClassFile;
import cn.taketoday.bytecode.util.Textifier;
import cn.taketoday.bytecode.util.TraceMethodVisitor;

/**
 * A builder of {@link MethodNode}, to construct test cases for unit tests.
 *
 * @author Eric Bruneton
 */
final class MethodNodeBuilder {

  private final MethodNode methodNode;

  MethodNodeBuilder(final int maxStack, final int maxLocals) {
    this("m", "()V", maxStack, maxLocals);
  }

  MethodNodeBuilder(
          final String name, final String descriptor, final int maxStack, final int maxLocals) {
    methodNode = new MethodNode(Opcodes.ACC_PUBLIC, name, descriptor, null, null);
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

  MethodNodeBuilder varInsn(final int opcode, final int operand) {
    methodNode.visitVarInsn(opcode, operand);
    return this;
  }

  MethodNodeBuilder typeInsn(final int opcode, final String operand) {
    methodNode.visitTypeInsn(opcode, operand);
    return this;
  }

  MethodNodeBuilder fieldInsn(
          final int opcode, final String owner, final String name, final String descriptor) {
    methodNode.visitFieldInsn(opcode, owner, name, descriptor);
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

  MethodNodeBuilder jumpInsn(final int opcode, final Label label) {
    methodNode.visitJumpInsn(opcode, label);
    return this;
  }

  MethodNodeBuilder ldcInsn(final Object value) {
    methodNode.visitLdcInsn(value);
    return this;
  }

  MethodNodeBuilder iconst_0() {
    methodNode.visitInsn(Opcodes.ICONST_0);
    return this;
  }

  MethodNodeBuilder pop() {
    methodNode.visitInsn(Opcodes.POP);
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

  MethodNodeBuilder vreturn() {
    methodNode.visitInsn(Opcodes.RETURN);
    return this;
  }

  MethodNodeBuilder label() {
    methodNode.visitLabel(new Label());
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

  MethodNodeBuilder switchto(
          final Label defaultLabel, final int key, final Label target, final boolean useTableSwitch) {
    if (useTableSwitch) {
      methodNode.visitTableSwitchInsn(key, key, defaultLabel, new Label[] { target });
    }
    else {
      methodNode.visitLookupSwitchInsn(defaultLabel, new int[] { key }, new Label[] { target });
    }
    return this;
  }

  MethodNodeBuilder switchto(final Label label0, final Label label1, final boolean useTableSwitch) {
    if (useTableSwitch) {
      methodNode.visitTableSwitchInsn(0, 1, label0, new Label[] { label0, label1 });
    }
    else {
      methodNode.visitLookupSwitchInsn(label0, new int[] { 1 }, new Label[] { label1 });
    }
    return this;
  }

  MethodNodeBuilder multiANewArrayInsn(final String descriptor, final int numDimensions) {
    methodNode.visitMultiANewArrayInsn(descriptor, numDimensions);
    return this;
  }

  MethodNodeBuilder trycatch(final Label start, final Label end, final Label handler) {
    methodNode.visitTryCatchBlock(start, end, handler, null);
    return this;
  }

  MethodNodeBuilder line(final int line, final Label start) {
    methodNode.visitLineNumber(line, start);
    return this;
  }

  MethodNodeBuilder localvar(
          final String name,
          final String descriptor,
          final int index,
          final Label start,
          final Label end) {
    methodNode.visitLocalVariable(name, descriptor, null, start, end, index);
    return this;
  }

  MethodNode build() {
    methodNode.visitEnd();
    return methodNode;
  }

  static String toText(final MethodNode methodNode) {
    Textifier textifier = new Textifier();
    methodNode.accept(new TraceMethodVisitor(textifier));

    StringBuilder stringBuilder = new StringBuilder();
    for (Object text : textifier.text) {
      stringBuilder.append(text);
    }
    return stringBuilder.toString();
  }

  static ClassFile buildClassWithMethod(final MethodNode methodNode) {
    ClassWriter classWriter = new ClassWriter(0);
    classWriter.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, "C", null, "java/lang/Object", null);
    classWriter.visitField(Opcodes.ACC_STATIC, "f", "[[I", null, null);
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
