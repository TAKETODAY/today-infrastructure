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

package cn.taketoday.core.bytecode.commons;

import cn.taketoday.core.bytecode.ConstantDynamic;
import cn.taketoday.core.bytecode.Handle;
import cn.taketoday.core.bytecode.Label;
import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;

/**
 * A {@link MethodVisitor} providing a more detailed API to generate and transform instructions.
 *
 * @author Eric Bruneton
 */
public class InstructionAdapter extends MethodVisitor {

  /**
   * Constructs a new {@link InstructionAdapter}.
   *
   * @param methodVisitor the method visitor to which this adapter delegates calls.
   * @throws IllegalStateException If a subclass calls this constructor.
   */
  public InstructionAdapter(final MethodVisitor methodVisitor) {
    super(methodVisitor);
  }

  @Override
  public void visitInsn(final int opcode) {
    switch (opcode) {
      case Opcodes.NOP -> nop();
      case Opcodes.ACONST_NULL -> aconst(null);
      case Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1,
              Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4,
              Opcodes.ICONST_5 -> iconst(opcode - Opcodes.ICONST_0);
      case Opcodes.LCONST_0, Opcodes.LCONST_1 -> lconst(opcode - Opcodes.LCONST_0);
      case Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2 -> fconst((float) (opcode - Opcodes.FCONST_0));
      case Opcodes.DCONST_0, Opcodes.DCONST_1 -> dconst(opcode - Opcodes.DCONST_0);
      case Opcodes.IALOAD -> aload(Type.INT_TYPE);
      case Opcodes.LALOAD -> aload(Type.LONG_TYPE);
      case Opcodes.FALOAD -> aload(Type.FLOAT_TYPE);
      case Opcodes.DALOAD -> aload(Type.DOUBLE_TYPE);
      case Opcodes.AALOAD -> aload(Type.TYPE_OBJECT);
      case Opcodes.BALOAD -> aload(Type.BYTE_TYPE);
      case Opcodes.CALOAD -> aload(Type.CHAR_TYPE);
      case Opcodes.SALOAD -> aload(Type.SHORT_TYPE);
      case Opcodes.IASTORE -> astore(Type.INT_TYPE);
      case Opcodes.LASTORE -> astore(Type.LONG_TYPE);
      case Opcodes.FASTORE -> astore(Type.FLOAT_TYPE);
      case Opcodes.DASTORE -> astore(Type.DOUBLE_TYPE);
      case Opcodes.AASTORE -> astore(Type.TYPE_OBJECT);
      case Opcodes.BASTORE -> astore(Type.BYTE_TYPE);
      case Opcodes.CASTORE -> astore(Type.CHAR_TYPE);
      case Opcodes.SASTORE -> astore(Type.SHORT_TYPE);
      case Opcodes.POP -> pop();
      case Opcodes.POP2 -> pop2();
      case Opcodes.DUP -> dup();
      case Opcodes.DUP_X1 -> dupX1();
      case Opcodes.DUP_X2 -> dupX2();
      case Opcodes.DUP2 -> dup2();
      case Opcodes.DUP2_X1 -> dup2X1();
      case Opcodes.DUP2_X2 -> dup2X2();
      case Opcodes.SWAP -> swap();
      case Opcodes.IADD -> add(Type.INT_TYPE);
      case Opcodes.LADD -> add(Type.LONG_TYPE);
      case Opcodes.FADD -> add(Type.FLOAT_TYPE);
      case Opcodes.DADD -> add(Type.DOUBLE_TYPE);
      case Opcodes.ISUB -> sub(Type.INT_TYPE);
      case Opcodes.LSUB -> sub(Type.LONG_TYPE);
      case Opcodes.FSUB -> sub(Type.FLOAT_TYPE);
      case Opcodes.DSUB -> sub(Type.DOUBLE_TYPE);
      case Opcodes.IMUL -> mul(Type.INT_TYPE);
      case Opcodes.LMUL -> mul(Type.LONG_TYPE);
      case Opcodes.FMUL -> mul(Type.FLOAT_TYPE);
      case Opcodes.DMUL -> mul(Type.DOUBLE_TYPE);
      case Opcodes.IDIV -> div(Type.INT_TYPE);
      case Opcodes.LDIV -> div(Type.LONG_TYPE);
      case Opcodes.FDIV -> div(Type.FLOAT_TYPE);
      case Opcodes.DDIV -> div(Type.DOUBLE_TYPE);
      case Opcodes.IREM -> rem(Type.INT_TYPE);
      case Opcodes.LREM -> rem(Type.LONG_TYPE);
      case Opcodes.FREM -> rem(Type.FLOAT_TYPE);
      case Opcodes.DREM -> rem(Type.DOUBLE_TYPE);
      case Opcodes.INEG -> neg(Type.INT_TYPE);
      case Opcodes.LNEG -> neg(Type.LONG_TYPE);
      case Opcodes.FNEG -> neg(Type.FLOAT_TYPE);
      case Opcodes.DNEG -> neg(Type.DOUBLE_TYPE);
      case Opcodes.ISHL -> shl(Type.INT_TYPE);
      case Opcodes.LSHL -> shl(Type.LONG_TYPE);
      case Opcodes.ISHR -> shr(Type.INT_TYPE);
      case Opcodes.LSHR -> shr(Type.LONG_TYPE);
      case Opcodes.IUSHR -> ushr(Type.INT_TYPE);
      case Opcodes.LUSHR -> ushr(Type.LONG_TYPE);
      case Opcodes.IAND -> and(Type.INT_TYPE);
      case Opcodes.LAND -> and(Type.LONG_TYPE);
      case Opcodes.IOR -> or(Type.INT_TYPE);
      case Opcodes.LOR -> or(Type.LONG_TYPE);
      case Opcodes.IXOR -> xor(Type.INT_TYPE);
      case Opcodes.LXOR -> xor(Type.LONG_TYPE);
      case Opcodes.I2L -> cast(Type.INT_TYPE, Type.LONG_TYPE);
      case Opcodes.I2F -> cast(Type.INT_TYPE, Type.FLOAT_TYPE);
      case Opcodes.I2D -> cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
      case Opcodes.L2I -> cast(Type.LONG_TYPE, Type.INT_TYPE);
      case Opcodes.L2F -> cast(Type.LONG_TYPE, Type.FLOAT_TYPE);
      case Opcodes.L2D -> cast(Type.LONG_TYPE, Type.DOUBLE_TYPE);
      case Opcodes.F2I -> cast(Type.FLOAT_TYPE, Type.INT_TYPE);
      case Opcodes.F2L -> cast(Type.FLOAT_TYPE, Type.LONG_TYPE);
      case Opcodes.F2D -> cast(Type.FLOAT_TYPE, Type.DOUBLE_TYPE);
      case Opcodes.D2I -> cast(Type.DOUBLE_TYPE, Type.INT_TYPE);
      case Opcodes.D2L -> cast(Type.DOUBLE_TYPE, Type.LONG_TYPE);
      case Opcodes.D2F -> cast(Type.DOUBLE_TYPE, Type.FLOAT_TYPE);
      case Opcodes.I2B -> cast(Type.INT_TYPE, Type.BYTE_TYPE);
      case Opcodes.I2C -> cast(Type.INT_TYPE, Type.CHAR_TYPE);
      case Opcodes.I2S -> cast(Type.INT_TYPE, Type.SHORT_TYPE);
      case Opcodes.LCMP -> lcmp();
      case Opcodes.FCMPL -> cmpl(Type.FLOAT_TYPE);
      case Opcodes.FCMPG -> cmpg(Type.FLOAT_TYPE);
      case Opcodes.DCMPL -> cmpl(Type.DOUBLE_TYPE);
      case Opcodes.DCMPG -> cmpg(Type.DOUBLE_TYPE);
      case Opcodes.IRETURN -> areturn(Type.INT_TYPE);
      case Opcodes.LRETURN -> areturn(Type.LONG_TYPE);
      case Opcodes.FRETURN -> areturn(Type.FLOAT_TYPE);
      case Opcodes.DRETURN -> areturn(Type.DOUBLE_TYPE);
      case Opcodes.ARETURN -> areturn(Type.TYPE_OBJECT);
      case Opcodes.RETURN -> areturn(Type.VOID_TYPE);
      case Opcodes.ARRAYLENGTH -> arraylength();
      case Opcodes.ATHROW -> athrow();
      case Opcodes.MONITORENTER -> monitorenter();
      case Opcodes.MONITOREXIT -> monitorexit();
      default -> throw new IllegalArgumentException();
    }
  }

  @Override
  public void visitIntInsn(final int opcode, final int operand) {
    switch (opcode) {
      case Opcodes.BIPUSH:
      case Opcodes.SIPUSH:
        iconst(operand);
        break;
      case Opcodes.NEWARRAY:
        switch (operand) {
          case Opcodes.T_INT -> newArray(Type.INT_TYPE);
          case Opcodes.T_CHAR -> newArray(Type.CHAR_TYPE);
          case Opcodes.T_LONG -> newArray(Type.LONG_TYPE);
          case Opcodes.T_BYTE -> newArray(Type.BYTE_TYPE);
          case Opcodes.T_FLOAT -> newArray(Type.FLOAT_TYPE);
          case Opcodes.T_SHORT -> newArray(Type.SHORT_TYPE);
          case Opcodes.T_DOUBLE -> newArray(Type.DOUBLE_TYPE);
          case Opcodes.T_BOOLEAN -> newArray(Type.BOOLEAN_TYPE);
          default -> throw new IllegalArgumentException();
        }
        break;
      default:
        throw new IllegalArgumentException();
    }
  }

  @Override
  public void visitVarInsn(final int opcode, final int var) {
    switch (opcode) {
      case Opcodes.ILOAD -> load(var, Type.INT_TYPE);
      case Opcodes.LLOAD -> load(var, Type.LONG_TYPE);
      case Opcodes.FLOAD -> load(var, Type.FLOAT_TYPE);
      case Opcodes.DLOAD -> load(var, Type.DOUBLE_TYPE);
      case Opcodes.ALOAD -> load(var, Type.TYPE_OBJECT);
      case Opcodes.ISTORE -> store(var, Type.INT_TYPE);
      case Opcodes.LSTORE -> store(var, Type.LONG_TYPE);
      case Opcodes.FSTORE -> store(var, Type.FLOAT_TYPE);
      case Opcodes.DSTORE -> store(var, Type.DOUBLE_TYPE);
      case Opcodes.ASTORE -> store(var, Type.TYPE_OBJECT);
      case Opcodes.RET -> ret(var);
      default -> throw new IllegalArgumentException();
    }
  }

  @Override
  public void visitTypeInsn(final int opcode, final String type) {
    Type objectType = Type.fromInternalName(type);
    switch (opcode) {
      case Opcodes.NEW -> anew(objectType);
      case Opcodes.ANEWARRAY -> newArray(objectType);
      case Opcodes.CHECKCAST -> checkcast(objectType);
      case Opcodes.INSTANCEOF -> instanceOf(objectType);
      default -> throw new IllegalArgumentException();
    }
  }

  @Override
  public void visitFieldInsn(
          final int opcode, final String owner, final String name, final String descriptor) {
    switch (opcode) {
      case Opcodes.GETSTATIC -> getStatic(owner, name, descriptor);
      case Opcodes.PUTSTATIC -> putStatic(owner, name, descriptor);
      case Opcodes.GETFIELD -> getField(owner, name, descriptor);
      case Opcodes.PUTFIELD -> putField(owner, name, descriptor);
      default -> throw new IllegalArgumentException();
    }
  }

  @Override
  public void visitMethodInsn(
          final int opcodeAndSource,
          final String owner,
          final String name,
          final String descriptor,
          final boolean isInterface) {
    int opcode = opcodeAndSource & ~Opcodes.SOURCE_MASK;

    switch (opcode) {
      case Opcodes.INVOKESPECIAL -> invokeSpecial(owner, name, descriptor, isInterface);
      case Opcodes.INVOKEVIRTUAL -> invokeVirtual(owner, name, descriptor, isInterface);
      case Opcodes.INVOKESTATIC -> invokeStatic(owner, name, descriptor, isInterface);
      case Opcodes.INVOKEINTERFACE -> invokeInterface(owner, name, descriptor);
      default -> throw new IllegalArgumentException();
    }
  }

  @Override
  public void visitInvokeDynamicInsn(
          final String name,
          final String descriptor,
          final Handle bootstrapMethodHandle,
          final Object... bootstrapMethodArguments) {
    invokeDynamic(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
  }

  @Override
  public void visitJumpInsn(final int opcode, final Label label) {
    switch (opcode) {
      case Opcodes.IFEQ -> ifeq(label);
      case Opcodes.IFNE -> ifne(label);
      case Opcodes.IFLT -> iflt(label);
      case Opcodes.IFGE -> ifge(label);
      case Opcodes.IFGT -> ifgt(label);
      case Opcodes.IFLE -> ifle(label);
      case Opcodes.IF_ICMPEQ -> ificmpeq(label);
      case Opcodes.IF_ICMPNE -> ificmpne(label);
      case Opcodes.IF_ICMPLT -> ificmplt(label);
      case Opcodes.IF_ICMPGE -> ificmpge(label);
      case Opcodes.IF_ICMPGT -> ificmpgt(label);
      case Opcodes.IF_ICMPLE -> ificmple(label);
      case Opcodes.IF_ACMPEQ -> ifacmpeq(label);
      case Opcodes.IF_ACMPNE -> ifacmpne(label);
      case Opcodes.GOTO -> goTo(label);
      case Opcodes.JSR -> jsr(label);
      case Opcodes.IFNULL -> ifnull(label);
      case Opcodes.IFNONNULL -> ifnonnull(label);
      default -> throw new IllegalArgumentException();
    }
  }

  @Override
  public void visitLabel(final Label label) {
    mark(label);
  }

  @Override
  public void visitLdcInsn(final Object value) {
    if (value instanceof Integer) {
      iconst((Integer) value);
    }
    else if (value instanceof Byte) {
      iconst(((Byte) value).intValue());
    }
    else if (value instanceof Character) {
      iconst((Character) value);
    }
    else if (value instanceof Short) {
      iconst(((Short) value).intValue());
    }
    else if (value instanceof Boolean) {
      iconst((Boolean) value ? 1 : 0);
    }
    else if (value instanceof Float) {
      fconst((Float) value);
    }
    else if (value instanceof Long) {
      lconst((Long) value);
    }
    else if (value instanceof Double) {
      dconst((Double) value);
    }
    else if (value instanceof String) {
      aconst(value);
    }
    else if (value instanceof Type) {
      tconst((Type) value);
    }
    else if (value instanceof Handle) {
      hconst((Handle) value);
    }
    else if (value instanceof ConstantDynamic) {
      cconst((ConstantDynamic) value);
    }
    else {
      throw new IllegalArgumentException();
    }
  }

  @Override
  public void visitIincInsn(final int var, final int increment) {
    iinc(var, increment);
  }

  @Override
  public void visitTableSwitchInsn(
          final int min, final int max, final Label dflt, final Label... labels) {
    tableSwitch(min, max, dflt, labels);
  }

  @Override
  public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
    lookupSwitch(dflt, keys, labels);
  }

  @Override
  public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
    multianewarray(descriptor, numDimensions);
  }

  // -----------------------------------------------------------------------------------------------

  /** Generates a nop instruction. */
  public void nop() {
    mv.visitInsn(Opcodes.NOP);
  }

  /**
   * Generates the instruction to push the given value on the stack.
   *
   * @param value the constant to be pushed on the stack. This parameter must be an {@link Integer},
   * a {@link Float}, a {@link Long}, a {@link Double}, a {@link String}, a {@link Type} of
   * OBJECT or ARRAY sort for {@code .class} constants, for classes whose version is 49, a
   * {@link Type} of METHOD sort for MethodType, a {@link Handle} for MethodHandle constants,
   * for classes whose version is 51 or a {@link ConstantDynamic} for a constant dynamic for
   * classes whose version is 55.
   */
  public void aconst(final Object value) {
    if (value == null) {
      mv.visitInsn(Opcodes.ACONST_NULL);
    }
    else {
      mv.visitLdcInsn(value);
    }
  }

  /**
   * Generates the instruction to push the given value on the stack.
   *
   * @param intValue the constant to be pushed on the stack.
   */
  public void iconst(final int intValue) {
    push(mv, intValue);
  }

  public static void push(MethodVisitor mv, int value) {
    if (value >= -1 && value <= 5) {
      mv.visitInsn(Opcodes.ICONST_0 + value);
    }
    else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
      mv.visitIntInsn(Opcodes.BIPUSH, value);
    }
    else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
      mv.visitIntInsn(Opcodes.SIPUSH, value);
    }
    else {
      mv.visitLdcInsn(value);
    }
  }

  /**
   * Generates the instruction to push the given value on the stack.
   *
   * @param longValue the constant to be pushed on the stack.
   */
  public void lconst(final long longValue) {
    if (longValue == 0L || longValue == 1L) {
      mv.visitInsn(Opcodes.LCONST_0 + (int) longValue);
    }
    else {
      mv.visitLdcInsn(longValue);
    }
  }

  /**
   * Generates the instruction to push the given value on the stack.
   *
   * @param floatValue the constant to be pushed on the stack.
   */
  public void fconst(final float floatValue) {
    int bits = Float.floatToIntBits(floatValue);
    if (bits == 0L || bits == 0x3F800000 || bits == 0x40000000) { // 0..2
      mv.visitInsn(Opcodes.FCONST_0 + (int) floatValue);
    }
    else {
      mv.visitLdcInsn(floatValue);
    }
  }

  /**
   * Generates the instruction to push the given value on the stack.
   *
   * @param doubleValue the constant to be pushed on the stack.
   */
  public void dconst(final double doubleValue) {
    long bits = Double.doubleToLongBits(doubleValue);
    if (bits == 0L || bits == 0x3FF0000000000000L) { // +0.0d and 1.0d
      mv.visitInsn(Opcodes.DCONST_0 + (int) doubleValue);
    }
    else {
      mv.visitLdcInsn(doubleValue);
    }
  }

  /**
   * Generates the instruction to push the given type on the stack.
   *
   * @param type the type to be pushed on the stack.
   */
  public void tconst(final Type type) {
    mv.visitLdcInsn(type);
  }

  /**
   * Generates the instruction to push the given handle on the stack.
   *
   * @param handle the handle to be pushed on the stack.
   */
  public void hconst(final Handle handle) {
    mv.visitLdcInsn(handle);
  }

  /**
   * Generates the instruction to push the given constant dynamic on the stack.
   *
   * @param constantDynamic the constant dynamic to be pushed on the stack.
   */
  public void cconst(final ConstantDynamic constantDynamic) {
    mv.visitLdcInsn(constantDynamic);
  }

  public void load(final int var, final Type type) {
    mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), var);
  }

  public void aload(final Type type) {
    mv.visitInsn(type.getOpcode(Opcodes.IALOAD));
  }

  public void store(final int var, final Type type) {
    mv.visitVarInsn(type.getOpcode(Opcodes.ISTORE), var);
  }

  public void aastore() {
    mv.visitInsn(Opcodes.AASTORE);
  }

  public void astore(final Type type) {
    mv.visitInsn(type.getOpcode(Opcodes.IASTORE));
  }

  public void pop() {
    mv.visitInsn(Opcodes.POP);
  }

  public void pop2() {
    mv.visitInsn(Opcodes.POP2);
  }

  public void dup() {
    mv.visitInsn(Opcodes.DUP);
  }

  public void dup2() {
    mv.visitInsn(Opcodes.DUP2);
  }

  public void dupX1() {
    mv.visitInsn(Opcodes.DUP_X1);
  }

  public void dupX2() {
    mv.visitInsn(Opcodes.DUP_X2);
  }

  public void dup2X1() {
    mv.visitInsn(Opcodes.DUP2_X1);
  }

  public void dup2X2() {
    mv.visitInsn(Opcodes.DUP2_X2);
  }

  public void swap() {
    mv.visitInsn(Opcodes.SWAP);
  }

  public void add(final Type type) {
    mv.visitInsn(type.getOpcode(Opcodes.IADD));
  }

  public void sub(final Type type) {
    mv.visitInsn(type.getOpcode(Opcodes.ISUB));
  }

  public void mul(final Type type) {
    mv.visitInsn(type.getOpcode(Opcodes.IMUL));
  }

  public void div(final Type type) {
    mv.visitInsn(type.getOpcode(Opcodes.IDIV));
  }

  public void rem(final Type type) {
    mv.visitInsn(type.getOpcode(Opcodes.IREM));
  }

  public void neg(final Type type) {
    mv.visitInsn(type.getOpcode(Opcodes.INEG));
  }

  public void shl(final Type type) {
    mv.visitInsn(type.getOpcode(Opcodes.ISHL));
  }

  public void shr(final Type type) {
    mv.visitInsn(type.getOpcode(Opcodes.ISHR));
  }

  public void ushr(final Type type) {
    mv.visitInsn(type.getOpcode(Opcodes.IUSHR));
  }

  public void and(final Type type) {
    mv.visitInsn(type.getOpcode(Opcodes.IAND));
  }

  public void or(final Type type) {
    mv.visitInsn(type.getOpcode(Opcodes.IOR));
  }

  public void xor(final Type type) {
    mv.visitInsn(type.getOpcode(Opcodes.IXOR));
  }

  public void iinc(final int var, final int increment) {
    mv.visitIincInsn(var, increment);
  }

  /**
   * Generates the instruction to cast from the first given type to the other.
   *
   * @param from a Type.
   * @param to a Type.
   */
  public void cast(final Type from, final Type to) {
    cast(mv, from, to);
  }

  /**
   * Generates the instruction to cast from the first given type to the other.
   *
   * @param methodVisitor the method visitor to use to generate the instruction.
   * @param from a Type.
   * @param to a Type.
   */
  public static void cast(final MethodVisitor methodVisitor, final Type from, final Type to) {
    if (from != to) {
      if (from == Type.DOUBLE_TYPE) {
        if (to == Type.FLOAT_TYPE) {
          methodVisitor.visitInsn(Opcodes.D2F);
        }
        else if (to == Type.LONG_TYPE) {
          methodVisitor.visitInsn(Opcodes.D2L);
        }
        else {
          methodVisitor.visitInsn(Opcodes.D2I);
          cast(methodVisitor, Type.INT_TYPE, to);
        }
      }
      else if (from == Type.FLOAT_TYPE) {
        if (to == Type.DOUBLE_TYPE) {
          methodVisitor.visitInsn(Opcodes.F2D);
        }
        else if (to == Type.LONG_TYPE) {
          methodVisitor.visitInsn(Opcodes.F2L);
        }
        else {
          methodVisitor.visitInsn(Opcodes.F2I);
          cast(methodVisitor, Type.INT_TYPE, to);
        }
      }
      else if (from == Type.LONG_TYPE) {
        if (to == Type.DOUBLE_TYPE) {
          methodVisitor.visitInsn(Opcodes.L2D);
        }
        else if (to == Type.FLOAT_TYPE) {
          methodVisitor.visitInsn(Opcodes.L2F);
        }
        else {
          methodVisitor.visitInsn(Opcodes.L2I);
          cast(methodVisitor, Type.INT_TYPE, to);
        }
      }
      else {
        if (to == Type.BYTE_TYPE) {
          methodVisitor.visitInsn(Opcodes.I2B);
        }
        else if (to == Type.CHAR_TYPE) {
          methodVisitor.visitInsn(Opcodes.I2C);
        }
        else if (to == Type.DOUBLE_TYPE) {
          methodVisitor.visitInsn(Opcodes.I2D);
        }
        else if (to == Type.FLOAT_TYPE) {
          methodVisitor.visitInsn(Opcodes.I2F);
        }
        else if (to == Type.LONG_TYPE) {
          methodVisitor.visitInsn(Opcodes.I2L);
        }
        else if (to == Type.SHORT_TYPE) {
          methodVisitor.visitInsn(Opcodes.I2S);
        }
      }
    }
  }

  public void lcmp() {
    mv.visitInsn(Opcodes.LCMP);
  }

  public void cmpl(final Type type) {
    mv.visitInsn(type == Type.FLOAT_TYPE ? Opcodes.FCMPL : Opcodes.DCMPL);
  }

  public void cmpg(final Type type) {
    mv.visitInsn(type == Type.FLOAT_TYPE ? Opcodes.FCMPG : Opcodes.DCMPG);
  }

  public void ifeq(final Label label) {
    mv.visitJumpInsn(Opcodes.IFEQ, label);
  }

  public void ifne(final Label label) {
    mv.visitJumpInsn(Opcodes.IFNE, label);
  }

  public void iflt(final Label label) {
    mv.visitJumpInsn(Opcodes.IFLT, label);
  }

  public void ifge(final Label label) {
    mv.visitJumpInsn(Opcodes.IFGE, label);
  }

  public void ifgt(final Label label) {
    mv.visitJumpInsn(Opcodes.IFGT, label);
  }

  public void ifle(final Label label) {
    mv.visitJumpInsn(Opcodes.IFLE, label);
  }

  public void ificmpeq(final Label label) {
    mv.visitJumpInsn(Opcodes.IF_ICMPEQ, label);
  }

  public void ificmpne(final Label label) {
    mv.visitJumpInsn(Opcodes.IF_ICMPNE, label);
  }

  public void ificmplt(final Label label) {
    mv.visitJumpInsn(Opcodes.IF_ICMPLT, label);
  }

  public void ificmpge(final Label label) {
    mv.visitJumpInsn(Opcodes.IF_ICMPGE, label);
  }

  public void ificmpgt(final Label label) {
    mv.visitJumpInsn(Opcodes.IF_ICMPGT, label);
  }

  public void ificmple(final Label label) {
    mv.visitJumpInsn(Opcodes.IF_ICMPLE, label);
  }

  public void ifacmpeq(final Label label) {
    mv.visitJumpInsn(Opcodes.IF_ACMPEQ, label);
  }

  public void ifacmpne(final Label label) {
    mv.visitJumpInsn(Opcodes.IF_ACMPNE, label);
  }

  public void goTo(final Label label) {
    mv.visitJumpInsn(Opcodes.GOTO, label);
  }

  public void jsr(final Label label) {
    mv.visitJumpInsn(Opcodes.JSR, label);
  }

  public void ret(final int var) {
    mv.visitVarInsn(Opcodes.RET, var);
  }

  public void tableSwitch(final int min, final int max, final Label dflt, final Label... labels) {
    mv.visitTableSwitchInsn(min, max, dflt, labels);
  }

  public void lookupSwitch(final Label dflt, final int[] keys, final Label[] labels) {
    mv.visitLookupSwitchInsn(dflt, keys, labels);
  }

  public void areturn(final Type type) {
    mv.visitInsn(type.getOpcode(Opcodes.IRETURN));
  }

  public void getStatic(final String owner, final String name, final String descriptor) {
    mv.visitFieldInsn(Opcodes.GETSTATIC, owner, name, descriptor);
  }

  public void putStatic(final String owner, final String name, final String descriptor) {
    mv.visitFieldInsn(Opcodes.PUTSTATIC, owner, name, descriptor);
  }

  public void getField(final String owner, final String name, final String descriptor) {
    mv.visitFieldInsn(Opcodes.GETFIELD, owner, name, descriptor);
  }

  public void putField(final String owner, final String name, final String descriptor) {
    mv.visitFieldInsn(Opcodes.PUTFIELD, owner, name, descriptor);
  }

  /**
   * Deprecated.
   *
   * @param owner the internal name of the method's owner class.
   * @param name the method's name.
   * @param descriptor the method's descriptor (see {@link Type}).
   */
  public void invokeVirtual(final String owner, final String name, final String descriptor) {
    invokeVirtual(owner, name, descriptor, false);
  }

  /**
   * Generates the instruction to call the given virtual method.
   *
   * @param owner the internal name of the method's owner class (see {@link
   * Type#getInternalName()}).
   * @param name the method's name.
   * @param descriptor the method's descriptor (see {@link Type}).
   * @param isInterface if the method's owner class is an interface.
   */
  public void invokeVirtual(
          final String owner, final String name, final String descriptor, final boolean isInterface) {
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, descriptor, isInterface);
  }

  /**
   * Deprecated.
   *
   * @param owner the internal name of the method's owner class.
   * @param name the method's name.
   * @param descriptor the method's descriptor (see {@link Type}).
   */
  public void invokeSpecial(final String owner, final String name, final String descriptor) {
    invokeSpecial(owner, name, descriptor, false);
  }

  /**
   * Generates the instruction to call the given special method.
   *
   * @param owner the internal name of the method's owner class (see {@link
   * Type#getInternalName()}).
   * @param name the method's name.
   * @param descriptor the method's descriptor (see {@link Type}).
   * @param isInterface if the method's owner class is an interface.
   */
  public void invokeSpecial(
          final String owner, final String name, final String descriptor, final boolean isInterface) {
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, name, descriptor, isInterface);
  }

  /**
   * Deprecated.
   *
   * @param owner the internal name of the method's owner class.
   * @param name the method's name.
   * @param descriptor the method's descriptor (see {@link Type}).
   */
  public void invokeStatic(final String owner, final String name, final String descriptor) {
    invokeStatic(owner, name, descriptor, false);
  }

  /**
   * Generates the instruction to call the given static method.
   *
   * @param owner the internal name of the method's owner class (see {@link
   * Type#getInternalName()}).
   * @param name the method's name.
   * @param descriptor the method's descriptor (see {@link Type}).
   * @param isInterface if the method's owner class is an interface.
   */
  public void invokeStatic(
          final String owner, final String name, final String descriptor, final boolean isInterface) {
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, descriptor, isInterface);
  }

  /**
   * Generates the instruction to call the given interface method.
   *
   * @param owner the internal name of the method's owner class (see {@link
   * Type#getInternalName()}).
   * @param name the method's name.
   * @param descriptor the method's descriptor (see {@link Type}).
   */
  public void invokeInterface(final String owner, final String name, final String descriptor) {
    mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, owner, name, descriptor, true);
  }

  /**
   * Generates the instruction to call the given dynamic method.
   *
   * @param name the method's name.
   * @param descriptor the method's descriptor (see {@link Type}).
   * @param bootstrapMethodHandle the bootstrap method.
   * @param bootstrapMethodArguments the bootstrap method constant arguments. Each argument must be
   * an {@link Integer}, {@link Float}, {@link Long}, {@link Double}, {@link String}, {@link
   * Type}, {@link Handle} or {@link ConstantDynamic} value. This method is allowed to modify
   * the content of the array so a caller should expect that this array may change.
   */
  public void invokeDynamic(
          final String name,
          final String descriptor,
          final Handle bootstrapMethodHandle,
          final Object[] bootstrapMethodArguments) {
    mv.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
  }

  public void anew(final Type type) {
    mv.visitTypeInsn(Opcodes.NEW, type.getInternalName());
  }

  /**
   * Generates the instruction to create and push on the stack an array of the given type.
   *
   * @param type an array Type.
   */
  public void newArray(final Type type) {
    newArray(mv, type);
  }

  /**
   * Generates the instruction to create and push on the stack an array of the given type.
   *
   * @param methodVisitor the method visitor to use to generate the instruction.
   * @param type an array Type.
   */
  public static void newArray(final MethodVisitor methodVisitor, final Type type) {
    int arrayType;
    switch (type.getSort()) {
      case Type.CHAR -> arrayType = Opcodes.T_CHAR;
      case Type.BYTE -> arrayType = Opcodes.T_BYTE;
      case Type.INT -> arrayType = Opcodes.T_INT;
      case Type.LONG -> arrayType = Opcodes.T_LONG;
      case Type.SHORT -> arrayType = Opcodes.T_SHORT;
      case Type.FLOAT -> arrayType = Opcodes.T_FLOAT;
      case Type.DOUBLE -> arrayType = Opcodes.T_DOUBLE;
      case Type.BOOLEAN -> arrayType = Opcodes.T_BOOLEAN;
      default -> {
        methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, type.getInternalName());
        return;
      }
    }
    methodVisitor.visitIntInsn(Opcodes.NEWARRAY, arrayType);
  }

  public void arraylength() {
    mv.visitInsn(Opcodes.ARRAYLENGTH);
  }

  public void athrow() {
    mv.visitInsn(Opcodes.ATHROW);
  }

  public void checkcast(final Type type) {
    mv.visitTypeInsn(Opcodes.CHECKCAST, type.getInternalName());
  }

  public void instanceOf(final Type type) {
    mv.visitTypeInsn(Opcodes.INSTANCEOF, type.getInternalName());
  }

  public void monitorenter() {
    mv.visitInsn(Opcodes.MONITORENTER);
  }

  public void monitorexit() {
    mv.visitInsn(Opcodes.MONITOREXIT);
  }

  public void multianewarray(final String descriptor, final int numDimensions) {
    mv.visitMultiANewArrayInsn(descriptor, numDimensions);
  }

  public void ifnull(final Label label) {
    mv.visitJumpInsn(Opcodes.IFNULL, label);
  }

  public void ifnonnull(final Label label) {
    mv.visitJumpInsn(Opcodes.IFNONNULL, label);
  }

  public void mark(final Label label) {
    mv.visitLabel(label);
  }
}
