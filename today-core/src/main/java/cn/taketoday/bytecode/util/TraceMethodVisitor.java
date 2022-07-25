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
package cn.taketoday.bytecode.util;

import cn.taketoday.bytecode.AnnotationVisitor;
import cn.taketoday.bytecode.Attribute;
import cn.taketoday.bytecode.Handle;
import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.TypePath;

/**
 * A {@link MethodVisitor} that prints the methods it visits with a {@link Printer}.
 *
 * @author Eric Bruneton
 */
public final class TraceMethodVisitor extends MethodVisitor {

  /** The printer to convert the visited method into text. */
  // DontCheck(MemberName): can't be renamed (for backward binary compatibility).
  public final Printer p;

  /**
   * Constructs a new {@link TraceMethodVisitor}.
   *
   * @param printer the printer to convert the visited method into text.
   */
  public TraceMethodVisitor(final Printer printer) {
    this(null, printer);
  }

  /**
   * Constructs a new {@link TraceMethodVisitor}.
   *
   * @param methodVisitor the method visitor to which to delegate calls. May be {@literal null}.
   * @param printer the printer to convert the visited method into text.
   */
  public TraceMethodVisitor(final MethodVisitor methodVisitor, final Printer printer) {
    super(methodVisitor);
    this.p = printer;
  }

  @Override
  public void visitParameter(final String name, final int access) {
    p.visitParameter(name, access);
    super.visitParameter(name, access);
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    Printer annotationPrinter = p.visitMethodAnnotation(descriptor, visible);
    return new TraceAnnotationVisitor(
            super.visitAnnotation(descriptor, visible), annotationPrinter);
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    Printer annotationPrinter = p.visitMethodTypeAnnotation(typeRef, typePath, descriptor, visible);
    return new TraceAnnotationVisitor(
            super.visitTypeAnnotation(typeRef, typePath, descriptor, visible), annotationPrinter);
  }

  @Override
  public void visitAttribute(final Attribute attribute) {
    p.visitMethodAttribute(attribute);
    super.visitAttribute(attribute);
  }

  @Override
  public AnnotationVisitor visitAnnotationDefault() {
    Printer annotationPrinter = p.visitAnnotationDefault();
    return new TraceAnnotationVisitor(super.visitAnnotationDefault(), annotationPrinter);
  }

  @Override
  public void visitAnnotableParameterCount(final int parameterCount, final boolean visible) {
    p.visitAnnotableParameterCount(parameterCount, visible);
    super.visitAnnotableParameterCount(parameterCount, visible);
  }

  @Override
  public AnnotationVisitor visitParameterAnnotation(
          final int parameter, final String descriptor, final boolean visible) {
    Printer annotationPrinter = p.visitParameterAnnotation(parameter, descriptor, visible);
    return new TraceAnnotationVisitor(
            super.visitParameterAnnotation(parameter, descriptor, visible), annotationPrinter);
  }

  @Override
  public void visitCode() {
    p.visitCode();
    super.visitCode();
  }

  @Override
  public void visitFrame(
          final int type,
          final int numLocal,
          final Object[] local,
          final int numStack,
          final Object[] stack) {
    p.visitFrame(type, numLocal, local, numStack, stack);
    super.visitFrame(type, numLocal, local, numStack, stack);
  }

  @Override
  public void visitInsn(final int opcode) {
    p.visitInsn(opcode);
    super.visitInsn(opcode);
  }

  @Override
  public void visitIntInsn(final int opcode, final int operand) {
    p.visitIntInsn(opcode, operand);
    super.visitIntInsn(opcode, operand);
  }

  @Override
  public void visitVarInsn(final int opcode, final int var) {
    p.visitVarInsn(opcode, var);
    super.visitVarInsn(opcode, var);
  }

  @Override
  public void visitTypeInsn(final int opcode, final String type) {
    p.visitTypeInsn(opcode, type);
    super.visitTypeInsn(opcode, type);
  }

  @Override
  public void visitFieldInsn(
          final int opcode, final String owner, final String name, final String descriptor) {
    p.visitFieldInsn(opcode, owner, name, descriptor);
    super.visitFieldInsn(opcode, owner, name, descriptor);
  }

  @Override
  public void visitMethodInsn(
          final int opcode,
          final String owner,
          final String name,
          final String descriptor,
          final boolean isInterface) {
    p.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    if (mv != null) {
      mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }
  }

  @Override
  public void visitInvokeDynamicInsn(
          final String name,
          final String descriptor,
          final Handle bootstrapMethodHandle,
          final Object... bootstrapMethodArguments) {
    p.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
  }

  @Override
  public void visitJumpInsn(final int opcode, final Label label) {
    p.visitJumpInsn(opcode, label);
    super.visitJumpInsn(opcode, label);
  }

  @Override
  public void visitLabel(final Label label) {
    p.visitLabel(label);
    super.visitLabel(label);
  }

  @Override
  public void visitLdcInsn(final Object value) {
    p.visitLdcInsn(value);
    super.visitLdcInsn(value);
  }

  @Override
  public void visitIincInsn(final int var, final int increment) {
    p.visitIincInsn(var, increment);
    super.visitIincInsn(var, increment);
  }

  @Override
  public void visitTableSwitchInsn(
          final int min, final int max, final Label dflt, final Label... labels) {
    p.visitTableSwitchInsn(min, max, dflt, labels);
    super.visitTableSwitchInsn(min, max, dflt, labels);
  }

  @Override
  public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
    p.visitLookupSwitchInsn(dflt, keys, labels);
    super.visitLookupSwitchInsn(dflt, keys, labels);
  }

  @Override
  public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
    p.visitMultiANewArrayInsn(descriptor, numDimensions);
    super.visitMultiANewArrayInsn(descriptor, numDimensions);
  }

  @Override
  public AnnotationVisitor visitInsnAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    Printer annotationPrinter = p.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
    return new TraceAnnotationVisitor(
            super.visitInsnAnnotation(typeRef, typePath, descriptor, visible), annotationPrinter);
  }

  @Override
  public void visitTryCatchBlock(
          final Label start, final Label end, final Label handler, final String type) {
    p.visitTryCatchBlock(start, end, handler, type);
    super.visitTryCatchBlock(start, end, handler, type);
  }

  @Override
  public AnnotationVisitor visitTryCatchAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    Printer annotationPrinter = p.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
    return new TraceAnnotationVisitor(
            super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible), annotationPrinter);
  }

  @Override
  public void visitLocalVariable(
          final String name,
          final String descriptor,
          final String signature,
          final Label start,
          final Label end,
          final int index) {
    p.visitLocalVariable(name, descriptor, signature, start, end, index);
    super.visitLocalVariable(name, descriptor, signature, start, end, index);
  }

  @Override
  public AnnotationVisitor visitLocalVariableAnnotation(
          final int typeRef,
          final TypePath typePath,
          final Label[] start,
          final Label[] end,
          final int[] index,
          final String descriptor,
          final boolean visible) {
    Printer annotationPrinter =
            p.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
    return new TraceAnnotationVisitor(
            super.visitLocalVariableAnnotation(
                    typeRef, typePath, start, end, index, descriptor, visible),
            annotationPrinter);
  }

  @Override
  public void visitLineNumber(final int line, final Label start) {
    p.visitLineNumber(line, start);
    super.visitLineNumber(line, start);
  }

  @Override
  public void visitMaxs(final int maxStack, final int maxLocals) {
    p.visitMaxs(maxStack, maxLocals);
    super.visitMaxs(maxStack, maxLocals);
  }

  @Override
  public void visitEnd() {
    p.visitMethodEnd();
    super.visitEnd();
  }
}
