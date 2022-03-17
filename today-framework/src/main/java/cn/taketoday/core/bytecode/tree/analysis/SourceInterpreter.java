// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package cn.taketoday.core.bytecode.tree.analysis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.tree.AbstractInsnNode;
import cn.taketoday.core.bytecode.tree.FieldInsnNode;
import cn.taketoday.core.bytecode.tree.InvokeDynamicInsnNode;
import cn.taketoday.core.bytecode.tree.LdcInsnNode;
import cn.taketoday.core.bytecode.tree.MethodInsnNode;

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
      case GETSTATIC -> size = Type.fromDescriptor(((FieldInsnNode) insn).desc).getSize();
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
      case GETFIELD -> Type.fromDescriptor(((FieldInsnNode) insn).desc).getSize();
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
