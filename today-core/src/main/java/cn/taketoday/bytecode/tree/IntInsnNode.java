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
package cn.taketoday.bytecode.tree;

import java.util.Map;

import cn.taketoday.bytecode.MethodVisitor;

/**
 * A node that represents an instruction with a single int operand.
 *
 * @author Eric Bruneton
 */
public class IntInsnNode extends AbstractInsnNode {

  /** The operand of this instruction. */
  public int operand;

  /**
   * Constructs a new {@link IntInsnNode}.
   *
   * @param opcode the opcode of the instruction to be constructed. This opcode must be BIPUSH,
   * SIPUSH or NEWARRAY.
   * @param operand the operand of the instruction to be constructed.
   */
  public IntInsnNode(final int opcode, final int operand) {
    super(opcode);
    this.operand = operand;
  }

  /**
   * Sets the opcode of this instruction.
   *
   * @param opcode the new instruction opcode. This opcode must be BIPUSH, SIPUSH or NEWARRAY.
   */
  public void setOpcode(final int opcode) {
    this.opcode = opcode;
  }

  @Override
  public int getType() {
    return INT_INSN;
  }

  @Override
  public void accept(final MethodVisitor methodVisitor) {
    methodVisitor.visitIntInsn(opcode, operand);
    acceptAnnotations(methodVisitor);
  }

  @Override
  public AbstractInsnNode clone(final Map<LabelNode, LabelNode> clonedLabels) {
    return new IntInsnNode(opcode, operand).cloneAnnotations(this);
  }
}
