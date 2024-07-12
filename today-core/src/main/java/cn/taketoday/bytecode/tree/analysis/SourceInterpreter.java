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
package cn.taketoday.bytecode.tree.analysis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.tree.AbstractInsnNode;
import cn.taketoday.bytecode.tree.FieldInsnNode;
import cn.taketoday.bytecode.tree.InvokeDynamicInsnNode;
import cn.taketoday.bytecode.tree.LdcInsnNode;
import cn.taketoday.bytecode.tree.MethodInsnNode;

/**
 * An {@link Interpreter} for {@link SourceValue} values.
 *
 * @author Eric Bruneton
 */
public class SourceInterpreter extends Interpreter<SourceValue> implements Opcodes {

  /**
   * Constructs a new {@link SourceInterpreter} for the latest ASM API version.
   */
  public SourceInterpreter() { }

  @Override
  public SourceValue newValue(final Type type) {
    if (type == Type.VOID_TYPE) {
      return null;
    }
    return new SourceValue(type == null ? 1 : type.getSize());
  }

  @Override
  public SourceValue newOperation(final AbstractInsnNode insn) {
    int size;
    switch (insn.getOpcode()) {
      case LCONST_0, LCONST_1, DCONST_0, DCONST_1 -> size = 2;
      case LDC -> {
        Object value = ((LdcInsnNode) insn).cst;
        size = value instanceof Long || value instanceof Double ? 2 : 1;
      }
      case GETSTATIC -> size = Type.forDescriptor(((FieldInsnNode) insn).desc).getSize();
      default -> size = 1;
    }
    return new SourceValue(size, insn);
  }

  @Override
  public SourceValue copyOperation(final AbstractInsnNode insn, final SourceValue value) {
    return new SourceValue(value.getSize(), insn);
  }

  @Override
  public SourceValue unaryOperation(final AbstractInsnNode insn, final SourceValue value) {
    int size = switch (insn.getOpcode()) {
      case LNEG, DNEG, I2L, I2D, L2D, F2L, F2D, D2L -> 2;
      case GETFIELD -> Type.forDescriptor(((FieldInsnNode) insn).desc).getSize();
      default -> 1;
    };
    return new SourceValue(size, insn);
  }

  @Override
  public SourceValue binaryOperation(
          final AbstractInsnNode insn, final SourceValue value1, final SourceValue value2) {
    int size = switch (insn.getOpcode()) {
      case LALOAD, DALOAD, LADD, DADD, LSUB, DSUB, LMUL, DMUL,
              LDIV, DDIV, LREM, DREM, LSHL, LSHR, LUSHR, LAND, LOR, LXOR -> 2;
      default -> 1;
    };
    return new SourceValue(size, insn);
  }

  @Override
  public SourceValue ternaryOperation(
          final AbstractInsnNode insn,
          final SourceValue value1,
          final SourceValue value2,
          final SourceValue value3) {
    return new SourceValue(1, insn);
  }

  @Override
  public SourceValue naryOperation(
          final AbstractInsnNode insn, final List<? extends SourceValue> values) {
    int size;
    int opcode = insn.getOpcode();
    if (opcode == MULTIANEWARRAY) {
      size = 1;
    }
    else if (opcode == INVOKEDYNAMIC) {
      size = Type.forReturnType(((InvokeDynamicInsnNode) insn).desc).getSize();
    }
    else {
      size = Type.forReturnType(((MethodInsnNode) insn).desc).getSize();
    }
    return new SourceValue(size, insn);
  }

  @Override
  public void returnOperation(
          final AbstractInsnNode insn, final SourceValue value, final SourceValue expected) {
    // Nothing to do.
  }

  @Override
  public SourceValue merge(final SourceValue value1, final SourceValue value2) {
    if (value1.insns instanceof SmallSet && value2.insns instanceof SmallSet) {
      Set<AbstractInsnNode> setUnion =
              ((SmallSet<AbstractInsnNode>) value1.insns)
                      .union((SmallSet<AbstractInsnNode>) value2.insns);
      if (setUnion == value1.insns && value1.size == value2.size) {
        return value1;
      }
      else {
        return new SourceValue(Math.min(value1.size, value2.size), setUnion);
      }
    }
    if (value1.size != value2.size || !containsAll(value1.insns, value2.insns)) {
      HashSet<AbstractInsnNode> setUnion = new HashSet<>();
      setUnion.addAll(value1.insns);
      setUnion.addAll(value2.insns);
      return new SourceValue(Math.min(value1.size, value2.size), setUnion);
    }
    return value1;
  }

  private static <E> boolean containsAll(final Set<E> self, final Set<E> other) {
    if (self.size() < other.size()) {
      return false;
    }
    return self.containsAll(other);
  }
}
