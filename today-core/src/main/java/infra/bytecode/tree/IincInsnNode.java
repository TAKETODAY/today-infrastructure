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

import infra.bytecode.MethodVisitor;
import infra.bytecode.Opcodes;

/**
 * A node that represents an IINC instruction.
 *
 * @author Eric Bruneton
 */
public class IincInsnNode extends AbstractInsnNode {

  /** Index of the local variable to be incremented. */
  public int var;

  /** Amount to increment the local variable by. */
  public int incr;

  /**
   * Constructs a new {@link IincInsnNode}.
   *
   * @param var index of the local variable to be incremented.
   * @param incr increment amount to increment the local variable by.
   */
  public IincInsnNode(final int var, final int incr) {
    super(Opcodes.IINC);
    this.var = var;
    this.incr = incr;
  }

  @Override
  public int getType() {
    return IINC_INSN;
  }

  @Override
  public void accept(final MethodVisitor methodVisitor) {
    methodVisitor.visitIincInsn(var, incr);
    acceptAnnotations(methodVisitor);
  }

  @Override
  public AbstractInsnNode clone(final Map<LabelNode, LabelNode> clonedLabels) {
    return new IincInsnNode(var, incr).cloneAnnotations(this);
  }
}
