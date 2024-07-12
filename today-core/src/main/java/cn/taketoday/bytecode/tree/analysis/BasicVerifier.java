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

import java.util.List;

import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.tree.AbstractInsnNode;
import cn.taketoday.bytecode.tree.FieldInsnNode;
import cn.taketoday.bytecode.tree.InvokeDynamicInsnNode;
import cn.taketoday.bytecode.tree.MethodInsnNode;

/**
 * An extended {@link BasicInterpreter} that checks that bytecode instructions are correctly used.
 *
 * @author Eric Bruneton
 * @author Bing Ran
 */
public class BasicVerifier extends BasicInterpreter {

  /**
   * Constructs a new {@link BasicVerifier} for the latest ASM API version.
   */
  public BasicVerifier() { }

  @Override
  public BasicValue copyOperation(final AbstractInsnNode insn, final BasicValue value)
          throws AnalyzerException {
    Value expected;
    switch (insn.getOpcode()) {
      case ILOAD:
      case ISTORE:
        expected = BasicValue.INT_VALUE;
        break;
      case FLOAD:
      case FSTORE:
        expected = BasicValue.FLOAT_VALUE;
        break;
      case LLOAD:
      case LSTORE:
        expected = BasicValue.LONG_VALUE;
        break;
      case DLOAD:
      case DSTORE:
        expected = BasicValue.DOUBLE_VALUE;
        break;
      case ALOAD:
        if (!value.isReference()) {
          throw new AnalyzerException(insn, null, "an object reference", value);
        }
        return value;
      case ASTORE:
        if (!value.isReference() && !BasicValue.RETURNADDRESS_VALUE.equals(value)) {
          throw new AnalyzerException(insn, null, "an object reference or a return address", value);
        }
        return value;
      default:
        return value;
    }
    if (!expected.equals(value)) {
      throw new AnalyzerException(insn, null, expected, value);
    }
    return value;
  }

  @Override
  public BasicValue unaryOperation(final AbstractInsnNode insn, final BasicValue value) throws AnalyzerException {
    BasicValue expected;
    switch (insn.getOpcode()) {
      case INEG, IINC, I2F, I2L, I2D, I2B, I2C, I2S, IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, TABLESWITCH, LOOKUPSWITCH,
              IRETURN, NEWARRAY, ANEWARRAY -> expected = BasicValue.INT_VALUE;
      case FNEG, F2I, F2L, F2D, FRETURN -> expected = BasicValue.FLOAT_VALUE;
      case LNEG, L2I, L2F, L2D, LRETURN -> expected = BasicValue.LONG_VALUE;
      case DNEG, D2I, D2F, D2L, DRETURN -> expected = BasicValue.DOUBLE_VALUE;
      case GETFIELD -> expected = newValue(Type.forInternalName(((FieldInsnNode) insn).owner));
      case ARRAYLENGTH -> {
        if (!isArrayValue(value)) {
          throw new AnalyzerException(insn, null, "an array reference", value);
        }
        return super.unaryOperation(insn, value);
      }
      case CHECKCAST, ARETURN, ATHROW, INSTANCEOF, MONITORENTER, MONITOREXIT, IFNULL, IFNONNULL -> {
        if (!value.isReference()) {
          throw new AnalyzerException(insn, null, "an object reference", value);
        }
        return super.unaryOperation(insn, value);
      }
      case PUTSTATIC -> expected = newValue(Type.forDescriptor(((FieldInsnNode) insn).desc));
      default -> throw new AssertionError();
    }
    if (!isSubTypeOf(value, expected)) {
      throw new AnalyzerException(insn, null, expected, value);
    }
    return super.unaryOperation(insn, value);
  }

  @Override
  public BasicValue binaryOperation(
          final AbstractInsnNode insn, final BasicValue value1, final BasicValue value2)
          throws AnalyzerException {
    BasicValue expected1;
    BasicValue expected2;
    switch (insn.getOpcode()) {
      case IALOAD -> {
        expected1 = newValue(Type.forDescriptor("[I"));
        expected2 = BasicValue.INT_VALUE;
      }
      case BALOAD -> {
        if (isSubTypeOf(value1, newValue(Type.forDescriptor("[Z")))) {
          expected1 = newValue(Type.forDescriptor("[Z"));
        }
        else {
          expected1 = newValue(Type.forDescriptor("[B"));
        }
        expected2 = BasicValue.INT_VALUE;
      }
      case CALOAD -> {
        expected1 = newValue(Type.forDescriptor("[C"));
        expected2 = BasicValue.INT_VALUE;
      }
      case SALOAD -> {
        expected1 = newValue(Type.forDescriptor("[S"));
        expected2 = BasicValue.INT_VALUE;
      }
      case LALOAD -> {
        expected1 = newValue(Type.forDescriptor("[J"));
        expected2 = BasicValue.INT_VALUE;
      }
      case FALOAD -> {
        expected1 = newValue(Type.forDescriptor("[F"));
        expected2 = BasicValue.INT_VALUE;
      }
      case DALOAD -> {
        expected1 = newValue(Type.forDescriptor("[D"));
        expected2 = BasicValue.INT_VALUE;
      }
      case AALOAD -> {
        expected1 = newValue(Type.forDescriptor("[Ljava/lang/Object;"));
        expected2 = BasicValue.INT_VALUE;
      }
      case IADD, ISUB, IMUL, IDIV, IREM, ISHL, ISHR, IUSHR, IAND, IOR,
              IXOR, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE -> {
        expected1 = BasicValue.INT_VALUE;
        expected2 = BasicValue.INT_VALUE;
      }
      case FADD, FSUB, FMUL, FDIV, FREM, FCMPL, FCMPG -> {
        expected1 = BasicValue.FLOAT_VALUE;
        expected2 = BasicValue.FLOAT_VALUE;
      }
      case LADD, LSUB, LMUL, LDIV, LREM, LAND, LOR, LXOR, LCMP -> {
        expected1 = BasicValue.LONG_VALUE;
        expected2 = BasicValue.LONG_VALUE;
      }
      case LSHL, LSHR, LUSHR -> {
        expected1 = BasicValue.LONG_VALUE;
        expected2 = BasicValue.INT_VALUE;
      }
      case DADD, DSUB, DMUL, DDIV, DREM, DCMPL, DCMPG -> {
        expected1 = BasicValue.DOUBLE_VALUE;
        expected2 = BasicValue.DOUBLE_VALUE;
      }
      case IF_ACMPEQ, IF_ACMPNE -> {
        expected1 = BasicValue.REFERENCE_VALUE;
        expected2 = BasicValue.REFERENCE_VALUE;
      }
      case PUTFIELD -> {
        FieldInsnNode fieldInsn = (FieldInsnNode) insn;
        expected1 = newValue(Type.forInternalName(fieldInsn.owner));
        expected2 = newValue(Type.forDescriptor(fieldInsn.desc));
      }
      default -> throw new AssertionError();
    }
    if (!isSubTypeOf(value1, expected1)) {
      throw new AnalyzerException(insn, "First argument", expected1, value1);
    }
    else if (!isSubTypeOf(value2, expected2)) {
      throw new AnalyzerException(insn, "Second argument", expected2, value2);
    }
    if (insn.getOpcode() == AALOAD) {
      return getElementValue(value1);
    }
    else {
      return super.binaryOperation(insn, value1, value2);
    }
  }

  @Override
  public BasicValue ternaryOperation(
          final AbstractInsnNode insn,
          final BasicValue value1,
          final BasicValue value2,
          final BasicValue value3)
          throws AnalyzerException {
    BasicValue expected1;
    BasicValue expected3;
    switch (insn.getOpcode()) {
      case IASTORE -> {
        expected1 = newValue(Type.forDescriptor("[I"));
        expected3 = BasicValue.INT_VALUE;
      }
      case BASTORE -> {
        if (isSubTypeOf(value1, newValue(Type.forDescriptor("[Z")))) {
          expected1 = newValue(Type.forDescriptor("[Z"));
        }
        else {
          expected1 = newValue(Type.forDescriptor("[B"));
        }
        expected3 = BasicValue.INT_VALUE;
      }
      case CASTORE -> {
        expected1 = newValue(Type.forDescriptor("[C"));
        expected3 = BasicValue.INT_VALUE;
      }
      case SASTORE -> {
        expected1 = newValue(Type.forDescriptor("[S"));
        expected3 = BasicValue.INT_VALUE;
      }
      case LASTORE -> {
        expected1 = newValue(Type.forDescriptor("[J"));
        expected3 = BasicValue.LONG_VALUE;
      }
      case FASTORE -> {
        expected1 = newValue(Type.forDescriptor("[F"));
        expected3 = BasicValue.FLOAT_VALUE;
      }
      case DASTORE -> {
        expected1 = newValue(Type.forDescriptor("[D"));
        expected3 = BasicValue.DOUBLE_VALUE;
      }
      case AASTORE -> {
        expected1 = value1;
        expected3 = BasicValue.REFERENCE_VALUE;
      }
      default -> throw new AssertionError();
    }
    if (!isSubTypeOf(value1, expected1)) {
      throw new AnalyzerException(
              insn, "First argument", "a " + expected1 + " array reference", value1);
    }
    else if (!BasicValue.INT_VALUE.equals(value2)) {
      throw new AnalyzerException(insn, "Second argument", BasicValue.INT_VALUE, value2);
    }
    else if (!isSubTypeOf(value3, expected3)) {
      throw new AnalyzerException(insn, "Third argument", expected3, value3);
    }
    return null;
  }

  @Override
  public BasicValue naryOperation(
          final AbstractInsnNode insn, final List<? extends BasicValue> values) throws AnalyzerException {
    int opcode = insn.getOpcode();
    if (opcode == MULTIANEWARRAY) {
      for (BasicValue value : values) {
        if (!BasicValue.INT_VALUE.equals(value)) {
          throw new AnalyzerException(insn, null, BasicValue.INT_VALUE, value);
        }
      }
    }
    else {
      int i = 0;
      int j = 0;
      if (opcode != INVOKESTATIC && opcode != INVOKEDYNAMIC) {
        Type owner = Type.forInternalName(((MethodInsnNode) insn).owner);
        if (!isSubTypeOf(values.get(i++), newValue(owner))) {
          throw new AnalyzerException(insn, "Method owner", newValue(owner), values.get(0));
        }
      }
      String methodDescriptor = (opcode == INVOKEDYNAMIC) ? ((InvokeDynamicInsnNode) insn).desc : ((MethodInsnNode) insn).desc;
      Type[] args = Type.forArgumentTypes(methodDescriptor);
      int size = values.size();
      while (i < size) {
        BasicValue expected = newValue(args[j++]);
        BasicValue actual = values.get(i++);
        if (!isSubTypeOf(actual, expected)) {
          throw new AnalyzerException(insn, "Argument " + j, expected, actual);
        }
      }
    }
    return super.naryOperation(insn, values);
  }

  @Override
  public void returnOperation(
          final AbstractInsnNode insn, final BasicValue value, final BasicValue expected) throws AnalyzerException {
    if (!isSubTypeOf(value, expected)) {
      throw new AnalyzerException(insn, "Incompatible return type", expected, value);
    }
  }

  /**
   * Returns whether the given value corresponds to an array reference.
   *
   * @param value a value.
   * @return whether 'value' corresponds to an array reference.
   */
  protected boolean isArrayValue(final BasicValue value) {
    return value.isReference();
  }

  /**
   * Returns the value corresponding to the type of the elements of the given array reference value.
   *
   * @param objectArrayValue a value corresponding to array of object (or array) references.
   * @return the value corresponding to the type of the elements of 'objectArrayValue'.
   * @throws AnalyzerException if objectArrayValue does not correspond to an array type.
   */
  protected BasicValue getElementValue(final BasicValue objectArrayValue) throws AnalyzerException {
    return BasicValue.REFERENCE_VALUE;
  }

  /**
   * Returns whether the type corresponding to the first argument is a subtype of the type
   * corresponding to the second argument.
   *
   * @param value a value.
   * @param expected another value.
   * @return whether the type corresponding to 'value' is a subtype of the type corresponding to
   * 'expected'.
   */
  protected boolean isSubTypeOf(final BasicValue value, final BasicValue expected) {
    return value.equals(expected);
  }
}
