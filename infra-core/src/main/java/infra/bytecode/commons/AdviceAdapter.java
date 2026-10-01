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

package infra.bytecode.commons;

import java.util.HashMap;
import java.util.Map;

import infra.bytecode.ConstantDynamic;
import infra.bytecode.Handle;
import infra.bytecode.Label;
import infra.bytecode.LimitExceededException;
import infra.bytecode.MethodVisitor;
import infra.bytecode.Opcodes;
import infra.bytecode.Type;

/**
 * A {@link MethodVisitor} to insert before, after and around advices in methods and constructors.
 * For constructors, the code keeps track of the elements on the stack and in local variables in
 * order to detect when the super class constructor is called (note that there can be multiple such
 * calls in different branches). {@code onMethodEnter} is called after each super class constructor
 * call, because the object cannot be used before it is properly initialized.
 *
 * <p><b>Note:</b> <i>this adapter does not work for arbitrary constructors if stack map frames are
 * not present</i> (if they are, it supports arbitrary valid code). Instead, it assumes that the
 * code has been produced by a Java compiler. In particular it assumes that, in constructors:
 *
 * <ul>
 *   <li>the uninitialized instance is in local variable 0,
 *   <li>the uninitialized instance is never copied into another local variable,
 *   <li>there are no backward jumps when the stack map frame contains the uninitialized instance.
 * </ul>
 *
 * <p>In these hypotheses are not true, and if there are no stack map frames, {@code onMethodEnter}
 * might not be called at all, or might be called at the wrong place.
 *
 * @author Eugene Kuleshov
 * @author Eric Bruneton
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AdviceAdapter extends GeneratorAdapter implements Opcodes {

  /** The "uninitialized this" value. */
  private static final boolean UNINITIALIZED_THIS = true;

  /** Any value other than "uninitialized this". */
  private static final boolean OTHER = false;

  /** Prefix of the error message when invalid opcodes are found. */
  private static final String INVALID_OPCODE = "Invalid opcode ";

  /**
   * The default max memory limit for {@link #setComputeLimits}. Update the comment in {@link
   * #setComputeLimits} is you change this value.
   */
  static final int DEFAULT_MAX_MEMORY_LIMIT = 1024 * 1024;

  /** The access flags of the visited method. */
  protected int methodAccess;

  /** The descriptor of the visited method. */
  protected String methodDesc;

  /** Whether the visited method is a constructor. */
  private final boolean isConstructor;

  /**
   * The values in the current execution stack frame (long and double are represented by two
   * elements). Each value is either {@link #UNINITIALIZED_THIS} (for the uninitialized this value),
   * or {@link #OTHER} (for any other value). This field is only maintained for constructors, in
   * branches where the super class constructor has not been called yet. It is {@literal null} in
   * any other case.
   */
  private StackFrame stackFrame;

  /**
   * The stack map frames corresponding to the labels of the forward jumps made *before* the super
   * class constructor has been called. This field is only maintained for constructors.
   */
  private Map<Label, StackFrame> forwardJumpStackFrames;

  /**
   * Maximum number of bytes which can be used to allocate new frames in {@link
   * #forwardJumpStackFrames}.
   */
  private int remainingBytes = DEFAULT_MAX_MEMORY_LIMIT;

  /**
   * Constructs a new {@link AdviceAdapter}.
   *
   * @param methodVisitor the method visitor to which this adapter delegates calls.
   * @param access the method's access flags (see {@link Opcodes}).
   * @param name the method's name.
   * @param descriptor the method's descriptor (see {@link Type Type}).
   */
  protected AdviceAdapter(
          final MethodVisitor methodVisitor,
          final int access,
          final String name,
          final String descriptor) {
    super(methodVisitor, access, name, descriptor);
    methodAccess = access;
    methodDesc = descriptor;
    isConstructor = MethodSignature.CONSTRUCTOR_NAME.equals(name);
  }

  /**
   * Sets the maximum number of bytes which can be allocated by this adapter.
   *
   * <p>The default limit should be sufficient for any "normal" class. You only need to set a new
   * limit if this adapter throws a {@link LimitExceededException} on some of your classes.
   *
   * @param maxBytes the maximum number of bytes which can be allocated. Not all object
   * instantiations are tracked (and garbage collection is ignored), but the most important ones
   * are. The default value, 1MB, is more than one hundred times larger than the memory used for
   * any constructor in the java.* modules of the JDK (this adapter does not do any allocations
   * for methods).
   */
  public void setComputeLimits(final int maxBytes) {
    remainingBytes = maxBytes;
  }

  @Override
  public void visitCode() {
    super.visitCode();
    if (isConstructor) {
      stackFrame = new StackFrame();
      stackFrame.setLocal(0, UNINITIALIZED_THIS);
      forwardJumpStackFrames = new HashMap<>();
    }
    else {
      onMethodEnter();
    }
  }

  @Override
  public void visitLabel(final Label label) {
    super.visitLabel(label);
    if (isConstructor && forwardJumpStackFrames != null) {
      StackFrame labelStackFrame = forwardJumpStackFrames.get(label);
      if (labelStackFrame != null) {
        stackFrame = labelStackFrame;
        forwardJumpStackFrames.remove(label);
      }
    }
  }

  @Override
  public void visitFrame(
          final int type,
          final int numLocal,
          final Object[] local,
          final int numStack,
          final Object[] stack) {
    super.visitFrame(type, numLocal, local, numStack, stack);
    if (type != Opcodes.F_NEW) {
      throw new IllegalArgumentException(
              "AdviceAdapter only accepts expanded frames (see ClassReader.EXPAND_FRAMES)");
    }
    boolean hasUninitializedThis = false;
    for (int i = 0; i < numLocal; ++i) {
      if (local[i] == Opcodes.UNINITIALIZED_THIS) {
        hasUninitializedThis = true;
        break;
      }
    }
    if (!hasUninitializedThis) {
      for (int i = 0; i < numStack; ++i) {
        if (stack[i] == Opcodes.UNINITIALIZED_THIS) {
          hasUninitializedThis = true;
          break;
        }
      }
    }
    if (hasUninitializedThis) {
      stackFrame = new StackFrame();
      int currentLocal = 0;
      for (int i = 0; i < numLocal; ++i) {
        if (local[i] == Opcodes.UNINITIALIZED_THIS) {
          stackFrame.setLocal(currentLocal++, UNINITIALIZED_THIS);
        }
        else {
          stackFrame.setLocal(currentLocal++, OTHER);
          if (local[i] == Opcodes.LONG || local[i] == Opcodes.DOUBLE) {
            stackFrame.setLocal(currentLocal++, OTHER);
          }
        }
      }
      for (int i = 0; i < numStack; ++i) {
        if (stack[i] == Opcodes.UNINITIALIZED_THIS) {
          stackFrame.push(UNINITIALIZED_THIS);
        }
        else {
          stackFrame.push(OTHER);
          if (stack[i] == Opcodes.LONG || stack[i] == Opcodes.DOUBLE) {
            stackFrame.push(OTHER);
          }
        }
      }
    }
    else {
      stackFrame = null;
    }
  }

  @Override
  public void visitInsn(final int opcode) {
    if (isConstructor && stackFrame != null) {
      boolean value1;
      boolean value2;
      boolean value3;
      boolean value4;
      switch (opcode) {
        case IRETURN:
        case FRETURN:
        case ARETURN:
        case LRETURN:
        case DRETURN:
          throw new IllegalArgumentException("Invalid return in constructor");
        case RETURN: // stack map after instruction does not matter
        case ATHROW: // idem
          onMethodExit(opcode);
          endConstructorBasicBlockWithoutSuccessor();
          break;
        case NOP:
        case LALOAD: // remove 2 add 2
        case DALOAD: // remove 2 add 2
        case LNEG:
        case DNEG:
        case FNEG:
        case INEG:
        case L2D:
        case D2L:
        case F2I:
        case I2B:
        case I2C:
        case I2S:
        case I2F:
        case ARRAYLENGTH:
          break;
        case ACONST_NULL:
        case ICONST_M1:
        case ICONST_0:
        case ICONST_1:
        case ICONST_2:
        case ICONST_3:
        case ICONST_4:
        case ICONST_5:
        case FCONST_0:
        case FCONST_1:
        case FCONST_2:
        case F2L: // 1 before 2 after
        case F2D:
        case I2L:
        case I2D:
          stackFrame.push(OTHER);
          break;
        case LCONST_0:
        case LCONST_1:
        case DCONST_0:
        case DCONST_1:
          stackFrame.push(OTHER);
          stackFrame.push(OTHER);
          break;
        case IALOAD: // remove 2 add 1
        case FALOAD: // remove 2 add 1
        case AALOAD: // remove 2 add 1
        case BALOAD: // remove 2 add 1
        case CALOAD: // remove 2 add 1
        case SALOAD: // remove 2 add 1
        case POP:
        case IADD:
        case FADD:
        case ISUB:
        case LSHL: // 3 before 2 after
        case LSHR: // 3 before 2 after
        case LUSHR: // 3 before 2 after
        case L2I: // 2 before 1 after
        case L2F: // 2 before 1 after
        case D2I: // 2 before 1 after
        case D2F: // 2 before 1 after
        case FSUB:
        case FMUL:
        case FDIV:
        case FREM:
        case FCMPL: // 2 before 1 after
        case FCMPG: // 2 before 1 after
        case IMUL:
        case IDIV:
        case IREM:
        case ISHL:
        case ISHR:
        case IUSHR:
        case IAND:
        case IOR:
        case IXOR:
        case MONITORENTER:
        case MONITOREXIT:
          stackFrame.pop();
          break;
        case POP2:
        case LSUB:
        case LMUL:
        case LDIV:
        case LREM:
        case LADD:
        case LAND:
        case LOR:
        case LXOR:
        case DADD:
        case DMUL:
        case DSUB:
        case DDIV:
        case DREM:
          stackFrame.pop();
          stackFrame.pop();
          break;
        case IASTORE:
        case FASTORE:
        case AASTORE:
        case BASTORE:
        case CASTORE:
        case SASTORE:
        case LCMP: // 4 before 1 after
        case DCMPL:
        case DCMPG:
          stackFrame.pop();
          stackFrame.pop();
          stackFrame.pop();
          break;
        case LASTORE:
        case DASTORE:
          stackFrame.pop();
          stackFrame.pop();
          stackFrame.pop();
          stackFrame.pop();
          break;
        case Opcodes.DUP:
          value1 = stackFrame.pop();
          stackFrame.push(value1);
          stackFrame.push(value1);
          break;
        case Opcodes.DUP_X1:
          value1 = stackFrame.pop();
          value2 = stackFrame.pop();
          stackFrame.push(value1);
          stackFrame.push(value2);
          stackFrame.push(value1);
          break;
        case Opcodes.DUP_X2:
          value1 = stackFrame.pop();
          value2 = stackFrame.pop();
          value3 = stackFrame.pop();
          stackFrame.push(value1);
          stackFrame.push(value3);
          stackFrame.push(value2);
          stackFrame.push(value1);
          break;
        case Opcodes.DUP2:
          value1 = stackFrame.pop();
          value2 = stackFrame.pop();
          stackFrame.push(value2);
          stackFrame.push(value1);
          stackFrame.push(value2);
          stackFrame.push(value1);
          break;
        case Opcodes.DUP2_X1:
          value1 = stackFrame.pop();
          value2 = stackFrame.pop();
          value3 = stackFrame.pop();
          stackFrame.push(value2);
          stackFrame.push(value1);
          stackFrame.push(value3);
          stackFrame.push(value2);
          stackFrame.push(value1);
          break;
        case Opcodes.DUP2_X2:
          value1 = stackFrame.pop();
          value2 = stackFrame.pop();
          value3 = stackFrame.pop();
          value4 = stackFrame.pop();
          stackFrame.push(value2);
          stackFrame.push(value1);
          stackFrame.push(value4);
          stackFrame.push(value3);
          stackFrame.push(value2);
          stackFrame.push(value1);
          break;
        case Opcodes.SWAP:
          value1 = stackFrame.pop();
          value2 = stackFrame.pop();
          stackFrame.push(value1);
          stackFrame.push(value2);
          break;
        default:
          throw new IllegalArgumentException(INVALID_OPCODE + opcode);
      }
    }
    else {
      switch (opcode) {
        case RETURN:
        case IRETURN:
        case FRETURN:
        case ARETURN:
        case LRETURN:
        case DRETURN:
        case ATHROW:
          onMethodExit(opcode);
          break;
        default:
          break;
      }
    }
    super.visitInsn(opcode);
  }

  @Override
  public void visitVarInsn(final int opcode, final int varIndex) {
    super.visitVarInsn(opcode, varIndex);
    if (isConstructor && stackFrame != null) {
      switch (opcode) {
        case ILOAD:
        case FLOAD:
          stackFrame.push(OTHER);
          break;
        case LLOAD:
        case DLOAD:
          stackFrame.push(OTHER);
          stackFrame.push(OTHER);
          break;
        case ALOAD:
          stackFrame.push(stackFrame.getLocal(varIndex));
          break;
        case ASTORE:
        case ISTORE:
        case FSTORE:
          stackFrame.setLocal(varIndex, stackFrame.pop());
          break;
        case LSTORE:
        case DSTORE:
          stackFrame.pop();
          stackFrame.pop();
          stackFrame.setLocal(varIndex, OTHER);
          stackFrame.setLocal(varIndex + 1, OTHER);
          break;
        case RET:
          endConstructorBasicBlockWithoutSuccessor();
          break;
        default:
          throw new IllegalArgumentException(INVALID_OPCODE + opcode);
      }
    }
  }

  @Override
  public void visitFieldInsn(
          final int opcode, final String owner, final String name, final String descriptor) {
    super.visitFieldInsn(opcode, owner, name, descriptor);
    if (isConstructor && stackFrame != null) {
      char firstDescriptorChar = descriptor.charAt(0);
      boolean longOrDouble = firstDescriptorChar == 'J' || firstDescriptorChar == 'D';
      switch (opcode) {
        case GETSTATIC:
          stackFrame.push(OTHER);
          if (longOrDouble) {
            stackFrame.push(OTHER);
          }
          break;
        case PUTSTATIC:
          stackFrame.pop();
          if (longOrDouble) {
            stackFrame.pop();
          }
          break;
        case PUTFIELD:
          stackFrame.pop();
          stackFrame.pop();
          if (longOrDouble) {
            stackFrame.pop();
          }
          break;
        case GETFIELD:
          if (longOrDouble) {
            stackFrame.push(OTHER);
          }
          break;
        default:
          throw new IllegalArgumentException(INVALID_OPCODE + opcode);
      }
    }
  }

  @Override
  public void visitIntInsn(final int opcode, final int operand) {
    super.visitIntInsn(opcode, operand);
    if (isConstructor && stackFrame != null && opcode != NEWARRAY) {
      stackFrame.push(OTHER);
    }
  }

  @Override
  public void visitLdcInsn(final Object value) {
    super.visitLdcInsn(value);
    if (isConstructor && stackFrame != null) {
      stackFrame.push(OTHER);
      if (value instanceof Double
              || value instanceof Long
              || (value instanceof ConstantDynamic && ((ConstantDynamic) value).getSize() == 2)) {
        stackFrame.push(OTHER);
      }
    }
  }

  @Override
  public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
    super.visitMultiANewArrayInsn(descriptor, numDimensions);
    if (isConstructor && stackFrame != null) {
      for (int i = 0; i < numDimensions; i++) {
        stackFrame.pop();
      }
      stackFrame.push(OTHER);
    }
  }

  @Override
  public void visitTypeInsn(final int opcode, final String type) {
    super.visitTypeInsn(opcode, type);
    // ANEWARRAY, CHECKCAST or INSTANCEOF don't change stack.
    if (isConstructor && stackFrame != null && opcode == NEW) {
      stackFrame.push(OTHER);
    }
  }

  @Override
  public void visitMethodInsn(
          final int opcodeAndSource,
          final String owner,
          final String name,
          final String descriptor,
          final boolean isInterface) {
    super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface);
    int opcode = opcodeAndSource & ~Opcodes.SOURCE_MASK;

    doVisitMethodInsn(opcode, name, descriptor);
  }

  private void doVisitMethodInsn(final int opcode, final String name, final String descriptor) {
    if (isConstructor && stackFrame != null) {
      for (Type argumentType : Type.forArgumentTypes(descriptor)) {
        stackFrame.pop();
        if (argumentType.getSize() == 2) {
          stackFrame.pop();
        }
      }
      switch (opcode) {
        case INVOKEINTERFACE:
        case INVOKEVIRTUAL:
          stackFrame.pop();
          break;
        case INVOKESPECIAL:
          boolean value = stackFrame.pop();
          if (value == UNINITIALIZED_THIS && name.equals("<init>")) {
            stackFrame = null;
            onMethodEnter();
          }
          return;
        default:
          break;
      }

      Type returnType = Type.forReturnType(descriptor);
      if (returnType != Type.VOID_TYPE) {
        stackFrame.push(OTHER);
        if (returnType.getSize() == 2) {
          stackFrame.push(OTHER);
        }
      }
    }
  }

  @Override
  public void visitInvokeDynamicInsn(
          final String name,
          final String descriptor,
          final Handle bootstrapMethodHandle,
          final Object... bootstrapMethodArguments) {
    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    doVisitMethodInsn(Opcodes.INVOKEDYNAMIC, name, descriptor);
  }

  @Override
  public void visitJumpInsn(final int opcode, final Label label) {
    super.visitJumpInsn(opcode, label);
    if (isConstructor && stackFrame != null) {
      switch (opcode) {
        case IFEQ:
        case IFNE:
        case IFLT:
        case IFGE:
        case IFGT:
        case IFLE:
        case IFNULL:
        case IFNONNULL:
          stackFrame.pop();
          break;
        case IF_ICMPEQ:
        case IF_ICMPNE:
        case IF_ICMPLT:
        case IF_ICMPGE:
        case IF_ICMPGT:
        case IF_ICMPLE:
        case IF_ACMPEQ:
        case IF_ACMPNE:
          stackFrame.pop();
          stackFrame.pop();
          break;
        case JSR:
          stackFrame.push(OTHER);
          break;
        case GOTO:
          addForwardJump(label);
          endConstructorBasicBlockWithoutSuccessor();
          return;
        default:
          break;
      }
      // We assume that this is actually a forward jump. If it is not this does not have any effect
      // other than wasting some memory (the label has already been visited, hence the
      // forwardJumpStackFrames value for this label will never be read).
      addForwardJump(label);
    }
  }

  @Override
  public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
    super.visitLookupSwitchInsn(dflt, keys, labels);
    if (isConstructor && stackFrame != null) {
      stackFrame.pop();
      addForwardJumps(dflt, labels);
      endConstructorBasicBlockWithoutSuccessor();
    }
  }

  @Override
  public void visitTableSwitchInsn(
          final int min, final int max, final Label dflt, final Label... labels) {
    super.visitTableSwitchInsn(min, max, dflt, labels);
    if (isConstructor && stackFrame != null) {
      stackFrame.pop();
      addForwardJumps(dflt, labels);
      endConstructorBasicBlockWithoutSuccessor();
    }
  }

  @Override
  public void visitTryCatchBlock(
          final Label start, final Label end, final Label handler, final String type) {
    super.visitTryCatchBlock(start, end, handler, type);
    // By definition of 'forwardJumpStackFrames', 'handler' should be pushed only if there is an
    // instruction between 'start' and 'end' at which the super class constructor is not yet
    // called. Unfortunately, try catch blocks must be visited before their labels, so we have no
    // way to know this at this point. Instead, we suppose that the super class constructor has not
    // been called at the start of *any* exception handler. If this is wrong, normally there should
    // not be a second super class constructor call in the exception handler (an object can't be
    // initialized twice), so this is not issue (in the sense that there is no risk to emit a wrong
    // 'onMethodEnter').
    if (isConstructor && !forwardJumpStackFrames.containsKey(handler)) {
      StackFrame handlerStackFrame = new StackFrame();
      // If there are no stack map frames in the original code, we assume that UNINITIALIZED_THIS is
      // in local variable 0, and only in this local variable (see {@link AdviceAdapter}). If there
      // are stack map frames, handleStackFrame will be overridden in {@link #visitFrame} (frames
      // are visited after the corresponding label).
      handlerStackFrame.setLocal(0, UNINITIALIZED_THIS);
      handlerStackFrame.push(OTHER);
      forwardJumpStackFrames.put(handler, handlerStackFrame);
    }
  }

  private void addForwardJumps(final Label dflt, final Label[] labels) {
    addForwardJump(dflt);
    for (Label label : labels) {
      addForwardJump(label);
    }
  }

  private void addForwardJump(final Label label) {
    if (forwardJumpStackFrames.containsKey(label)) {
      return;
    }
    int allocatedBytes = stackFrame.sizeInBytes();
    if (allocatedBytes < 0 || allocatedBytes > remainingBytes) {
      throw new LimitExceededException("Too many allocated bytes");
    }
    remainingBytes -= allocatedBytes;
    forwardJumpStackFrames.put(label, new StackFrame(stackFrame));
  }

  private void endConstructorBasicBlockWithoutSuccessor() {
    // The next instruction is not reachable from this instruction. If it is dead code, we
    // should not try to simulate stack operations, and there is no need to insert advices
    // here. If it is reachable with a backward jump, the only possible case is that the super
    // class constructor has already been called (due to our hypotheses when there are no stack map
    // frames). If it is reachable with a forward jump, there are two sub-cases. Either the
    // super class constructor has already been called when reaching the next instruction, or
    // it has not been called. But in this case there must be a forwardJumpStackFrames entry
    // for a Label designating the next instruction. We can therefore always set this to null.
    stackFrame = null;
  }

  /**
   * Generates the "before" advice for the visited method. The default implementation of this method
   * does nothing. Subclasses can use or change all the local variables, but should not change state
   * of the stack. This method is called at the beginning of the method or after super class
   * constructor has been called (in constructors).
   */
  protected void onMethodEnter() { }

  /**
   * Generates the "after" advice for the visited method. The default implementation of this method
   * does nothing. Subclasses can use or change all the local variables, but should not change state
   * of the stack. This method is called at the end of the method, just before return and athrow
   * instructions. The top element on the stack contains the return value or the exception instance.
   * For example:
   *
   * <pre>
   * public void onMethodExit(final int opcode) {
   *   if (opcode == RETURN) {
   *     visitInsn(ACONST_NULL);
   *   } else if (opcode == ARETURN || opcode == ATHROW) {
   *     dup();
   *   } else {
   *     if (opcode == LRETURN || opcode == DRETURN) {
   *       dup2();
   *     } else {
   *       dup();
   *     }
   *     box(Type.getReturnType(this.methodDesc));
   *   }
   *   visitIntInsn(SIPUSH, opcode);
   *   visitMethodInsn(INVOKESTATIC, owner, "onExit", "(Ljava/lang/Object;I)V");
   * }
   *
   * // An actual call back method.
   * public static void onExit(final Object exitValue, final int opcode) {
   *   ...
   * }
   * </pre>
   *
   * @param opcode one of {@link Opcodes#RETURN}, {@link Opcodes#IRETURN}, {@link Opcodes#FRETURN},
   * {@link Opcodes#ARETURN}, {@link Opcodes#LRETURN}, {@link Opcodes#DRETURN} or {@link
   * Opcodes#ATHROW}.
   */
  protected void onMethodExit(final int opcode) { }

  /**
   * A stack map frame represented with one bit per element. Each bit indicates whether the element
   * is UNINITIALIZED_THIS (bit = 1), or OTHER (i.e., any other value; bit = 0).
   */
  static final class StackFrame {

    /**
     * The values of the local variables. Long and double are represented with two values (i.e., two
     * bits equal to OTHER). Local variables whose index is larger than the number of bits in this
     * array are equal to OTHER.
     */
    long[] locals;

    /**
     * The values on the stack. Long and double are represented with two values (i.e., two bits
     * equal to OTHER).
     */
    long[] stack;

    /** The number of values on the stack. */
    int stackSize;

    /** Constructs an empty stack map frame. */
    StackFrame() {
      this.locals = new long[1];
      this.stack = new long[1];
    }

    /**
     * Constructs a copy of the given stack map frame.
     *
     * @param stackFrame the stack map frame to copy.
     */
    StackFrame(final StackFrame stackFrame) {
      this.locals = arraycopy(stackFrame.locals, stackFrame.locals.length);
      this.stack = arraycopy(stackFrame.stack, stackFrame.stack.length);
      this.stackSize = stackFrame.stackSize;
    }

    /**
     * Returns the value of the given local variable.
     *
     * @param index a local variable index.
     * @return the local variable value
     */
    boolean getLocal(final int index) {
      int i = index / 64;
      int mask = 1 << (index % 64);
      return i < locals.length ? (locals[i] & mask) != 0 : false;
    }

    /**
     * Sets the value of the given local variable.
     *
     * @param index a local variable index.
     * @param value the new local variable value.
     */
    void setLocal(final int index, final boolean value) {
      int i = index / 64;
      int mask = 1 << (index % 64);
      if (value) {
        if (i >= locals.length) {
          locals = arraycopy(locals, locals.length * 2);
        }
        locals[i] |= mask;
      }
      else if (i < locals.length) {
        locals[i] &= ~mask;
      }
    }

    /**
     * Pushes a new value on the stack.
     *
     * @param value the value to push.
     */
    void push(final boolean value) {
      int i = stackSize / 64;
      int mask = 1 << (stackSize % 64);
      if (i >= stack.length) {
        stack = arraycopy(stack, stack.length * 2);
      }
      if (value) {
        stack[i] |= mask;
      }
      else {
        stack[i] &= ~mask;
      }
      ++stackSize;
    }

    /**
     * Pops a value from the stack and returns its value.
     *
     * @return the value popped from the stack.
     */
    boolean pop() {
      --stackSize;
      int i = stackSize / 64;
      int mask = 1 << (stackSize % 64);
      return (stack[i] & mask) != 0;
    }

    /**
     * Returns the number of bytes used by object in memory.
     *
     * @return the number of bytes used by object in memory. In theory, might be negative in case of
     * overflow. In practice this should never happen.
     */
    int sizeInBytes() {
      // Object header + (3 fields + padding) + (2 array headers) + size of the array elements.
      return (8 + (3 * 4 + 4) + (2 * 16)) + (locals.length + stack.length) * 8;
    }

    /**
     * Returns a copy of the given array, possibly with a larger size.
     *
     * @param array the array to copy.
     * @param newLength the length of the copied array (must be greater than or equal to
     * array.length).
     * @return the copied array.
     */
    static long[] arraycopy(final long[] array, final int newLength) {
      long[] newArray = new long[newLength];
      System.arraycopy(array, 0, newArray, 0, array.length);
      return newArray;
    }
  }
}
