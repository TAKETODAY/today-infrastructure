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
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;

/**
 * A node that represents a method instruction. A method instruction is an instruction that invokes
 * a method.
 *
 * @author Eric Bruneton
 */
public class MethodInsnNode extends AbstractInsnNode {

  /**
   * The internal name of the method's owner class (see {@link
   * Type#getInternalName()}).
   *
   * <p>For methods of arrays, e.g., {@code clone()}, the array type descriptor.
   */
  public String owner;

  /** The method's name. */
  public String name;

  /** The method's descriptor (see {@link Type}). */
  public String desc;

  /** Whether the method's owner class if an interface. */
  public boolean itf;

  /**
   * Constructs a new {@link MethodInsnNode}.
   *
   * @param opcode the opcode of the type instruction to be constructed. This opcode must be
   * INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC or INVOKEINTERFACE.
   * @param owner the internal name of the method's owner class (see {@link
   * Type#getInternalName()}).
   * @param name the method's name.
   * @param descriptor the method's descriptor (see {@link Type}).
   */
  public MethodInsnNode(
          final int opcode, final String owner, final String name, final String descriptor) {
    this(opcode, owner, name, descriptor, opcode == Opcodes.INVOKEINTERFACE);
  }

  /**
   * Constructs a new {@link MethodInsnNode}.
   *
   * @param opcode the opcode of the type instruction to be constructed. This opcode must be
   * INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC or INVOKEINTERFACE.
   * @param owner the internal name of the method's owner class (see {@link
   * Type#getInternalName()}).
   * @param name the method's name.
   * @param descriptor the method's descriptor (see {@link Type}).
   * @param isInterface if the method's owner class is an interface.
   */
  public MethodInsnNode(
          final int opcode,
          final String owner,
          final String name,
          final String descriptor,
          final boolean isInterface) {
    super(opcode);
    this.owner = owner;
    this.name = name;
    this.desc = descriptor;
    this.itf = isInterface;
  }

  /**
   * Sets the opcode of this instruction.
   *
   * @param opcode the new instruction opcode. This opcode must be INVOKEVIRTUAL, INVOKESPECIAL,
   * INVOKESTATIC or INVOKEINTERFACE.
   */
  public void setOpcode(final int opcode) {
    this.opcode = opcode;
  }

  @Override
  public int getType() {
    return METHOD_INSN;
  }

  @Override
  public void accept(final MethodVisitor methodVisitor) {
    methodVisitor.visitMethodInsn(opcode, owner, name, desc, itf);
    acceptAnnotations(methodVisitor);
  }

  @Override
  public AbstractInsnNode clone(final Map<LabelNode, LabelNode> clonedLabels) {
    return new MethodInsnNode(opcode, owner, name, desc, itf).cloneAnnotations(this);
  }
}
