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
package cn.taketoday.bytecode.commons;

import java.util.ArrayList;
import java.util.Arrays;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.ConstantDynamic;
import cn.taketoday.bytecode.Handle;
import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;

/**
 * A {@link MethodVisitor} with convenient methods to generate code. For example, using this
 * adapter, the class below
 *
 * <pre>
 * public class Example {
 *   public static void main(String[] args) {
 *     System.out.println(&quot;Hello world!&quot;);
 *   }
 * }
 * </pre>
 *
 * <p>can be generated as follows:
 *
 * <pre>
 * ClassWriter cw = new ClassWriter(0);
 * cw.visit(V1_1, ACC_PUBLIC, &quot;Example&quot;, null, &quot;java/lang/Object&quot;, null);
 *
 * MethodSignature m = MethodSignature.from(&quot;void &lt;init&gt; ()&quot;);
 * GeneratorAdapter mg = new GeneratorAdapter(ACC_PUBLIC, m, null, null, cw);
 * mg.loadThis();
 * mg.invokeConstructor(Type.getType(Object.class), m);
 * mg.returnValue();
 * mg.endMethod();
 *
 * m = MethodSignature.from(&quot;void main (String[])&quot;);
 * mg = new GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, m, null, null, cw);
 * mg.getStatic(Type.getType(System.class), &quot;out&quot;, Type.getType(PrintStream.class));
 * mg.push(&quot;Hello world!&quot;);
 * mg.invokeVirtual(Type.getType(PrintStream.class),
 *         MethodSignature.from(&quot;void println (String)&quot;));
 * mg.returnValue();
 * mg.endMethod();
 *
 * cw.visitEnd();
 * </pre>
 *
 * @author Juozas Baliuka
 * @author Chris Nokleberg
 * @author Eric Bruneton
 * @author Prashant Deva
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class GeneratorAdapter extends LocalVariablesSorter {

  private static final String CLASS_DESCRIPTOR = "Ljava/lang/Class;";

  /** Constant for the {@link #math} method. */
  public static final int ADD = Opcodes.IADD;

  /** Constant for the {@link #math} method. */
  public static final int SUB = Opcodes.ISUB;

  /** Constant for the {@link #math} method. */
  public static final int MUL = Opcodes.IMUL;

  /** Constant for the {@link #math} method. */
  public static final int DIV = Opcodes.IDIV;

  /** Constant for the {@link #math} method. */
  public static final int REM = Opcodes.IREM;

  /** Constant for the {@link #math} method. */
  public static final int NEG = Opcodes.INEG;

  /** Constant for the {@link #math} method. */
  public static final int SHL = Opcodes.ISHL;

  /** Constant for the {@link #math} method. */
  public static final int SHR = Opcodes.ISHR;

  /** Constant for the {@link #math} method. */
  public static final int USHR = Opcodes.IUSHR;

  /** Constant for the {@link #math} method. */
  public static final int AND = Opcodes.IAND;

  /** Constant for the {@link #math} method. */
  public static final int OR = Opcodes.IOR;

  /** Constant for the {@link #math} method. */
  public static final int XOR = Opcodes.IXOR;

  /** Constant for the {@link #ifCmp} method. */
  public static final int EQ = Opcodes.IFEQ;

  /** Constant for the {@link #ifCmp} method. */
  public static final int NE = Opcodes.IFNE;

  /** Constant for the {@link #ifCmp} method. */
  public static final int LT = Opcodes.IFLT;

  /** Constant for the {@link #ifCmp} method. */
  public static final int GE = Opcodes.IFGE;

  /** Constant for the {@link #ifCmp} method. */
  public static final int GT = Opcodes.IFGT;

  /** Constant for the {@link #ifCmp} method. */
  public static final int LE = Opcodes.IFLE;

  /** The access flags of the visited method. */
  private final int access;

  /** The name of the visited method. */
  private final String name;

  /** The return type of the visited method. */
  private final Type returnType;

  /** The types of the local variables of the visited method. */
  private final ArrayList<Type> localTypes;

  /**
   * Constructs a new {@link GeneratorAdapter}.
   *
   * @param methodVisitor the method visitor to which this adapter delegates calls.
   * @param access the method's access flags (see {@link Opcodes}).
   * @param name the method's name.
   * @param descriptor the method's descriptor (see {@link Type}).
   */
  public GeneratorAdapter(
          final MethodVisitor methodVisitor,
          final int access,
          final String name,
          final String descriptor) {
    super(access, descriptor, methodVisitor);
    this.name = name;
    this.access = access;
    this.localTypes = new ArrayList<>();
    this.returnType = Type.forReturnType(descriptor);
  }

  public GeneratorAdapter(GeneratorAdapter other) {
    super(other);
    this.name = other.name;
    this.access = other.access;
    this.returnType = other.returnType;
    this.localTypes = other.localTypes;
  }

  /**
   * Constructs a new {@link GeneratorAdapter}.
   *
   * @param access access flags of the adapted method.
   * @param method the adapted method.
   * @param methodVisitor the method visitor to which this adapter delegates calls.
   */
  public GeneratorAdapter(
          final int access, final MethodSignature method, final MethodVisitor methodVisitor) {
    this(methodVisitor, access, method.getName(), method.getDescriptor());
  }

  /**
   * Constructs a new {@link GeneratorAdapter}.
   *
   * @param access access flags of the adapted method.
   * @param method the adapted method.
   * @param signature the signature of the adapted method (may be {@literal null}).
   * @param exceptions the exceptions thrown by the adapted method (may be {@literal null}).
   * @param classVisitor the class visitor to which this adapter delegates calls.
   */
  public GeneratorAdapter(
          final int access,
          final MethodSignature method,
          final String signature,
          final Type[] exceptions,
          final ClassVisitor classVisitor) {
    this(access,
            method,
            classVisitor.visitMethod(
                    access,
                    method.getName(),
                    method.getDescriptor(),
                    signature,
                    exceptions == null ? null : getInternalNames(exceptions)));
  }

  /**
   * Returns the internal names of the given types.
   *
   * @param types a set of types.
   * @return the internal names of the given types.
   */
  private static String[] getInternalNames(final Type[] types) {
    String[] names = new String[types.length];
    for (int i = 0; i < names.length; ++i) {
      names[i] = types[i].getInternalName();
    }
    return names;
  }

  public int getAccess() {
    return access;
  }

  public String getName() {
    return name;
  }

  public Type getReturnType() {
    return returnType;
  }

  // -----------------------------------------------------------------------------------------------
  // Instructions to push constants on the stack
  // -----------------------------------------------------------------------------------------------

  /**
   * Generates the instruction to push the given value on the stack.
   *
   * @param value the value to be pushed on the stack.
   */
  public void push(final boolean value) {
    push(value ? 1 : 0);
  }

  /**
   * Generates the instruction to push the given value on the stack.
   *
   * @param value the value to be pushed on the stack.
   */
  public void push(final int value) {
    InstructionAdapter.push(mv, value);
  }

  /**
   * Generates the instruction to push the given value on the stack.
   *
   * @param value the value to be pushed on the stack.
   */
  public void push(final long value) {
    if (value == 0L || value == 1L) {
      mv.visitInsn(Opcodes.LCONST_0 + (int) value);
    }
    else {
      mv.visitLdcInsn(value);
    }
  }

  /**
   * Generates the instruction to push the given value on the stack.
   *
   * @param value the value to be pushed on the stack.
   */
  public void push(final float value) {
    int bits = Float.floatToIntBits(value);
    if (bits == 0L || bits == 0x3F800000 || bits == 0x40000000) { // 0..2
      mv.visitInsn(Opcodes.FCONST_0 + (int) value);
    }
    else {
      mv.visitLdcInsn(value);
    }
  }

  /**
   * Generates the instruction to push the given value on the stack.
   *
   * @param value the value to be pushed on the stack.
   */
  public void push(final double value) {
    long bits = Double.doubleToLongBits(value);
    if (bits == 0L || bits == 0x3FF0000000000000L) { // +0.0d and 1.0d
      mv.visitInsn(Opcodes.DCONST_0 + (int) value);
    }
    else {
      mv.visitLdcInsn(value);
    }
  }

  /**
   * Generates the instruction to push the given value on the stack.
   *
   * @param value the value to be pushed on the stack. May be {@literal null}.
   */
  public void push(final String value) {
    if (value == null) {
      aconst_null();
    }
    else {
      mv.visitLdcInsn(value);
    }
  }

  // since 4.0
  public void aconst_null() {
    mv.visitInsn(Opcodes.ACONST_NULL);
  }

  /**
   * Generates the instruction to push the given value on the stack.
   *
   * @param value the value to be pushed on the stack.
   */
  public void push(final Type value) {
    if (value == null) {
      aconst_null();
    }
    else {
      switch (value.getSort()) {
        case Type.BYTE -> mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Byte", "TYPE", CLASS_DESCRIPTOR);
        case Type.LONG -> mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Long", "TYPE", CLASS_DESCRIPTOR);
        case Type.SHORT -> mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Short", "TYPE", CLASS_DESCRIPTOR);
        case Type.INT -> mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Integer", "TYPE", CLASS_DESCRIPTOR);
        case Type.FLOAT -> mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Float", "TYPE", CLASS_DESCRIPTOR);
        case Type.DOUBLE -> mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Double", "TYPE", CLASS_DESCRIPTOR);
        case Type.CHAR -> mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Character", "TYPE", CLASS_DESCRIPTOR);
        case Type.BOOLEAN -> mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Boolean", "TYPE", CLASS_DESCRIPTOR);
        default -> mv.visitLdcInsn(value);
      }
    }
  }

  /**
   * Generates the instruction to push a handle on the stack.
   *
   * @param handle the handle to be pushed on the stack.
   */
  public void push(final Handle handle) {
    if (handle == null) {
      aconst_null();
    }
    else {
      mv.visitLdcInsn(handle);
    }
  }

  /**
   * Generates the instruction to push a constant dynamic on the stack.
   *
   * @param constantDynamic the constant dynamic to be pushed on the stack.
   */
  public void push(final ConstantDynamic constantDynamic) {
    if (constantDynamic == null) {
      aconst_null();
    }
    else {
      mv.visitLdcInsn(constantDynamic);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Instructions to load and store method arguments
  // -----------------------------------------------------------------------------------------------

  /**
   * Returns the index of the given method argument in the frame's local variables array.
   *
   * @param arg the index of a method argument.
   * @return the index of the given method argument in the frame's local variables array.
   */
  public int getArgIndex(final int arg) {
    final Type[] argumentTypes = getArgumentTypes();
    int index = (access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
    for (int i = 0; i < arg; i++) {
      index += argumentTypes[i].getSize();
    }
    return index;
  }

  /**
   * Generates the instruction to push a local variable on the stack.
   *
   * @param type the type of the local variable to be loaded.
   * @param index an index in the frame's local variables array.
   */
  public void loadInsn(final Type type, final int index) {
    mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
  }

  /**
   * Generates the instruction to store the top stack value in a local variable.
   *
   * @param type the type of the local variable to be stored.
   * @param index an index in the frame's local variables array.
   */
  public void storeInsn(final Type type, final int index) {
    mv.visitVarInsn(type.getOpcode(Opcodes.ISTORE), index);
  }

  /** Generates the instruction to load 'this' on the stack. */
  public void loadThis() {
    if ((access & Opcodes.ACC_STATIC) != 0) {
      throw new IllegalStateException("no 'this' pointer within static method");
    }
    mv.visitVarInsn(Opcodes.ALOAD, 0);
  }

  /**
   * Generates the instruction to load the given method argument on the stack.
   * <p>Pushes the specified argument of the current method onto the stack.
   *
   * @param arg the index of a method argument. the zero-based index into the argument list
   */
  public void loadArg(final int arg) {
    loadInsn(argumentTypes[arg], getArgIndex(arg));
  }

  /**
   * Generates the instructions to load the given method arguments on the stack.
   *
   * @param arg the index of the first method argument to be loaded.
   * @param count the number of method arguments to be loaded.
   */
  public void loadArgs(final int arg, final int count) {
    int index = getArgIndex(arg);
    final Type[] argumentTypes = this.argumentTypes;
    for (int i = 0; i < count; ++i) {
      Type argumentType = argumentTypes[arg + i];
      loadInsn(argumentType, index);
      index += argumentType.getSize();
    }
  }

  /**
   * Generates the instructions to load all the method arguments on the stack.
   * <p>Pushes all of the arguments of the current method onto the stack.
   */
  public void loadArgs() {
    loadArgs(0, argumentTypes.length);
  }

  /**
   * Generates the instructions to load all the method arguments on the stack, as a single object
   * array.
   * <p>
   * Allocates and fills an Object[] array with the arguments to the current
   * method. Primitive values are inserted as their boxed (Object) equivalents.
   */
  public void loadArgArray() {
    /* generates: Object[] args = new Object[]{ arg1, new Integer(arg2) }; */
    final Type[] argumentTypes = this.argumentTypes;
    push(argumentTypes.length);
    newArray(Type.TYPE_OBJECT);
    for (int i = 0; i < argumentTypes.length; i++) {
      dup();
      push(i);
      loadArg(i);
      box(argumentTypes[i]);
      arrayStore(Type.TYPE_OBJECT);
    }
  }

  /**
   * Generates the instruction to store the top stack value in the given method argument.
   *
   * @param arg the index of a method argument.
   */
  public void storeArg(final int arg) {
    storeInsn(argumentTypes[arg], getArgIndex(arg));
  }

  // -----------------------------------------------------------------------------------------------
  // Instructions to load and store local variables
  // -----------------------------------------------------------------------------------------------

  /**
   * Returns the type of the given local variable.
   *
   * @param local a local variable identifier, as returned by {@link
   * LocalVariablesSorter#newLocalIndex(Type)}.
   * @return the type of the given local variable.
   */
  public Type getLocalType(final int local) {
    return localTypes.get(local - firstLocal);
  }

  @Override
  protected void setLocalType(final int local, final Type type) {
    int index = local - firstLocal;
    while (localTypes.size() < index + 1) {
      localTypes.add(null);
    }
    localTypes.set(index, type);
  }

  public void loadLocal(final Local local) {
    loadInsn(local.type, local.index);
  }

  /**
   * Generates the instruction to load the given local variable on the stack.
   *
   * @param local a local variable identifier, as returned by {@link
   * LocalVariablesSorter#newLocalIndex(Type)}.
   */
  public void loadLocal(final int local) {
    loadInsn(getLocalType(local), local);
  }

  /**
   * Generates the instruction to load the given local variable on the stack.
   *
   * @param local a local variable identifier, as returned by {@link
   * LocalVariablesSorter#newLocalIndex(Type)}.
   * @param type the type of this local variable.
   */
  public void loadLocal(final int local, final Type type) {
    setLocalType(local, type);
    loadInsn(type, local);
  }

  public void storeLocal(final Local local) {
    storeInsn(local.type, local.index);
  }

  /**
   * Generates the instruction to store the top stack value in the given local variable.
   *
   * @param local a local variable identifier, as returned by {@link
   * LocalVariablesSorter#newLocalIndex(Type)}.
   */
  public void storeLocal(final int local) {
    storeInsn(getLocalType(local), local);
  }

  /**
   * Generates the instruction to store the top stack value in the given local variable.
   *
   * @param local a local variable identifier, as returned by {@link
   * LocalVariablesSorter#newLocalIndex(Type)}.
   * @param type the type of this local variable.
   */
  public void storeLocal(final int local, final Type type) {
    setLocalType(local, type);
    storeInsn(type, local);
  }

  /**
   * Generates the instruction to load an element from an array.
   *
   * @param type the type of the array element to be loaded.
   */
  public void arrayLoad(final Type type) {
    mv.visitInsn(type.getOpcode(Opcodes.IALOAD));
  }

  /**
   * Generates the instruction to store an element in an array.
   *
   * @param type the type of the array element to be stored.
   */
  public void arrayStore(final Type type) {
    mv.visitInsn(type.getOpcode(Opcodes.IASTORE));
  }

  public void aastore() {
    mv.visitInsn(Opcodes.AASTORE);
  }

  public void aaload(int index) {
    push(index);
    aaload();
  }

  public void aaload() {
    mv.visitInsn(Opcodes.AALOAD);
  }

  // -----------------------------------------------------------------------------------------------
  // Instructions to manage the stack
  // -----------------------------------------------------------------------------------------------

  /** Generates a POP instruction. */
  public void pop() {
    mv.visitInsn(Opcodes.POP);
  }

  /** Generates a POP2 instruction. */
  public void pop2() {
    mv.visitInsn(Opcodes.POP2);
  }

  /** Generates a DUP instruction. */
  public void dup() {
    mv.visitInsn(Opcodes.DUP);
  }

  /** Generates a DUP2 instruction. */
  public void dup2() {
    mv.visitInsn(Opcodes.DUP2);
  }

  /** Generates a DUP_X1 instruction. */
  public void dupX1() {
    mv.visitInsn(Opcodes.DUP_X1);
  }

  /** Generates a DUP_X2 instruction. */
  public void dupX2() {
    mv.visitInsn(Opcodes.DUP_X2);
  }

  /** Generates a DUP2_X1 instruction. */
  public void dup2X1() {
    mv.visitInsn(Opcodes.DUP2_X1);
  }

  /** Generates a DUP2_X2 instruction. */
  public void dup2X2() {
    mv.visitInsn(Opcodes.DUP2_X2);
  }

  /** Generates a SWAP instruction. */
  public void swap() {
    mv.visitInsn(Opcodes.SWAP);
  }

  /**
   * Generates the instructions to swap the top two stack values.
   *
   * @param prev type of the top - 1 stack value.
   * @param type type of the top stack value.
   */
  public void swap(final Type prev, final Type type) {
    if (type.getSize() == 1) {
      if (prev.getSize() == 1) {
        swap(); // Same as dupX1 pop.
      }
      else {
        dupX2();
        pop();
      }
    }
    else {
      if (prev.getSize() == 1) {
        dup2X1();
        pop2();
      }
      else {
        dup2X2();
        pop2();
      }
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Instructions to do mathematical and logical operations
  // -----------------------------------------------------------------------------------------------

  /**
   * Generates the instruction to do the specified mathematical or logical operation.
   *
   * @param op a mathematical or logical operation. Must be one of ADD, SUB, MUL, DIV, REM, NEG,
   * SHL, SHR, USHR, AND, OR, XOR.
   * @param type the type of the operand(s) for this operation.
   */
  public void math(final int op, final Type type) {
    mv.visitInsn(type.getOpcode(op));
  }

  /** Generates the instructions to compute the bitwise negation of the top stack value. */
  public void not() {
    mv.visitInsn(Opcodes.ICONST_1);
    mv.visitInsn(Opcodes.IXOR);
  }

  public void iinc(final Local local, final int amount) {
    mv.visitIincInsn(local.index, amount);
  }

  /**
   * Generates the instruction to increment the given local variable.
   *
   * @param local the local variable to be incremented.
   * @param amount the amount by which the local variable must be incremented.
   */
  public void iinc(final int local, final int amount) {
    mv.visitIincInsn(local, amount);
  }

  /**
   * Generates the instructions to cast a numerical value from one type to another.
   *
   * @param from the type of the top stack value
   * @param to the type into which this value must be cast.
   */
  public void cast(final Type from, final Type to) {
    if (from != to) {
      if (from.getSort() < Type.BOOLEAN
              || from.getSort() > Type.DOUBLE
              || to.getSort() < Type.BOOLEAN
              || to.getSort() > Type.DOUBLE) {
        throw new IllegalArgumentException("Cannot cast from " + from + " to " + to);
      }
      InstructionAdapter.cast(mv, from, to);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Instructions to do boxing and unboxing operations
  // -----------------------------------------------------------------------------------------------

  /**
   * Generates the instructions to box the top stack value. This value is replaced by its boxed
   * equivalent on top of the stack.
   * <p>
   * If the argument is a primitive class, replaces the primitive value on the top
   * of the stack with the wrapped (Object) equivalent. For example, char ->
   * Character. If the class is Void, a null is pushed onto the stack instead.
   *
   * @param type the type of the top stack value.
   */
  public void box(final Type type) {
    valueOf(type);
  }

  /**
   * Generates the instructions to box the top stack value using Java 5's valueOf() method. This
   * value is replaced by its boxed equivalent on top of the stack.
   *
   * @param type the type of the top stack value.
   */
  public void valueOf(final Type type) {
    if (type.isPrimitive()) {
      if (type == Type.VOID_TYPE) {
        aconst_null();
      }
      else {
        Type boxedType = type.getBoxedType();
        invokeStatic(boxedType, new MethodSignature(boxedType, "valueOf", type));
      }
    }
  }

  /**
   * Generates the instructions to unbox the top stack value. This value is replaced by its unboxed
   * equivalent on top of the stack.
   * <p>
   * If the argument is a primitive class, replaces the object on the top of the
   * stack with the unwrapped (primitive) equivalent. For example, Character ->
   * char.
   * </p>
   *
   * @param type the class indicating the desired type of the top stack value
   */
  public void unbox(final Type type) {
    Type boxedType = Type.TYPE_NUMBER;
    MethodSignature unboxMethod;
    switch (type.getSort()) {
      case Type.VOID:
        return;
      case Type.CHAR:
        boxedType = Type.TYPE_CHARACTER;
        unboxMethod = MethodSignature.CHAR_VALUE;
        break;
      case Type.BOOLEAN:
        boxedType = Type.TYPE_BOOLEAN;
        unboxMethod = MethodSignature.BOOLEAN_VALUE;
        break;
      case Type.DOUBLE:
        unboxMethod = MethodSignature.DOUBLE_VALUE;
        break;
      case Type.FLOAT:
        unboxMethod = MethodSignature.FLOAT_VALUE;
        break;
      case Type.LONG:
        unboxMethod = MethodSignature.LONG_VALUE;
        break;
      case Type.INT:
      case Type.SHORT:
      case Type.BYTE:
        unboxMethod = MethodSignature.INT_VALUE;
        break;
      default:
        unboxMethod = null;
        break;
    }
    if (unboxMethod == null) {
      checkCast(type);
    }
    else {
      checkCast(boxedType);
      invokeVirtual(boxedType, unboxMethod);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Instructions to jump to other instructions
  // -----------------------------------------------------------------------------------------------

  /**
   * Constructs a new {@link Label}.
   *
   * @return a new {@link Label}.
   */
  public Label newLabel() {
    return new Label();
  }

  /**
   * Marks the current code position with the given label.
   *
   * @param label a label.
   */
  public void mark(final Label label) {
    mv.visitLabel(label);
  }

  /**
   * Marks the current code position with a new label.
   *
   * @return the label that was created to mark the current code position.
   */
  public Label mark() {
    Label label = new Label();
    mv.visitLabel(label);
    return label;
  }

  /**
   * Generates the instructions to jump to a label based on the comparison of the top two stack
   * values.
   *
   * @param type the type of the top two stack values.
   * @param mode how these values must be compared. One of EQ, NE, LT, GE, GT, LE.
   * @param label where to jump if the comparison result is {@literal true}.
   */
  public void ifCmp(final Type type, final int mode, final Label label) {
    switch (type.getSort()) {
      case Type.LONG:
        mv.visitInsn(Opcodes.LCMP);
        break;
      case Type.DOUBLE:
        mv.visitInsn(mode == GE || mode == GT ? Opcodes.DCMPL : Opcodes.DCMPG);
        break;
      case Type.FLOAT:
        mv.visitInsn(mode == GE || mode == GT ? Opcodes.FCMPL : Opcodes.FCMPG);
        break;
      case Type.ARRAY:
      case Type.OBJECT:
        if (mode == EQ) {
          mv.visitJumpInsn(Opcodes.IF_ACMPEQ, label);
          return;
        }
        else if (mode == NE) {
          mv.visitJumpInsn(Opcodes.IF_ACMPNE, label);
          return;
        }
        else {
          throw new IllegalArgumentException("Bad comparison for type " + type);
        }
      default:
        int intOp = switch (mode) {
          case EQ -> Opcodes.IF_ICMPEQ;
          case NE -> Opcodes.IF_ICMPNE;
          case GE -> Opcodes.IF_ICMPGE;
          case LT -> Opcodes.IF_ICMPLT;
          case LE -> Opcodes.IF_ICMPLE;
          case GT -> Opcodes.IF_ICMPGT;
          default -> throw new IllegalArgumentException("Bad comparison mode " + mode);
        };
        mv.visitJumpInsn(intOp, label);
        return;
    }
    mv.visitJumpInsn(mode, label);
  }

  /**
   * Generates the instructions to jump to a label based on the comparison of the top two integer
   * stack values.
   *
   * @param mode how these values must be compared. One of EQ, NE, LT, GE, GT, LE.
   * @param label where to jump if the comparison result is {@literal true}.
   */
  public void ifICmp(final int mode, final Label label) {
    ifCmp(Type.INT_TYPE, mode, label);
  }

  /**
   * Generates the instructions to jump to a label based on the comparison of the top integer stack
   * value with zero.
   *
   * @param mode how these values must be compared. One of EQ, NE, LT, GE, GT, LE.
   * @param label where to jump if the comparison result is {@literal true}.
   */
  public void ifZCmp(final int mode, final Label label) {
    mv.visitJumpInsn(mode, label);
  }

  public void ifJump(int mode, Label label) {
    mv.visitJumpInsn(mode, label);
  }

  public void ifIcmp(int mode, Label label) {
    ifCmp(Type.INT_TYPE, mode, label);
  }

  /**
   * Generates the instruction to jump to the given label if the top stack value is null.
   *
   * @param label where to jump if the condition is {@literal true}.
   */
  public void ifNull(final Label label) {
    mv.visitJumpInsn(Opcodes.IFNULL, label);
  }

  /**
   * Generates the instruction to jump to the given label if the top stack value is not null.
   *
   * @param label where to jump if the condition is {@literal true}.
   */
  public void ifNonNull(final Label label) {
    mv.visitJumpInsn(Opcodes.IFNONNULL, label);
  }

  /**
   * Generates the instruction to jump to the given label.
   *
   * @param label where to jump if the condition is {@literal true}.
   */
  public void goTo(final Label label) {
    mv.visitJumpInsn(Opcodes.GOTO, label);
  }

  /**
   * Generates a RET instruction.
   *
   * @param local a local variable identifier, as returned by {@link
   * LocalVariablesSorter#newLocalIndex(Type)}.
   */
  public void ret(final int local) {
    mv.visitVarInsn(Opcodes.RET, local);
  }

  /**
   * Generates the instructions for a switch statement.
   *
   * @param keys the switch case keys.
   * @param generator a generator to generate the code for the switch cases.
   */
  public void tableSwitch(final int[] keys, final TableSwitchGenerator generator) {
    float density;
    if (keys.length == 0) {
      density = 0;
    }
    else {
      density = (float) keys.length / (keys[keys.length - 1] - keys[0] + 1);
    }
    tableSwitch(keys, generator, density >= 0.5f);
  }

  /**
   * Generates the instructions for a switch statement.
   *
   * @param keys the switch case keys.
   * @param generator a generator to generate the code for the switch cases.
   * @param useTable {@literal true} to use a TABLESWITCH instruction, or {@literal false} to use a
   * LOOKUPSWITCH instruction.
   */
  public void tableSwitch(
          final int[] keys, final TableSwitchGenerator generator, final boolean useTable) {
    for (int i = 1; i < keys.length; ++i) {
      if (keys[i] < keys[i - 1]) {
        throw new IllegalArgumentException("keys must be sorted in ascending order");
      }
    }
    Label defaultLabel = newLabel();
    Label endLabel = newLabel();
    if (keys.length > 0) {
      int numKeys = keys.length;
      if (useTable) {
        int min = keys[0];
        int max = keys[numKeys - 1];
        int range = max - min + 1;
        Label[] labels = new Label[range];
        Arrays.fill(labels, defaultLabel);
        for (final int key : keys) {
          labels[key - min] = newLabel();
        }
        mv.visitTableSwitchInsn(min, max, defaultLabel, labels);
        for (int i = 0; i < range; ++i) {
          Label label = labels[i];
          if (label != defaultLabel) {
            mark(label);
            generator.generateCase(i + min, endLabel);
          }
        }
      }
      else {
        Label[] labels = new Label[numKeys];
        for (int i = 0; i < numKeys; ++i) {
          labels[i] = newLabel();
        }
        mv.visitLookupSwitchInsn(defaultLabel, keys, labels);
        for (int i = 0; i < numKeys; ++i) {
          mark(labels[i]);
          generator.generateCase(keys[i], endLabel);
        }
      }
    }
    mark(defaultLabel);
    generator.generateDefault();
    mark(endLabel);
  }

  /** Generates the instruction to return the top stack value to the caller. */
  public void returnValue() {
    mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
  }

  // -----------------------------------------------------------------------------------------------
  // Instructions to load and store fields
  // -----------------------------------------------------------------------------------------------

  /**
   * Generates a get field or set field instruction.
   *
   * @param opcode the instruction's opcode.
   * @param ownerType the class in which the field is defined.
   * @param name the name of the field.
   * @param fieldType the type of the field.
   */
  public void fieldInsn(
          final int opcode, final Type ownerType, final String name, final Type fieldType) {
    mv.visitFieldInsn(opcode, ownerType.getInternalName(), name, fieldType.getDescriptor());
  }

  /**
   * Generates the instruction to push the value of a static field on the stack.
   *
   * @param owner the class in which the field is defined.
   * @param name the name of the field.
   * @param type the type of the field.
   */
  public void getStatic(final Type owner, final String name, final Type type) {
    fieldInsn(Opcodes.GETSTATIC, owner, name, type);
  }

  /**
   * Generates the instruction to store the top stack value in a static field.
   *
   * @param owner the class in which the field is defined.
   * @param name the name of the field.
   * @param type the type of the field.
   */
  public void putStatic(final Type owner, final String name, final Type type) {
    fieldInsn(Opcodes.PUTSTATIC, owner, name, type);
  }

  /**
   * Generates the instruction to push the value of a non static field on the stack.
   *
   * @param owner the class in which the field is defined.
   * @param name the name of the field.
   * @param type the type of the field.
   */
  public void getField(final Type owner, final String name, final Type type) {
    fieldInsn(Opcodes.GETFIELD, owner, name, type);
  }

  /**
   * Generates the instruction to store the top stack value in a non static field.
   *
   * @param owner the class in which the field is defined.
   * @param name the name of the field.
   * @param type the type of the field.
   */
  public void putField(final Type owner, final String name, final Type type) {
    fieldInsn(Opcodes.PUTFIELD, owner, name, type);
  }

  // -----------------------------------------------------------------------------------------------
  // Instructions to invoke methods
  // -----------------------------------------------------------------------------------------------

  /**
   * Generates an invoke method instruction.
   *
   * @param opcode the instruction's opcode.
   * @param type the class in which the method is defined.
   * @param method the method to be invoked.
   * @param isInterface whether the 'type' class is an interface or not.
   */
  public void invokeInsn(
          final int opcode, final Type type, final MethodSignature method, final boolean isInterface) {
    String owner = type.getSort() == Type.ARRAY ? type.getDescriptor() : type.getInternalName();
    mv.visitMethodInsn(opcode, owner, method.getName(), method.getDescriptor(), isInterface);
  }

  /**
   * Generates the instruction to invoke a normal method.
   *
   * @param owner the class in which the method is defined.
   * @param method the method to be invoked.
   */
  public void invokeVirtual(final Type owner, final MethodSignature method) {
    invokeInsn(Opcodes.INVOKEVIRTUAL, owner, method, false);
  }

  /**
   * Generates the instruction to invoke a default constructor.
   *
   * @param type the class in which the constructor is defined.
   */
  public void invokeConstructor(final Type type) {
    invokeInsn(Opcodes.INVOKESPECIAL, type, MethodSignature.EMPTY_CONSTRUCTOR, false);
  }

  /**
   * Generates the instruction to invoke a constructor.
   *
   * @param type the class in which the constructor is defined.
   * @param method the constructor to be invoked.
   */
  public void invokeConstructor(final Type type, final MethodSignature method) {
    invokeInsn(Opcodes.INVOKESPECIAL, type, method, false);
  }

  /**
   * Generates the instruction to invoke a static method.
   *
   * @param owner the class in which the method is defined.
   * @param method the method to be invoked.
   */
  public void invokeStatic(final Type owner, final MethodSignature method) {
    invokeInsn(Opcodes.INVOKESTATIC, owner, method, false);
  }

  /**
   * Generates the instruction to invoke an interface method.
   *
   * @param owner the class in which the method is defined.
   * @param method the method to be invoked.
   */
  public void invokeInterface(final Type owner, final MethodSignature method) {
    invokeInsn(Opcodes.INVOKEINTERFACE, owner, method, true);
  }

  /**
   * Generates an invokedynamic instruction.
   *
   * @param name the method's name.
   * @param descriptor the method's descriptor (see {@link Type}).
   * @param bootstrapMethodHandle the bootstrap method.
   * @param bootstrapMethodArguments the bootstrap method constant arguments. Each argument must be
   * an {@link Integer}, {@link Float}, {@link Long}, {@link Double}, {@link String}, {@link
   * Type} or {@link Handle} value. This method is allowed to modify the content of the array so
   * a caller should expect that this array may change.
   */
  public void invokeDynamic(
          final String name,
          final String descriptor,
          final Handle bootstrapMethodHandle,
          final Object... bootstrapMethodArguments) {
    mv.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
  }

  // -----------------------------------------------------------------------------------------------
  // Instructions to create objects and arrays
  // -----------------------------------------------------------------------------------------------

  /**
   * Generates a type dependent instruction.
   *
   * @param opcode the instruction's opcode.
   * @param type the instruction's operand.
   */
  protected void typeInsn(final int opcode, final Type type) {
    mv.visitTypeInsn(opcode, type.getInternalName());
  }

  /**
   * Generates the instruction to create a new object.
   *
   * @param type the class of the object to be created.
   */
  public void newInstance(final Type type) {
    typeInsn(Opcodes.NEW, type);
  }

  public void newArray() {
    newArray(Type.TYPE_OBJECT);
  }

  /**
   * Generates the instruction to create a new array.
   *
   * @param type the type of the array elements.
   */
  public void newArray(final Type type) {
    InstructionAdapter.newArray(mv, type);
  }

  /** Generates the instruction to compute the length of an array. */
  public void arrayLength() {
    mv.visitInsn(Opcodes.ARRAYLENGTH);
  }

  // -----------------------------------------------------------------------------------------------
  // Miscellaneous instructions
  // -----------------------------------------------------------------------------------------------

  /** Generates the instruction to throw an exception. */
  public void throwException() {
    mv.visitInsn(Opcodes.ATHROW);
  }

  /**
   * Generates the instructions to create and throw an exception. The exception class must have a
   * constructor with a single String argument.
   *
   * @param type the class of the exception to be thrown.
   * @param message the detailed message of the exception.
   */
  public void throwException(final Type type, final String message) {
    newInstance(type);
    dup();
    push(message);
    invokeConstructor(type, MethodSignature.constructWithString);
    throwException();
  }

  /**
   * Generates the instruction to check that the top stack value is of the given type.
   *
   * @param type a class or interface type.
   */
  public void checkCast(final Type type) {
    if (!type.equals(Type.TYPE_OBJECT)) {
      typeInsn(Opcodes.CHECKCAST, type);
    }
  }

  /**
   * Generates the instruction to test if the top stack value is of the given type.
   *
   * @param type a class or interface type.
   */
  public void instanceOf(final Type type) {
    typeInsn(Opcodes.INSTANCEOF, type);
  }

  /** Generates the instruction to get the monitor of the top stack value. */
  public void monitorEnter() {
    mv.visitInsn(Opcodes.MONITORENTER);
  }

  /** Generates the instruction to release the monitor of the top stack value. */
  public void monitorExit() {
    mv.visitInsn(Opcodes.MONITOREXIT);
  }

  // -----------------------------------------------------------------------------------------------
  // Non instructions
  // -----------------------------------------------------------------------------------------------

  /** Marks the end of the visited method. */
  public void endMethod() {
    if ((access & Opcodes.ACC_ABSTRACT) == 0) {
      mv.visitMaxs(0, 0);
    }
    mv.visitEnd();
  }

  /**
   * Marks the start of an exception handler.
   *
   * @param start beginning of the exception handler's scope (inclusive).
   * @param end end of the exception handler's scope (exclusive).
   * @param exception internal name of the type of exceptions handled by the handler.
   */
  public void catchException(final Label start, final Label end, final Type exception) {
    Label catchLabel = new Label();
    if (exception == null) {
      mv.visitTryCatchBlock(start, end, catchLabel, null);
    }
    else {
      mv.visitTryCatchBlock(start, end, catchLabel, exception.getInternalName());
    }
    mark(catchLabel);
  }
}
