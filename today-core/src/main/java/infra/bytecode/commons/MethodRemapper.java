/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.bytecode.commons;

import org.jspecify.annotations.Nullable;

import infra.bytecode.AnnotationVisitor;
import infra.bytecode.Handle;
import infra.bytecode.Label;
import infra.bytecode.MethodVisitor;
import infra.bytecode.TypePath;

/**
 * A {@link MethodVisitor} that remaps types with a {@link Remapper}.
 *
 * @author Eugene Kuleshov
 */
public class MethodRemapper extends MethodVisitor {

  /** The remapper used to remap the types in the visited field. */
  protected final Remapper remapper;

  /**
   * Constructs a new {@link MethodRemapper}.
   *
   * @param methodVisitor the method visitor this remapper must delegate to.
   * @param remapper the remapper to use to remap the types in the visited method.
   */
  public MethodRemapper(final MethodVisitor methodVisitor, final Remapper remapper) {
    super(methodVisitor);
    this.remapper = remapper;
  }

  @Override
  public @Nullable AnnotationVisitor visitAnnotationDefault() {
    AnnotationVisitor annotationVisitor = super.visitAnnotationDefault();
    return createAnnotationRemapper(/* descriptor = */ null, annotationVisitor);
  }

  @Override
  public @Nullable AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    AnnotationVisitor annotationVisitor =
            super.visitAnnotation(remapper.mapDesc(descriptor), visible);
    return createAnnotationRemapper(descriptor, annotationVisitor);
  }

  @Override
  public @Nullable AnnotationVisitor visitTypeAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    AnnotationVisitor annotationVisitor =
            super.visitTypeAnnotation(typeRef, typePath, remapper.mapDesc(descriptor), visible);
    return createAnnotationRemapper(descriptor, annotationVisitor);
  }

  @Override
  public @Nullable AnnotationVisitor visitParameterAnnotation(
          final int parameter, final String descriptor, final boolean visible) {
    AnnotationVisitor annotationVisitor =
            super.visitParameterAnnotation(parameter, remapper.mapDesc(descriptor), visible);
    return createAnnotationRemapper(descriptor, annotationVisitor);
  }

  @Override
  public void visitFrame(
          final int type,
          final int numLocal,
          final Object[] local,
          final int numStack,
          final Object[] stack) {
    super.visitFrame(
            type,
            numLocal,
            remapFrameTypes(numLocal, local),
            numStack,
            remapFrameTypes(numStack, stack));
  }

  private Object @Nullable [] remapFrameTypes(final int numTypes, final Object[] frameTypes) {
    if (frameTypes == null) {
      return null;
    }
    Object[] remappedFrameTypes = null;
    for (int i = 0; i < numTypes; ++i) {
      if (frameTypes[i] instanceof String) {
        if (remappedFrameTypes == null) {
          remappedFrameTypes = new Object[numTypes];
          System.arraycopy(frameTypes, 0, remappedFrameTypes, 0, numTypes);
        }
        remappedFrameTypes[i] = remapper.mapType((String) frameTypes[i]);
      }
    }
    return remappedFrameTypes == null ? frameTypes : remappedFrameTypes;
  }

  @Override
  public void visitFieldInsn(
          final int opcode, final String owner, final String name, final String descriptor) {
    super.visitFieldInsn(
            opcode,
            remapper.mapType(owner),
            remapper.mapFieldName(owner, name, descriptor),
            remapper.mapDesc(descriptor));
  }

  @Override
  public void visitMethodInsn(
          final int opcodeAndSource,
          final String owner,
          final String name,
          final String descriptor,
          final boolean isInterface) {
    super.visitMethodInsn(
            opcodeAndSource,
            remapper.mapType(owner),
            remapper.mapMethodName(owner, name, descriptor),
            remapper.mapMethodDesc(descriptor),
            isInterface);
  }

  @Override
  public void visitInvokeDynamicInsn(final String name, final String descriptor,
          final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
    String remappedName = remapper.mapInvokeDynamicMethodName(
            name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    Object[] remappedBootstrapMethodArguments = new Object[bootstrapMethodArguments.length];
    for (int i = 0; i < bootstrapMethodArguments.length; ++i) {
      remappedBootstrapMethodArguments[i] = remapper.mapValue(bootstrapMethodArguments[i]);
    }
    super.visitInvokeDynamicInsn(
            remappedName,
            remapper.mapMethodDesc(descriptor),
            (Handle) remapper.mapValue(bootstrapMethodHandle),
            remappedBootstrapMethodArguments);
  }

  @Override
  public void visitTypeInsn(final int opcode, final String type) {
    super.visitTypeInsn(opcode, remapper.mapType(type));
  }

  @Override
  public void visitLdcInsn(final Object value) {
    super.visitLdcInsn(remapper.mapValue(value));
  }

  @Override
  public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
    super.visitMultiANewArrayInsn(remapper.mapDesc(descriptor), numDimensions);
  }

  @Override
  public AnnotationVisitor visitInsnAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    AnnotationVisitor annotationVisitor =
            super.visitInsnAnnotation(typeRef, typePath, remapper.mapDesc(descriptor), visible);
    return createAnnotationRemapper(descriptor, annotationVisitor);
  }

  @Override
  public void visitTryCatchBlock(
          final Label start, final Label end, final Label handler, final String type) {
    super.visitTryCatchBlock(start, end, handler, type == null ? null : remapper.mapType(type));
  }

  @Override
  public AnnotationVisitor visitTryCatchAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    AnnotationVisitor annotationVisitor =
            super.visitTryCatchAnnotation(typeRef, typePath, remapper.mapDesc(descriptor), visible);
    return createAnnotationRemapper(descriptor, annotationVisitor);
  }

  @Override
  public void visitLocalVariable(
          final String name,
          final String descriptor,
          final String signature,
          final Label start,
          final Label end,
          final int index) {
    super.visitLocalVariable(
            name,
            remapper.mapDesc(descriptor),
            remapper.mapSignature(signature, true),
            start,
            end,
            index);
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
    AnnotationVisitor annotationVisitor =
            super.visitLocalVariableAnnotation(
                    typeRef, typePath, start, end, index, remapper.mapDesc(descriptor), visible);
    return createAnnotationRemapper(descriptor, annotationVisitor);
  }

  /**
   * Constructs a new remapper for annotations. The default implementation of this method returns a
   * new {@link AnnotationRemapper}.
   *
   * @param descriptor the descriptor of the visited annotation.
   * @param annotationVisitor the AnnotationVisitor the remapper must delegate to.
   * @return the newly created remapper.
   */
  @Nullable
  protected AnnotationVisitor createAnnotationRemapper(
          final String descriptor, @Nullable final AnnotationVisitor annotationVisitor) {
    if (annotationVisitor == null) {
      return null;
    }
    return new AnnotationRemapper(descriptor, annotationVisitor, remapper);
  }
}
