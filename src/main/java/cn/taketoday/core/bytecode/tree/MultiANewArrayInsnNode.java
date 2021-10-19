/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core.bytecode.tree;

import java.util.Map;

import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;

/**
 * A node that represents a MULTIANEWARRAY instruction.
 *
 * @author Eric Bruneton
 */
public class MultiANewArrayInsnNode extends AbstractInsnNode {

  /** An array type descriptor (see {@link Type}). */
  public String desc;

  /** Number of dimensions of the array to allocate. */
  public int dims;

  /**
   * Constructs a new {@link MultiANewArrayInsnNode}.
   *
   * @param descriptor an array type descriptor (see {@link Type}).
   * @param numDimensions the number of dimensions of the array to allocate.
   */
  public MultiANewArrayInsnNode(final String descriptor, final int numDimensions) {
    super(Opcodes.MULTIANEWARRAY);
    this.desc = descriptor;
    this.dims = numDimensions;
  }

  @Override
  public int getType() {
    return MULTIANEWARRAY_INSN;
  }

  @Override
  public void accept(final MethodVisitor methodVisitor) {
    methodVisitor.visitMultiANewArrayInsn(desc, dims);
    acceptAnnotations(methodVisitor);
  }

  @Override
  public AbstractInsnNode clone(final Map<LabelNode, LabelNode> clonedLabels) {
    return new MultiANewArrayInsnNode(desc, dims).cloneAnnotations(this);
  }
}
