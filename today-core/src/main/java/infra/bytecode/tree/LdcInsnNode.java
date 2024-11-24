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

import infra.bytecode.ConstantDynamic;
import infra.bytecode.Handle;
import infra.bytecode.MethodVisitor;
import infra.bytecode.Opcodes;
import infra.bytecode.Type;

/**
 * A node that represents an LDC instruction.
 *
 * @author Eric Bruneton
 */
public class LdcInsnNode extends AbstractInsnNode {

  /**
   * The constant to be loaded on the stack. This field must be a non null {@link Integer}, a {@link
   * Float}, a {@link Long}, a {@link Double}, a {@link String}, a {@link Type} of OBJECT or ARRAY
   * sort for {@code .class} constants, for classes whose version is 49, a {@link Type} of METHOD
   * sort for MethodType, a {@link Handle} for MethodHandle constants, for classes whose version is
   * 51 or a {@link ConstantDynamic} for a constant dynamic for classes whose version is 55.
   */
  public Object cst;

  /**
   * Constructs a new {@link LdcInsnNode}.
   *
   * @param value the constant to be loaded on the stack. This parameter mist be a non null {@link
   * Integer}, a {@link Float}, a {@link Long}, a {@link Double}, a {@link String}, a {@link
   * Type} of OBJECT or ARRAY sort for {@code .class} constants, for classes whose version is
   * 49, a {@link Type} of METHOD sort for MethodType, a {@link Handle} for MethodHandle
   * constants, for classes whose version is 51 or a {@link ConstantDynamic} for a constant
   * dynamic for classes whose version is 55.
   */
  public LdcInsnNode(final Object value) {
    super(Opcodes.LDC);
    this.cst = value;
  }

  @Override
  public int getType() {
    return LDC_INSN;
  }

  @Override
  public void accept(final MethodVisitor methodVisitor) {
    methodVisitor.visitLdcInsn(cst);
    acceptAnnotations(methodVisitor);
  }

  @Override
  public AbstractInsnNode clone(final Map<LabelNode, LabelNode> clonedLabels) {
    return new LdcInsnNode(cst).cloneAnnotations(this);
  }
}
