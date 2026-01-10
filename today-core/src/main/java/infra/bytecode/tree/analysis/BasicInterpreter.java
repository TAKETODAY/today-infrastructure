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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.bytecode.tree.analysis;

import java.util.List;

import infra.bytecode.ConstantDynamic;
import infra.bytecode.Handle;
import infra.bytecode.Opcodes;
import infra.bytecode.Type;
import infra.bytecode.tree.AbstractInsnNode;
import infra.bytecode.tree.FieldInsnNode;
import infra.bytecode.tree.IntInsnNode;
import infra.bytecode.tree.InvokeDynamicInsnNode;
import infra.bytecode.tree.LdcInsnNode;
import infra.bytecode.tree.MethodInsnNode;
import infra.bytecode.tree.MultiANewArrayInsnNode;
import infra.bytecode.tree.TypeInsnNode;

/**
 * An {@link Interpreter} for {@link BasicValue} values.
 *
 * @author Eric Bruneton
 * @author Bing Ran
 */
public class BasicInterpreter extends Interpreter<BasicValue> implements Opcodes {

  /**
   * Special type used for the {@literal null} literal. This is an object reference type with
   * descriptor 'Lnull;'.
   */
  public static final Type NULL_TYPE = Type.forInternalName("null");

  /**
   * Constructs a new {@link BasicInterpreter}.
   */
  public BasicInterpreter() { }

  @Override
  public BasicValue newValue(final Type type) {
    if (type == null) {
      return BasicValue.UNINITIALIZED_VALUE;
    }
    return switch (type.getSort()) {
      case Type.VOID -> null;
      case Type.LONG -> BasicValue.LONG_VALUE;
      case Type.FLOAT -> BasicValue.FLOAT_VALUE;
      case Type.DOUBLE -> BasicValue.DOUBLE_VALUE;
      case Type.ARRAY, Type.OBJECT -> BasicValue.REFERENCE_VALUE;
      case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> BasicValue.INT_VALUE;
      default -> throw new AssertionError();
    };
  }

  @Override
  public BasicValue newOperation(final AbstractInsnNode insn) throws AnalyzerException {
    switch (insn.getOpcode()) {
      case ACONST_NULL:
        return newValue(NULL_TYPE);
      case ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5:
      case BIPUSH, SIPUSH:
        return BasicValue.INT_VALUE;
      case LCONST_0, LCONST_1:
        return BasicValue.LONG_VALUE;
      case FCONST_0, FCONST_1, FCONST_2:
        return BasicValue.FLOAT_VALUE;
      case DCONST_0, DCONST_1:
        return BasicValue.DOUBLE_VALUE;
      case LDC:
        Object value = ((LdcInsnNode) insn).cst;
        if (value instanceof Integer) {
          return BasicValue.INT_VALUE;
        }
        else if (value instanceof Float) {
          return BasicValue.FLOAT_VALUE;
        }
        else if (value instanceof Long) {
          return BasicValue.LONG_VALUE;
        }
        else if (value instanceof Double) {
          return BasicValue.DOUBLE_VALUE;
        }
        else if (value instanceof String) {
          return newValue(Type.forInternalName("java/lang/String"));
        }
        else if (value instanceof Type) {
          int sort = ((Type) value).getSort();
          if (sort == Type.OBJECT || sort == Type.ARRAY) {
            return newValue(Type.forInternalName("java/lang/Class"));
          }
          else if (sort == Type.METHOD) {
            return newValue(Type.forInternalName("java/lang/invoke/MethodType"));
          }
          else {
            throw new AnalyzerException(insn, "Illegal LDC value " + value);
          }
        }
        else if (value instanceof Handle) {
          return newValue(Type.forInternalName("java/lang/invoke/MethodHandle"));
        }
        else if (value instanceof ConstantDynamic) {
          return newValue(Type.forDescriptor(((ConstantDynamic) value).getDescriptor()));
        }
        else {
          throw new AnalyzerException(insn, "Illegal LDC value " + value);
        }
      case JSR:
        return BasicValue.RETURNADDRESS_VALUE;
      case GETSTATIC:
        return newValue(Type.forDescriptor(((FieldInsnNode) insn).desc));
      case NEW:
        return newValue(Type.forInternalName(((TypeInsnNode) insn).desc));
      default:
        throw new AssertionError();
    }
  }

  @Override
  public BasicValue copyOperation(final AbstractInsnNode insn, final BasicValue value)
          throws AnalyzerException {
    return value;
  }

  @Override
  public BasicValue unaryOperation(final AbstractInsnNode insn, final BasicValue value)
          throws AnalyzerException {
    switch (insn.getOpcode()) {
      case INEG, IINC, L2I, F2I, D2I, I2B, I2C, I2S:
      case ARRAYLENGTH:
      case INSTANCEOF:
        return BasicValue.INT_VALUE;
      case FNEG, I2F, L2F, D2F:
        return BasicValue.FLOAT_VALUE;
      case LNEG, I2L, F2L, D2L:
        return BasicValue.LONG_VALUE;
      case DNEG, I2D, L2D, F2D:
        return BasicValue.DOUBLE_VALUE;
      case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, TABLESWITCH, LOOKUPSWITCH, IRETURN, LRETURN, FRETURN,
           DRETURN, ARETURN, PUTSTATIC, ATHROW, MONITORENTER, MONITOREXIT, IFNULL, IFNONNULL:
        return null;
      case GETFIELD:
        return newValue(Type.forDescriptor(((FieldInsnNode) insn).desc));
      case NEWARRAY:
        switch (((IntInsnNode) insn).operand) {
          case T_BOOLEAN:
            return newValue(Type.forDescriptor("[Z"));
          case T_CHAR:
            return newValue(Type.forDescriptor("[C"));
          case T_BYTE:
            return newValue(Type.forDescriptor("[B"));
          case T_SHORT:
            return newValue(Type.forDescriptor("[S"));
          case T_INT:
            return newValue(Type.forDescriptor("[I"));
          case T_FLOAT:
            return newValue(Type.forDescriptor("[F"));
          case T_DOUBLE:
            return newValue(Type.forDescriptor("[D"));
          case T_LONG:
            return newValue(Type.forDescriptor("[J"));
          default:
            break;
        }
        throw new AnalyzerException(insn, "Invalid array type");
      case ANEWARRAY:
        return newValue(Type.forDescriptor("[" + Type.forInternalName(((TypeInsnNode) insn).desc)));
      case CHECKCAST:
        return newValue(Type.forInternalName(((TypeInsnNode) insn).desc));
      default:
        throw new AssertionError();
    }
  }

  @Override
  public BasicValue binaryOperation(
          final AbstractInsnNode insn, final BasicValue value1, final BasicValue value2)
          throws AnalyzerException {
    return switch (insn.getOpcode()) {
      case IALOAD, BALOAD, CALOAD, SALOAD, IADD, ISUB, IMUL, IDIV, IREM, ISHL,
           ISHR, IUSHR, IAND, IOR, IXOR, LCMP, FCMPL, FCMPG, DCMPL, DCMPG -> BasicValue.INT_VALUE;
      case FALOAD, FADD, FSUB, FMUL, FDIV, FREM -> BasicValue.FLOAT_VALUE;
      case LALOAD, LADD, LSUB, LMUL, LDIV, LREM, LSHL, LSHR, LUSHR, LAND, LOR, LXOR -> BasicValue.LONG_VALUE;
      case DALOAD, DADD, DSUB, DMUL, DDIV, DREM -> BasicValue.DOUBLE_VALUE;
      case AALOAD -> BasicValue.REFERENCE_VALUE;
      case IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, PUTFIELD -> null;
      default -> throw new AssertionError();
    };
  }

  @Override
  public BasicValue ternaryOperation(
          final AbstractInsnNode insn,
          final BasicValue value1,
          final BasicValue value2,
          final BasicValue value3)
          throws AnalyzerException {
    return null;
  }

  @Override
  public BasicValue naryOperation(
          final AbstractInsnNode insn, final List<? extends BasicValue> values)
          throws AnalyzerException {
    int opcode = insn.getOpcode();
    if (opcode == MULTIANEWARRAY) {
      return newValue(Type.forDescriptor(((MultiANewArrayInsnNode) insn).desc));
    }
    else if (opcode == INVOKEDYNAMIC) {
      return newValue(Type.forReturnType(((InvokeDynamicInsnNode) insn).desc));
    }
    else {
      return newValue(Type.forReturnType(((MethodInsnNode) insn).desc));
    }
  }

  @Override
  public void returnOperation(
          final AbstractInsnNode insn, final BasicValue value, final BasicValue expected)
          throws AnalyzerException {
    // Nothing to do.
  }

  @Override
  public BasicValue merge(final BasicValue value1, final BasicValue value2) {
    if (!value1.equals(value2)) {
      return BasicValue.UNINITIALIZED_VALUE;
    }
    return value1;
  }
}
