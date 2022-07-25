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
import cn.taketoday.bytecode.Type;

/**
 * A node that represents a type instruction. A type instruction is an instruction that takes a type
 * descriptor as parameter.
 *
 * @author Eric Bruneton
 */
public class TypeInsnNode extends AbstractInsnNode {

  /**
   * The operand of this instruction. This operand is an internal name (see {@link
   * Type}).
   */
  public String desc;

  /**
   * Constructs a new {@link TypeInsnNode}.
   *
   * @param opcode the opcode of the type instruction to be constructed. This opcode must be NEW,
   * ANEWARRAY, CHECKCAST or INSTANCEOF.
   * @param descriptor the operand of the instruction to be constructed. This operand is an internal
   * name (see {@link Type}).
   */
  public TypeInsnNode(final int opcode, final String descriptor) {
    super(opcode);
    this.desc = descriptor;
  }

  /**
   * Sets the opcode of this instruction.
   *
   * @param opcode the new instruction opcode. This opcode must be NEW, ANEWARRAY, CHECKCAST or
   * INSTANCEOF.
   */
  public void setOpcode(final int opcode) {
    this.opcode = opcode;
  }

  @Override
  public int getType() {
    return TYPE_INSN;
  }

  @Override
  public void accept(final MethodVisitor methodVisitor) {
    methodVisitor.visitTypeInsn(opcode, desc);
    acceptAnnotations(methodVisitor);
  }

  @Override
  public AbstractInsnNode clone(final Map<LabelNode, LabelNode> clonedLabels) {
    return new TypeInsnNode(opcode, desc).cloneAnnotations(this);
  }
}
