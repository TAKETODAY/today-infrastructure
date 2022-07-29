/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.bytecode.tree.analysis;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.tree.AbstractInsnNode;
import cn.taketoday.bytecode.tree.IincInsnNode;
import cn.taketoday.bytecode.tree.InvokeDynamicInsnNode;
import cn.taketoday.bytecode.tree.LabelNode;
import cn.taketoday.bytecode.tree.MethodInsnNode;
import cn.taketoday.bytecode.tree.MethodNode;
import cn.taketoday.bytecode.tree.MultiANewArrayInsnNode;
import cn.taketoday.bytecode.tree.VarInsnNode;

/**
 * A symbolic execution stack frame. A stack frame contains a set of local variable slots, and an
 * operand stack. Warning: long and double values are represented with <i>two</i> slots in local
 * variables, and with <i>one</i> slot in the operand stack.
 *
 * @param <V> type of the Value used for the analysis.
 * @author Eric Bruneton
 */
public class Frame<V extends Value> {

  /** The maximum size of the operand stack of any method. */
  private static final int MAX_STACK_SIZE = 65536;

  /**
   * The expected return type of the analyzed method, or {@literal null} if the method returns void.
   */
  private V returnValue;

  /**
   * The local variables and the operand stack of this frame. The first {@link #numLocals} elements
   * correspond to the local variables. The following {@link #numStack} elements correspond to the
   * operand stack. Long and double values are represented with two elements in the local variables
   * section, and with one element in the operand stack section.
   */
  private V[] values;

  /**
   * The number of local variables of this frame. Long and double values are represented with two
   * elements.
   */
  private int numLocals;

  /**
   * The number of elements in the operand stack. Long and double values are represented with a
   * single element.
   */
  private int numStack;

  /**
   * The maximum number of elements in the operand stack. Long and double values are represented
   * with a single element.
   */
  private int maxStack;

  /**
   * Constructs a new frame with the given size.
   *
   * @param numLocals the number of local variables of the frame. Long and double values are
   * represented with two elements.
   * @param maxStack the maximum number of elements in the operand stack, or -1 if there is no
   * maximum value. Long and double values are represented with a single element.
   */
  @SuppressWarnings("unchecked")
  public Frame(final int numLocals, final int maxStack) {
    this.values = (V[]) new Value[numLocals + (maxStack >= 0 ? maxStack : 4)];
    this.numLocals = numLocals;
    this.numStack = 0;
    this.maxStack = maxStack >= 0 ? maxStack : MAX_STACK_SIZE;
  }

  /**
   * Constructs a copy of the given Frame.
   *
   * @param frame a frame.
   */
  public Frame(final Frame<? extends V> frame) {
    this(frame.numLocals, frame.values.length - frame.numLocals);
    init(frame); // NOPMD(ConstructorCallsOverridableMethod): can't fix for backward compatibility.
  }

  /**
   * Copies the state of the given frame into this frame.
   *
   * @param frame a frame.
   * @return this frame.
   */
  public Frame<V> init(final Frame<? extends V> frame) {
    returnValue = frame.returnValue;
    if (values.length < frame.values.length) {
      values = frame.values.clone();
    }
    else {
      System.arraycopy(frame.values, 0, values, 0, frame.values.length);
    }
    numLocals = frame.numLocals;
    numStack = frame.numStack;
    maxStack = frame.maxStack;
    return this;
  }

  /**
   * Initializes a frame corresponding to the target or to the successor of a jump instruction. This
   * method is called by {@link Analyzer#analyze(String, MethodNode)} while
   * interpreting jump instructions. It is called once for each possible target of the jump
   * instruction, and once for its successor instruction (except for GOTO and JSR), before the frame
   * is merged with the existing frame at this location. The default implementation of this method
   * does nothing.
   *
   * <p>Overriding this method and changing the frame values allows implementing branch-sensitive
   * analyses.
   *
   * @param opcode the opcode of the jump instruction. Can be IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE,
   * IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE,
   * GOTO, JSR, IFNULL, IFNONNULL, TABLESWITCH or LOOKUPSWITCH.
   * @param target a target of the jump instruction this frame corresponds to, or {@literal null} if
   * this frame corresponds to the successor of the jump instruction (i.e. the next instruction
   * in the instructions sequence).
   */
  public void initJumpTarget(final int opcode, final LabelNode target) {
    // Does nothing by default.
  }

  /**
   * Sets the expected return type of the analyzed method.
   *
   * @param v the expected return type of the analyzed method, or {@literal null} if the method
   * returns void.
   */
  public void setReturn(final V v) {
    returnValue = v;
  }

  /**
   * Returns the maximum number of local variables of this frame. Long and double values are
   * represented with two variables.
   *
   * @return the maximum number of local variables of this frame.
   */
  public int getLocals() {
    return numLocals;
  }

  /**
   * Returns the maximum number of elements in the operand stack of this frame. Long and double
   * values are represented with a single element.
   *
   * @return the maximum number of elements in the operand stack of this frame.
   */
  public int getMaxStackSize() {
    return maxStack;
  }

  /**
   * Returns the value of the given local variable. Long and double values are represented with two
   * variables.
   *
   * @param index a local variable index.
   * @return the value of the given local variable.
   * @throws IndexOutOfBoundsException if the variable does not exist.
   */
  public V getLocal(final int index) {
    if (index >= numLocals) {
      throw new IndexOutOfBoundsException("Trying to get an inexistant local variable " + index);
    }
    return values[index];
  }

  /**
   * Sets the value of the given local variable. Long and double values are represented with two
   * variables.
   *
   * @param index a local variable index.
   * @param value the new value of this local variable.
   * @throws IndexOutOfBoundsException if the variable does not exist.
   */
  public void setLocal(final int index, final V value) {
    if (index >= numLocals) {
      throw new IndexOutOfBoundsException("Trying to set an inexistant local variable " + index);
    }
    values[index] = value;
  }

  /**
   * Returns the number of elements in the operand stack of this frame. Long and double values are
   * represented with a single element.
   *
   * @return the number of elements in the operand stack of this frame.
   */
  public int getStackSize() {
    return numStack;
  }

  /**
   * Returns the value of the given operand stack slot.
   *
   * @param index the index of an operand stack slot.
   * @return the value of the given operand stack slot.
   * @throws IndexOutOfBoundsException if the operand stack slot does not exist.
   */
  public V getStack(final int index) {
    return values[numLocals + index];
  }

  /**
   * Sets the value of the given stack slot.
   *
   * @param index the index of an operand stack slot.
   * @param value the new value of the stack slot.
   * @throws IndexOutOfBoundsException if the stack slot does not exist.
   */
  public void setStack(final int index, final V value) {
    values[numLocals + index] = value;
  }

  /** Clears the operand stack of this frame. */
  public void clearStack() {
    numStack = 0;
  }

  /**
   * Pops a value from the operand stack of this frame.
   *
   * @return the value that has been popped from the stack.
   * @throws IndexOutOfBoundsException if the operand stack is empty.
   */
  public V pop() {
    if (numStack == 0) {
      throw new IndexOutOfBoundsException("Cannot pop operand off an empty stack.");
    }
    return values[numLocals + (--numStack)];
  }

  /**
   * Pushes a value into the operand stack of this frame.
   *
   * @param value the value that must be pushed into the stack.
   * @throws IndexOutOfBoundsException if the operand stack is full.
   */
  @SuppressWarnings("unchecked")
  public void push(final V value) {
    if (numLocals + numStack >= values.length) {
      if (numLocals + numStack >= maxStack) {
        throw new IndexOutOfBoundsException("Insufficient maximum stack size.");
      }
      V[] oldValues = values;
      values = (V[]) new Value[2 * values.length];
      System.arraycopy(oldValues, 0, values, 0, oldValues.length);
    }
    values[numLocals + (numStack++)] = value;
  }

  /**
   * Simulates the execution of the given instruction on this execution stack frame.
   *
   * @param insn the instruction to execute.
   * @param interpreter the interpreter to use to compute values from other values.
   * @throws AnalyzerException if the instruction cannot be executed on this execution frame (e.g. a
   * POP on an empty operand stack).
   */
  public void execute(final AbstractInsnNode insn, final Interpreter<V> interpreter)
          throws AnalyzerException {
    V value1;
    V value2;
    V value3;
    V value4;
    int var;

    switch (insn.getOpcode()) {
      case Opcodes.NOP, Opcodes.RET, Opcodes.GOTO:
        break;
      case Opcodes.ACONST_NULL, Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2,
              Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5, Opcodes.LCONST_0, Opcodes.LCONST_1,
              Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2, Opcodes.DCONST_0, Opcodes.DCONST_1,
              Opcodes.BIPUSH, Opcodes.SIPUSH, Opcodes.LDC, Opcodes.NEW, Opcodes.JSR, Opcodes.GETSTATIC:
        push(interpreter.newOperation(insn));
        break;
      case Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD:
        push(interpreter.copyOperation(insn, getLocal(((VarInsnNode) insn).var)));
        break;
      case Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE: {
        value1 = interpreter.copyOperation(insn, pop());
        var = ((VarInsnNode) insn).var;
        setLocal(var, value1);
        if (value1.getSize() == 2) {
          setLocal(var + 1, interpreter.newEmptyValue(var + 1));
        }
        if (var > 0) {
          Value local = getLocal(var - 1);
          if (local != null && local.getSize() == 2) {
            setLocal(var - 1, interpreter.newEmptyValue(var - 1));
          }
        }
        break;
      }
      case Opcodes.IASTORE, Opcodes.LASTORE, Opcodes.FASTORE, Opcodes.DASTORE,
              Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE: {
        value3 = pop();
        value2 = pop();
        value1 = pop();
        interpreter.ternaryOperation(insn, value1, value2, value3);
        break;
      }
      case Opcodes.POP:
        if (pop().getSize() == 2) {
          throw new AnalyzerException(insn, "Illegal use of POP");
        }
        break;
      case Opcodes.POP2:
        if (pop().getSize() == 1 && pop().getSize() != 1) {
          throw new AnalyzerException(insn, "Illegal use of POP2");
        }
        break;
      case Opcodes.DUP:
        value1 = pop();
        if (value1.getSize() != 1) {
          throw new AnalyzerException(insn, "Illegal use of DUP");
        }
        push(value1);
        push(interpreter.copyOperation(insn, value1));
        break;
      case Opcodes.DUP_X1:
        value1 = pop();
        value2 = pop();
        if (value1.getSize() != 1 || value2.getSize() != 1) {
          throw new AnalyzerException(insn, "Illegal use of DUP_X1");
        }
        push(interpreter.copyOperation(insn, value1));
        push(value2);
        push(value1);
        break;
      case Opcodes.DUP_X2:
        value1 = pop();
        if (value1.getSize() == 1 && executeDupX2(insn, value1, interpreter)) {
          break;
        }
        throw new AnalyzerException(insn, "Illegal use of DUP_X2");
      case Opcodes.DUP2:
        value1 = pop();
        if (value1.getSize() == 1) {
          value2 = pop();
          if (value2.getSize() == 1) {
            push(value2);
            push(value1);
            push(interpreter.copyOperation(insn, value2));
            push(interpreter.copyOperation(insn, value1));
            break;
          }
        }
        else {
          push(value1);
          push(interpreter.copyOperation(insn, value1));
          break;
        }
        throw new AnalyzerException(insn, "Illegal use of DUP2");
      case Opcodes.DUP2_X1: {
        value1 = pop();
        if (value1.getSize() == 1) {
          value2 = pop();
          if (value2.getSize() == 1) {
            value3 = pop();
            if (value3.getSize() == 1) {
              push(interpreter.copyOperation(insn, value2));
              push(interpreter.copyOperation(insn, value1));
              push(value3);
              push(value2);
              push(value1);
              break;
            }
          }
        }
        else {
          value2 = pop();
          if (value2.getSize() == 1) {
            push(interpreter.copyOperation(insn, value1));
            push(value2);
            push(value1);
            break;
          }
        }
        throw new AnalyzerException(insn, "Illegal use of DUP2_X1");
      }
      case Opcodes.DUP2_X2: {
        value1 = pop();
        if (value1.getSize() == 1) {
          value2 = pop();
          if (value2.getSize() == 1) {
            value3 = pop();
            if (value3.getSize() == 1) {
              value4 = pop();
              if (value4.getSize() == 1) {
                push(interpreter.copyOperation(insn, value2));
                push(interpreter.copyOperation(insn, value1));
                push(value4);
                push(value3);
                push(value2);
                push(value1);
                break;
              }
            }
            else {
              push(interpreter.copyOperation(insn, value2));
              push(interpreter.copyOperation(insn, value1));
              push(value3);
              push(value2);
              push(value1);
              break;
            }
          }
        }
        else if (executeDupX2(insn, value1, interpreter)) {
          break;
        }
        throw new AnalyzerException(insn, "Illegal use of DUP2_X2");
      }
      case Opcodes.SWAP:
        value2 = pop();
        value1 = pop();
        if (value1.getSize() != 1 || value2.getSize() != 1) {
          throw new AnalyzerException(insn, "Illegal use of SWAP");
        }
        push(interpreter.copyOperation(insn, value2));
        push(interpreter.copyOperation(insn, value1));
        break;
      case Opcodes.IALOAD, Opcodes.LALOAD, Opcodes.FALOAD, Opcodes.DALOAD, Opcodes.AALOAD,
              Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD, Opcodes.IADD, Opcodes.LADD,
              Opcodes.FADD, Opcodes.DADD, Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB,
              Opcodes.DSUB, Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL,
              Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV, Opcodes.IREM,
              Opcodes.LREM, Opcodes.FREM, Opcodes.DREM, Opcodes.ISHL, Opcodes.LSHL,
              Opcodes.ISHR, Opcodes.LSHR, Opcodes.IUSHR, Opcodes.LUSHR, Opcodes.IAND,
              Opcodes.LAND, Opcodes.IOR, Opcodes.LOR, Opcodes.IXOR, Opcodes.LXOR,
              Opcodes.LCMP, Opcodes.FCMPL, Opcodes.FCMPG, Opcodes.DCMPL, Opcodes.DCMPG: {
        value2 = pop();
        value1 = pop();
        push(interpreter.binaryOperation(insn, value1, value2));
        break;
      }
      case Opcodes.INEG, Opcodes.LNEG, Opcodes.FNEG, Opcodes.DNEG, Opcodes.I2L, Opcodes.I2F,
              Opcodes.I2D, Opcodes.L2I, Opcodes.L2F, Opcodes.L2D, Opcodes.F2I, Opcodes.F2L,
              Opcodes.F2D, Opcodes.D2I, Opcodes.D2L, Opcodes.D2F, Opcodes.I2B, Opcodes.I2C,
              Opcodes.I2S, Opcodes.GETFIELD, Opcodes.NEWARRAY, Opcodes.ANEWARRAY,
              Opcodes.ARRAYLENGTH, Opcodes.CHECKCAST, Opcodes.INSTANCEOF: {
        push(interpreter.unaryOperation(insn, pop()));
        break;
      }
      case Opcodes.IINC:
        var = ((IincInsnNode) insn).var;
        setLocal(var, interpreter.unaryOperation(insn, getLocal(var)));
        break;
      case Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT,
              Opcodes.IFLE, Opcodes.TABLESWITCH, Opcodes.LOOKUPSWITCH, Opcodes.PUTSTATIC,
              Opcodes.ATHROW, Opcodes.MONITORENTER, Opcodes.MONITOREXIT, Opcodes.IFNULL, Opcodes.IFNONNULL: {
        interpreter.unaryOperation(insn, pop());
        break;
      }
      case Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE,
              Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE, Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE, Opcodes.PUTFIELD: {
        value2 = pop();
        value1 = pop();
        interpreter.binaryOperation(insn, value1, value2);
        break;
      }
      case Opcodes.IRETURN, Opcodes.LRETURN, Opcodes.FRETURN, Opcodes.DRETURN, Opcodes.ARETURN: {
        value1 = pop();
        interpreter.unaryOperation(insn, value1);
        interpreter.returnOperation(insn, value1, returnValue);
        break;
      }
      case Opcodes.RETURN:
        if (returnValue != null) {
          throw new AnalyzerException(insn, "Incompatible return type");
        }
        break;
      case Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESPECIAL, Opcodes.INVOKESTATIC, Opcodes.INVOKEINTERFACE: {
        executeInvokeInsn(insn, ((MethodInsnNode) insn).desc, interpreter);
        break;
      }
      case Opcodes.INVOKEDYNAMIC:
        executeInvokeInsn(insn, ((InvokeDynamicInsnNode) insn).desc, interpreter);
        break;
      case Opcodes.MULTIANEWARRAY: {
        ArrayList<V> valueList = new ArrayList<>();
        for (int i = ((MultiANewArrayInsnNode) insn).dims; i > 0; --i) {
          valueList.add(0, pop());
        }
        push(interpreter.naryOperation(insn, valueList));
        break;
      }
      default:
        throw new AnalyzerException(insn, "Illegal opcode " + insn.getOpcode());
    }
  }

  private boolean executeDupX2(
          final AbstractInsnNode insn, final V value1, final Interpreter<V> interpreter)
          throws AnalyzerException {
    V value2 = pop();
    if (value2.getSize() == 1) {
      V value3 = pop();
      if (value3.getSize() == 1) {
        push(interpreter.copyOperation(insn, value1));
        push(value3);
        push(value2);
        push(value1);
        return true;
      }
    }
    else {
      push(interpreter.copyOperation(insn, value1));
      push(value2);
      push(value1);
      return true;
    }
    return false;
  }

  private void executeInvokeInsn(
          final AbstractInsnNode insn, final String methodDescriptor, final Interpreter<V> interpreter)
          throws AnalyzerException {
    ArrayList<V> valueList = new ArrayList<>();
    for (int i = Type.getArgumentTypes(methodDescriptor).length; i > 0; --i) {
      valueList.add(0, pop());
    }
    if (insn.getOpcode() != Opcodes.INVOKESTATIC && insn.getOpcode() != Opcodes.INVOKEDYNAMIC) {
      valueList.add(0, pop());
    }
    if (Type.forReturnType(methodDescriptor) == Type.VOID_TYPE) {
      interpreter.naryOperation(insn, valueList);
    }
    else {
      push(interpreter.naryOperation(insn, valueList));
    }
  }

  /**
   * Merges the given frame into this frame.
   *
   * @param frame a frame. This frame is left unchanged by this method.
   * @param interpreter the interpreter used to merge values.
   * @return {@literal true} if this frame has been changed as a result of the merge operation, or
   * {@literal false} otherwise.
   * @throws AnalyzerException if the frames have incompatible sizes.
   */
  public boolean merge(final Frame<? extends V> frame, final Interpreter<V> interpreter)
          throws AnalyzerException {
    if (numStack != frame.numStack) {
      throw new AnalyzerException(null, "Incompatible stack heights");
    }
    boolean changed = false;
    for (int i = 0; i < numLocals + numStack; ++i) {
      V v = interpreter.merge(values[i], frame.values[i]);
      if (!v.equals(values[i])) {
        values[i] = v;
        changed = true;
      }
    }
    return changed;
  }

  /**
   * Merges the given frame into this frame (case of a subroutine). The operand stacks are not
   * merged, and only the local variables that have not been used by the subroutine are merged.
   *
   * @param frame a frame. This frame is left unchanged by this method.
   * @param localsUsed the local variables that are read or written by the subroutine. The i-th
   * element is true if and only if the local variable at index i is read or written by the
   * subroutine.
   * @return {@literal true} if this frame has been changed as a result of the merge operation, or
   * {@literal false} otherwise.
   */
  public boolean merge(final Frame<? extends V> frame, final boolean[] localsUsed) {
    boolean changed = false;
    for (int i = 0; i < numLocals; ++i) {
      if (!localsUsed[i] && !values[i].equals(frame.values[i])) {
        values[i] = frame.values[i];
        changed = true;
      }
    }
    return changed;
  }

  /**
   * Returns a string representation of this frame.
   *
   * @return a string representation of this frame.
   */
  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < getLocals(); ++i) {
      stringBuilder.append(getLocal(i));
    }
    stringBuilder.append(' ');
    for (int i = 0; i < getStackSize(); ++i) {
      stringBuilder.append(getStack(i).toString());
    }
    return stringBuilder.toString();
  }
}
