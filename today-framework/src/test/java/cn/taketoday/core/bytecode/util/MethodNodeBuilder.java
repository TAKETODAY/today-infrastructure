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

package cn.taketoday.core.bytecode.util;

import java.util.Arrays;

import cn.taketoday.core.bytecode.Label;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.tree.FrameNode;
import cn.taketoday.core.bytecode.tree.MethodNode;

/**
 * A builder of {@link MethodNode}, to construct test cases for unit tests.
 *
 * @author Eric Bruneton
 */
final class MethodNodeBuilder {

  private final MethodNode methodNode;

  MethodNodeBuilder() {
    this(/* maxStack = */ 10, /* maxLocals = */ 10);
  }

  MethodNodeBuilder(final int maxStack, final int maxLocals) {
    methodNode = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
    methodNode.maxStack = maxStack;
    methodNode.maxLocals = maxLocals;
    methodNode.visitCode();
  }

  MethodNodeBuilder nop() {
    methodNode.visitInsn(Opcodes.NOP);
    return this;
  }

  MethodNodeBuilder iconst_0() {
    methodNode.visitInsn(Opcodes.ICONST_0);
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

  MethodNodeBuilder go(final Label label) {
    methodNode.visitJumpInsn(Opcodes.GOTO, label);
    return this;
  }

  MethodNodeBuilder jsr(final Label label) {
    methodNode.visitJumpInsn(Opcodes.JSR, label);
    return this;
  }

  MethodNodeBuilder ret(final int varIndex) {
    methodNode.visitVarInsn(Opcodes.RET, varIndex);
    return this;
  }

  MethodNodeBuilder ifne(final Label label) {
    methodNode.visitJumpInsn(Opcodes.IFNE, label);
    return this;
  }

  MethodNodeBuilder frame(final int type, final Object[] local, final Object[] stack) {
    FrameNode frameNode = new FrameNode(Opcodes.F_NEW, 0, null, 0, null);
    frameNode.type = type;
    frameNode.local = local == null ? null : Arrays.asList(local);
    frameNode.stack = stack == null ? null : Arrays.asList(stack);
    methodNode.instructions.add(frameNode);
    return this;
  }

  MethodNode build() {
    methodNode.visitEnd();
    return methodNode;
  }
}
