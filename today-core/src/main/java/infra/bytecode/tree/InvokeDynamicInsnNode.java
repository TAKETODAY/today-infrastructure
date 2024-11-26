/*
 * Copyright 2017 - 2024 the original author or authors.
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
package infra.bytecode.tree;

import java.util.Map;

import infra.bytecode.Handle;
import infra.bytecode.MethodVisitor;
import infra.bytecode.Opcodes;
import infra.bytecode.Type;

/**
 * A node that represents an invokedynamic instruction.
 *
 * @author Remi Forax
 */
public class InvokeDynamicInsnNode extends AbstractInsnNode {

  /** The method's name. */
  public String name;

  /** The method's descriptor (see {@link Type}). */
  public String desc;

  /** The bootstrap method. */
  public Handle bsm;

  /** The bootstrap method constant arguments. */
  public Object[] bsmArgs;

  /**
   * Constructs a new {@link InvokeDynamicInsnNode}.
   *
   * @param name the method's name.
   * @param descriptor the method's descriptor (see {@link Type}).
   * @param bootstrapMethodHandle the bootstrap method.
   * @param bootstrapMethodArguments the bootstrap method constant arguments. Each argument must be
   * an {@link Integer}, {@link Float}, {@link Long}, {@link Double}, {@link String}, {@link
   * Type} or {@link Handle} value. This method is allowed to modify the
   * content of the array so a caller should expect that this array may change.
   */
  public InvokeDynamicInsnNode(
          final String name,
          final String descriptor,
          final Handle bootstrapMethodHandle,
          final Object... bootstrapMethodArguments) { // NOPMD(ArrayIsStoredDirectly): public field.
    super(Opcodes.INVOKEDYNAMIC);
    this.name = name;
    this.desc = descriptor;
    this.bsm = bootstrapMethodHandle;
    this.bsmArgs = bootstrapMethodArguments;
  }

  @Override
  public int getType() {
    return INVOKE_DYNAMIC_INSN;
  }

  @Override
  public void accept(final MethodVisitor methodVisitor) {
    methodVisitor.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    acceptAnnotations(methodVisitor);
  }

  @Override
  public AbstractInsnNode clone(final Map<LabelNode, LabelNode> clonedLabels) {
    return new InvokeDynamicInsnNode(name, desc, bsm, bsmArgs).cloneAnnotations(this);
  }
}
