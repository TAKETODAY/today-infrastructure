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

import java.util.List;

import cn.taketoday.core.bytecode.ConstantDynamic;
import cn.taketoday.core.bytecode.Handle;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.tree.AbstractInsnNode;
import cn.taketoday.core.bytecode.tree.FieldInsnNode;
import cn.taketoday.core.bytecode.tree.IntInsnNode;
import cn.taketoday.core.bytecode.tree.InvokeDynamicInsnNode;
import cn.taketoday.core.bytecode.tree.LdcInsnNode;
import cn.taketoday.core.bytecode.tree.MethodInsnNode;
import cn.taketoday.core.bytecode.tree.MultiANewArrayInsnNode;
import cn.taketoday.core.bytecode.tree.TypeInsnNode;

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
  public static final Type NULL_TYPE = Type.fromInternalName("null");

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
      case ICONST_M1:
      case ICONST_0:
      case ICONST_1:
      case ICONST_2:
      case ICONST_3:
      case ICONST_4:
      case ICONST_5:
        return BasicValue.INT_VALUE;
      case LCONST_0:
      case LCONST_1:
        return BasicValue.LONG_VALUE;
      case FCONST_0:
      case FCONST_1:
      case FCONST_2:
        return BasicValue.FLOAT_VALUE;
      case DCONST_0:
      case DCONST_1:
        return BasicValue.DOUBLE_VALUE;
      case BIPUSH:
      case SIPUSH:
        return BasicValue.INT_VALUE;
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
          return newValue(Type.fromInternalName("java/lang/String"));
        }
        else if (value instanceof Type) {
          int sort = ((Type) value).getSort();
          if (sort == Type.OBJECT || sort == Type.ARRAY) {
            return newValue(Type.fromInternalName("java/lang/Class"));
          }
          else if (sort == Type.METHOD) {
            return newValue(Type.fromInternalName("java/lang/invoke/MethodType"));
          }
          else {
            throw new AnalyzerException(insn, "Illegal LDC value " + value);
          }
        }
        else if (value instanceof Handle) {
          return newValue(Type.fromInternalName("java/lang/invoke/MethodHandle"));
        }
        else if (value instanceof ConstantDynamic) {
          return newValue(Type.fromDescriptor(((ConstantDynamic) value).getDescriptor()));
        }
        else {
          throw new AnalyzerException(insn, "Illegal LDC value " + value);
        }
      case JSR:
        return BasicValue.RETURNADDRESS_VALUE;
      case GETSTATIC:
        return newValue(Type.fromDescriptor(((FieldInsnNode) insn).desc));
      case NEW:
        return newValue(Type.fromInternalName(((TypeInsnNode) insn).desc));
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
      case INEG:
      case IINC:
      case L2I:
      case F2I:
      case D2I:
      case I2B:
      case I2C:
      case I2S:
        return BasicValue.INT_VALUE;
      case FNEG:
      case I2F:
      case L2F:
      case D2F:
        return BasicValue.FLOAT_VALUE;
      case LNEG:
      case I2L:
      case F2L:
      case D2L:
        return BasicValue.LONG_VALUE;
      case DNEG:
      case I2D:
      case L2D:
      case F2D:
        return BasicValue.DOUBLE_VALUE;
      case IFEQ:
      case IFNE:
      case IFLT:
      case IFGE:
      case IFGT:
      case IFLE:
      case TABLESWITCH:
      case LOOKUPSWITCH:
      case IRETURN:
      case LRETURN:
      case FRETURN:
      case DRETURN:
      case ARETURN:
      case PUTSTATIC:
        return null;
      case GETFIELD:
        return newValue(Type.fromDescriptor(((FieldInsnNode) insn).desc));
      case NEWARRAY:
        switch (((IntInsnNode) insn).operand) {
          case T_BOOLEAN:
            return newValue(Type.fromDescriptor("[Z"));
          case T_CHAR:
            return newValue(Type.fromDescriptor("[C"));
          case T_BYTE:
            return newValue(Type.fromDescriptor("[B"));
          case T_SHORT:
            return newValue(Type.fromDescriptor("[S"));
          case T_INT:
            return newValue(Type.fromDescriptor("[I"));
          case T_FLOAT:
            return newValue(Type.fromDescriptor("[F"));
          case T_DOUBLE:
            return newValue(Type.fromDescriptor("[D"));
          case T_LONG:
            return newValue(Type.fromDescriptor("[J"));
          default:
            break;
        }
        throw new AnalyzerException(insn, "Invalid array type");
      case ANEWARRAY:
        return newValue(Type.fromDescriptor("[" + Type.fromInternalName(((TypeInsnNode) insn).desc)));
      case ARRAYLENGTH:
        return BasicValue.INT_VALUE;
      case ATHROW:
        return null;
      case CHECKCAST:
        return newValue(Type.fromInternalName(((TypeInsnNode) insn).desc));
      case INSTANCEOF:
        return BasicValue.INT_VALUE;
      case MONITORENTER:
      case MONITOREXIT:
      case IFNULL:
      case IFNONNULL:
        return null;
      default:
        throw new AssertionError();
    }
  }

  @Override
  public BasicValue binaryOperation(
          final AbstractInsnNode insn, final BasicValue value1, final BasicValue value2)
          throws AnalyzerException {
    switch (insn.getOpcode()) {
      case IALOAD:
      case BALOAD:
      case CALOAD:
      case SALOAD:
      case IADD:
      case ISUB:
      case IMUL:
      case IDIV:
      case IREM:
      case ISHL:
      case ISHR:
      case IUSHR:
      case IAND:
      case IOR:
      case IXOR:
        return BasicValue.INT_VALUE;
      case FALOAD:
      case FADD:
      case FSUB:
      case FMUL:
      case FDIV:
      case FREM:
        return BasicValue.FLOAT_VALUE;
      case LALOAD:
      case LADD:
      case LSUB:
      case LMUL:
      case LDIV:
      case LREM:
      case LSHL:
      case LSHR:
      case LUSHR:
      case LAND:
      case LOR:
      case LXOR:
        return BasicValue.LONG_VALUE;
      case DALOAD:
      case DADD:
      case DSUB:
      case DMUL:
      case DDIV:
      case DREM:
        return BasicValue.DOUBLE_VALUE;
      case AALOAD:
        return BasicValue.REFERENCE_VALUE;
      case LCMP:
      case FCMPL:
      case FCMPG:
      case DCMPL:
      case DCMPG:
        return BasicValue.INT_VALUE;
      case IF_ICMPEQ:
      case IF_ICMPNE:
      case IF_ICMPLT:
      case IF_ICMPGE:
      case IF_ICMPGT:
      case IF_ICMPLE:
      case IF_ACMPEQ:
      case IF_ACMPNE:
      case PUTFIELD:
        return null;
      default:
        throw new AssertionError();
    }
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
      return newValue(Type.fromDescriptor(((MultiANewArrayInsnNode) insn).desc));
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
