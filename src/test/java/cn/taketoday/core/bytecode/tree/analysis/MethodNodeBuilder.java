// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package cn.taketoday.core.bytecode.tree.analysis;

import cn.taketoday.core.bytecode.ClassFile;
import cn.taketoday.core.bytecode.ClassWriter;
import cn.taketoday.core.bytecode.Label;
import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.tree.MethodNode;

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
