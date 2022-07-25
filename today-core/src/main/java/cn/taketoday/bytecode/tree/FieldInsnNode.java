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
 * A node that represents a field instruction. A field instruction is an instruction that loads or
 * stores the value of a field of an object.
 *
 * @author Eric Bruneton
 */
public class FieldInsnNode extends AbstractInsnNode {

  /**
   * The internal name of the field's owner class (see {@link
   * Type#getInternalName}).
   */
  public String owner;

  /** The field's name. */
  public String name;

  /** The field's descriptor (see {@link Type}). */
  public String desc;

  /**
   * Constructs a new {@link FieldInsnNode}.
   *
   * @param opcode the opcode of the type instruction to be constructed. This opcode must be
   * GETSTATIC, PUTSTATIC, GETFIELD or PUTFIELD.
   * @param owner the internal name of the field's owner class (see {@link
   * Type#getInternalName}).
   * @param name the field's name.
   * @param descriptor the field's descriptor (see {@link Type}).
   */
  public FieldInsnNode(
          final int opcode, final String owner, final String name, final String descriptor) {
    super(opcode);
    this.owner = owner;
    this.name = name;
    this.desc = descriptor;
  }

  /**
   * Sets the opcode of this instruction.
   *
   * @param opcode the new instruction opcode. This opcode must be GETSTATIC, PUTSTATIC, GETFIELD or
   * PUTFIELD.
   */
  public void setOpcode(final int opcode) {
    this.opcode = opcode;
  }

  @Override
  public int getType() {
    return FIELD_INSN;
  }

  @Override
  public void accept(final MethodVisitor methodVisitor) {
    methodVisitor.visitFieldInsn(opcode, owner, name, desc);
    acceptAnnotations(methodVisitor);
  }

  @Override
  public AbstractInsnNode clone(final Map<LabelNode, LabelNode> clonedLabels) {
    return new FieldInsnNode(opcode, owner, name, desc).cloneAnnotations(this);
  }
}
