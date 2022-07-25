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

import cn.taketoday.bytecode.ConstantDynamic;
import cn.taketoday.bytecode.Handle;
import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Opcodes;

/**
 * A {@link MethodVisitor} that approximates the size of the methods it visits.
 *
 * @author Eugene Kuleshov
 */
public class CodeSizeEvaluator extends MethodVisitor implements Opcodes {

  /** The minimum size in bytes of the visited method. */
  private int minSize;

  /** The maximum size in bytes of the visited method. */
  private int maxSize;

  public CodeSizeEvaluator(final MethodVisitor methodVisitor) {
    super(methodVisitor);
  }

  public int getMinSize() {
    return this.minSize;
  }

  public int getMaxSize() {
    return this.maxSize;
  }

  @Override
  public void visitInsn(final int opcode) {
    minSize += 1;
    maxSize += 1;
    super.visitInsn(opcode);
  }

  @Override
  public void visitIntInsn(final int opcode, final int operand) {
    if (opcode == SIPUSH) {
      minSize += 3;
      maxSize += 3;
    }
    else {
      minSize += 2;
      maxSize += 2;
    }
    super.visitIntInsn(opcode, operand);
  }

  @Override
  public void visitVarInsn(final int opcode, final int var) {
    if (var < 4 && opcode != RET) {
      minSize += 1;
      maxSize += 1;
    }
    else if (var >= 256) {
      minSize += 4;
      maxSize += 4;
    }
    else {
      minSize += 2;
      maxSize += 2;
    }
    super.visitVarInsn(opcode, var);
  }

  @Override
  public void visitTypeInsn(final int opcode, final String type) {
    minSize += 3;
    maxSize += 3;
    super.visitTypeInsn(opcode, type);
  }

  @Override
  public void visitFieldInsn(
          final int opcode, final String owner, final String name, final String descriptor) {
    minSize += 3;
    maxSize += 3;
    super.visitFieldInsn(opcode, owner, name, descriptor);
  }

  @Override
  public void visitMethodInsn(
          final int opcodeAndSource,
          final String owner,
          final String name,
          final String descriptor,
          final boolean isInterface) {
    int opcode = opcodeAndSource & ~Opcodes.SOURCE_MASK;

    if (opcode == INVOKEINTERFACE) {
      minSize += 5;
      maxSize += 5;
    }
    else {
      minSize += 3;
      maxSize += 3;
    }
    super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface);
  }

  @Override
  public void visitInvokeDynamicInsn(
          final String name,
          final String descriptor,
          final Handle bootstrapMethodHandle,
          final Object... bootstrapMethodArguments) {
    minSize += 5;
    maxSize += 5;
    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
  }

  @Override
  public void visitJumpInsn(final int opcode, final Label label) {
    minSize += 3;
    if (opcode == GOTO || opcode == JSR) {
      maxSize += 5;
    }
    else {
      maxSize += 8;
    }
    super.visitJumpInsn(opcode, label);
  }

  @Override
  public void visitLdcInsn(final Object value) {
    if (value instanceof Long
            || value instanceof Double
            || (value instanceof ConstantDynamic && ((ConstantDynamic) value).getSize() == 2)) {
      minSize += 3;
      maxSize += 3;
    }
    else {
      minSize += 2;
      maxSize += 3;
    }
    super.visitLdcInsn(value);
  }

  @Override
  public void visitIincInsn(final int var, final int increment) {
    if (var > 255 || increment > 127 || increment < -128) {
      minSize += 6;
      maxSize += 6;
    }
    else {
      minSize += 3;
      maxSize += 3;
    }
    super.visitIincInsn(var, increment);
  }

  @Override
  public void visitTableSwitchInsn(
          final int min, final int max, final Label dflt, final Label... labels) {
    minSize += 13 + labels.length * 4;
    maxSize += 16 + labels.length * 4;
    super.visitTableSwitchInsn(min, max, dflt, labels);
  }

  @Override
  public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
    minSize += 9 + keys.length * 8;
    maxSize += 12 + keys.length * 8;
    super.visitLookupSwitchInsn(dflt, keys, labels);
  }

  @Override
  public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
    minSize += 4;
    maxSize += 4;
    super.visitMultiANewArrayInsn(descriptor, numDimensions);
  }
}
