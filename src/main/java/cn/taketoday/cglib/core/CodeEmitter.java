/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.cglib.core;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import cn.taketoday.asm.Attribute;
import cn.taketoday.asm.Label;
import cn.taketoday.asm.MethodVisitor;
import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.Type;
import cn.taketoday.asm.commons.GeneratorAdapter;
import cn.taketoday.asm.commons.MethodSignature;
import cn.taketoday.asm.commons.TableSwitchGenerator;
import cn.taketoday.core.Assert;

/**
 * @author Juozas Baliuka, Chris Nokleberg
 */
@SuppressWarnings("all")
public class CodeEmitter extends GeneratorAdapter {

  private static final MethodSignature CSTRUCT_STRING = MethodSignature.forConstructor("String");
  private static final MethodSignature INT_VALUE = MethodSignature.from("int intValue()");
  private static final MethodSignature CHAR_VALUE = MethodSignature.from("char charValue()");
  private static final MethodSignature LONG_VALUE = MethodSignature.from("long longValue()");
  private static final MethodSignature FLOAT_VALUE = MethodSignature.from("float floatValue()");
  private static final MethodSignature DOUBLE_VALUE = MethodSignature.from("double doubleValue()");
  private static final MethodSignature BOOLEAN_VALUE = MethodSignature.from("boolean booleanValue()");

  public static final int ADD = Opcodes.IADD;
  public static final int MUL = Opcodes.IMUL;
  public static final int XOR = Opcodes.IXOR;
  public static final int USHR = Opcodes.IUSHR;
  public static final int SUB = Opcodes.ISUB;
  public static final int DIV = Opcodes.IDIV;
  public static final int NEG = Opcodes.INEG;
  public static final int REM = Opcodes.IREM;
  public static final int AND = Opcodes.IAND;
  public static final int OR = Opcodes.IOR;

  public static final int GT = Opcodes.IFGT;
  public static final int LT = Opcodes.IFLT;
  public static final int GE = Opcodes.IFGE;
  public static final int LE = Opcodes.IFLE;
  public static final int NE = Opcodes.IFNE;
  public static final int EQ = Opcodes.IFEQ;

  private ClassEmitter ce;
  private State state;

  private static class State extends MethodInfo {

    private ClassInfo classInfo;
    private int access;
    private MethodSignature sig;
    private Type[] argumentTypes;
    private int localOffset;
    private Type[] exceptionTypes;

    State(ClassInfo classInfo, int access, MethodSignature sig, Type[] exceptionTypes) {
      this.classInfo = classInfo;
      this.access = access;
      this.sig = sig;
      this.exceptionTypes = exceptionTypes;
      localOffset = Modifier.isStatic(access) ? 0 : 1;
      argumentTypes = sig.getArgumentTypes();
    }

    public ClassInfo getClassInfo() {
      return classInfo;
    }

    public int getModifiers() {
      return access;
    }

    public MethodSignature getSignature() {
      return sig;
    }

    public Type[] getExceptionTypes() {
      return exceptionTypes;
    }

    public Attribute getAttribute() {
      // TODO
      return null;
    }
  }

  CodeEmitter(ClassEmitter ce, MethodVisitor mv, int access, MethodSignature sig, Type[] exceptionTypes) {
    super(access, sig, mv);
    this.ce = ce;
    state = new State(ce.getClassInfo(), access, sig, exceptionTypes);
  }

  public CodeEmitter(CodeEmitter wrap) {
    super(wrap);
    this.ce = wrap.ce;
    this.state = wrap.state;
  }

  public boolean isStaticHook() {
    return false;
  }

  public MethodSignature getSignature() {
    return state.sig;
  }

  public Type getReturnType() {
    return state.sig.getReturnType();
  }

  public MethodInfo getMethodInfo() {
    return state;
  }

  public ClassEmitter getClassEmitter() {
    return ce;
  }

  public void end_method() {
    visitMaxs(0, 0);
  }

  public Block begin_block() {
    return new Block(this);
  }

  public void catch_exception(Block block, Type exception) {
    if (block.getEnd() == null) {
      throw new IllegalStateException("end of block is unset");
    }
    mv.visitTryCatchBlock(block.getStart(), block.getEnd(), mark(), exception.getInternalName());
  }

  public void goTo(Label label) {
    mv.visitJumpInsn(Opcodes.GOTO, label);
  }

  public void ifnull(Label label) {
    mv.visitJumpInsn(Opcodes.IFNULL, label);
  }

  public void ifnonnull(Label label) {
    mv.visitJumpInsn(Opcodes.IFNONNULL, label);
  }

  public void if_jump(int mode, Label label) {
    mv.visitJumpInsn(mode, label);
  }

  public void if_icmp(int mode, Label label) {
    if_cmp(Type.INT_TYPE, mode, label);
  }

  public void if_cmp(Type type, int mode, Label label) {
    int intOp = -1;
    int jumpmode = mode;
    switch (mode) {
      case GE:
        jumpmode = LT;
        break;
      case LE:
        jumpmode = GT;
        break;
    }
    switch (type.getSort()) {
      case Type.LONG:
        mv.visitInsn(Opcodes.LCMP);
        break;
      case Type.DOUBLE:
        mv.visitInsn(Opcodes.DCMPG);
        break;
      case Type.FLOAT:
        mv.visitInsn(Opcodes.FCMPG);
        break;
      case Type.ARRAY:
      case Type.OBJECT:
        switch (mode) {
          case EQ:
            mv.visitJumpInsn(Opcodes.IF_ACMPEQ, label);
            return;
          case NE:
            mv.visitJumpInsn(Opcodes.IF_ACMPNE, label);
            return;
        }
        throw new IllegalArgumentException("Bad comparison for type " + type);
      default:
        switch (mode) {
          case EQ:
            intOp = Opcodes.IF_ICMPEQ;
            break;
          case NE:
            intOp = Opcodes.IF_ICMPNE;
            break;
          case GE:
            swap(); /* fall through */
          case LT:
            intOp = Opcodes.IF_ICMPLT;
            break;
          case LE:
            swap(); /* fall through */
          case GT:
            intOp = Opcodes.IF_ICMPGT;
            break;
        }
        mv.visitJumpInsn(intOp, label);
        return;
    }
    if_jump(jumpmode, label);
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

  public void dup_x1() {
    mv.visitInsn(Opcodes.DUP_X1);
  }

  public void dup_x2() {
    mv.visitInsn(Opcodes.DUP_X2);
  }

  public void dup2_x1() {
    mv.visitInsn(Opcodes.DUP2_X1);
  }

  public void dup2_x2() {
    mv.visitInsn(Opcodes.DUP2_X2);
  }

  public void swap() {
    mv.visitInsn(Opcodes.SWAP);
  }

  public void aconst_null() {
    mv.visitInsn(Opcodes.ACONST_NULL);
  }

  public void swap(Type prev, Type type) {
    if (type.getSize() == 1) {
      if (prev.getSize() == 1) {
        swap(); // same as dup_x1(), pop();
      }
      else {
        dup_x2();
        pop();
      }
    }
    else {
      if (prev.getSize() == 1) {
        dup2_x1();
        pop2();
      }
      else {
        dup2_x2();
        pop2();
      }
    }
  }

  public void monitorenter() {
    mv.visitInsn(Opcodes.MONITORENTER);
  }

  public void monitorexit() {
    mv.visitInsn(Opcodes.MONITOREXIT);
  }

  public void math(int op, Type type) {
    mv.visitInsn(type.getOpcode(op));
  }

  public void array_load(Type type) {
    mv.visitInsn(type.getOpcode(Opcodes.IALOAD));
  }

  public void array_store(Type type) {
    mv.visitInsn(type.getOpcode(Opcodes.IASTORE));
  }

  /**
   * Casts from one primitive numeric type to another
   */
  public void cast_numeric(Type from, Type to) {
    if (from != to) {
      if (from == Type.DOUBLE_TYPE) {
        if (to == Type.FLOAT_TYPE) {
          mv.visitInsn(Opcodes.D2F);
        }
        else if (to == Type.LONG_TYPE) {
          mv.visitInsn(Opcodes.D2L);
        }
        else {
          mv.visitInsn(Opcodes.D2I);
          cast_numeric(Type.INT_TYPE, to);
        }
      }
      else if (from == Type.FLOAT_TYPE) {
        if (to == Type.DOUBLE_TYPE) {
          mv.visitInsn(Opcodes.F2D);
        }
        else if (to == Type.LONG_TYPE) {
          mv.visitInsn(Opcodes.F2L);
        }
        else {
          mv.visitInsn(Opcodes.F2I);
          cast_numeric(Type.INT_TYPE, to);
        }
      }
      else if (from == Type.LONG_TYPE) {
        if (to == Type.DOUBLE_TYPE) {
          mv.visitInsn(Opcodes.L2D);
        }
        else if (to == Type.FLOAT_TYPE) {
          mv.visitInsn(Opcodes.L2F);
        }
        else {
          mv.visitInsn(Opcodes.L2I);
          cast_numeric(Type.INT_TYPE, to);
        }
      }
      else {
        if (to == Type.BYTE_TYPE) {
          mv.visitInsn(Opcodes.I2B);
        }
        else if (to == Type.CHAR_TYPE) {
          mv.visitInsn(Opcodes.I2C);
        }
        else if (to == Type.DOUBLE_TYPE) {
          mv.visitInsn(Opcodes.I2D);
        }
        else if (to == Type.FLOAT_TYPE) {
          mv.visitInsn(Opcodes.I2F);
        }
        else if (to == Type.LONG_TYPE) {
          mv.visitInsn(Opcodes.I2L);
        }
        else if (to == Type.SHORT_TYPE) {
          mv.visitInsn(Opcodes.I2S);
        }
      }
    }
  }

  public void push(int i) {
    if (i < -1) {
      mv.visitLdcInsn(Integer.valueOf(i));
    }
    else if (i <= 5) {
      mv.visitInsn(iconst(i));
    }
    else if (i <= Byte.MAX_VALUE) {
      mv.visitIntInsn(Opcodes.BIPUSH, i);
    }
    else if (i <= Short.MAX_VALUE) {
      mv.visitIntInsn(Opcodes.SIPUSH, i);
    }
    else {
      mv.visitLdcInsn(Integer.valueOf(i));
    }
  }

  public void push(long value) {
    if (value == 0L || value == 1L) {
      mv.visitInsn(lconst(value));
    }
    else {
      mv.visitLdcInsn(Long.valueOf(value));
    }
  }

  public void push(float value) {
    if (value == 0f || value == 1f || value == 2f) {
      mv.visitInsn(fconst(value));
    }
    else {
      mv.visitLdcInsn(value);
    }
  }

  public void push(double value) {
    if (value == 0d || value == 1d) {
      mv.visitInsn(dconst(value));
    }
    else {
      mv.visitLdcInsn(value);
    }
  }

  public void push(String value) {
    mv.visitLdcInsn(value);
  }

  public void newArray() {
    newArray(Type.TYPE_OBJECT);
  }

  public void newArray(Type type) {
    if (type.isPrimitive()) {
      mv.visitIntInsn(Opcodes.NEWARRAY, newArrayFromType(type));
    }
    else {
      emit_type(Opcodes.ANEWARRAY, type);
    }
  }

  public void arraylength() {
    mv.visitInsn(Opcodes.ARRAYLENGTH);
  }

  public void load_this() {
    if (Modifier.isStatic(state.access)) {
      throw new IllegalStateException("no 'this' pointer within static method");
    }
    mv.visitVarInsn(Opcodes.ALOAD, 0);
  }

  /**
   * Pushes all of the arguments of the current method onto the stack.
   */
  public void load_args() {
    load_args(0, state.argumentTypes.length);
  }

  /**
   * Pushes the specified argument of the current method onto the stack.
   *
   * @param index
   *         the zero-based index into the argument list
   */
  public void load_arg(int index) {
    load_local(state.argumentTypes[index], state.localOffset + skipArgs(index));
  }

  // zero-based (see load_this)
  public void load_args(int fromArg, int count) {
    int pos = state.localOffset + skipArgs(fromArg);
    for (int i = 0; i < count; i++) {
      Type t = state.argumentTypes[fromArg + i];
      load_local(t, pos);
      pos += t.getSize();
    }
  }

  private int skipArgs(int numArgs) {
    int amount = 0;
    for (int i = 0; i < numArgs; i++) {
      amount += state.argumentTypes[i].getSize();
    }
    return amount;
  }

  private void load_local(Type t, int pos) {
    // TODO: make t == null ok?
    mv.visitVarInsn(t.getOpcode(Opcodes.ILOAD), pos);
  }

  private void store_local(Type t, int pos) {
    // TODO: make t == null ok?
    mv.visitVarInsn(t.getOpcode(Opcodes.ISTORE), pos);
  }

  public void iinc(Local local, int amount) {
    mv.visitIincInsn(local.getIndex(), amount);
  }

  public void store_local(Local local) {
    store_local(local.getType(), local.getIndex());
  }

  public void load_local(Local local) {
    load_local(local.getType(), local.getIndex());
  }

  public void return_value() {
    mv.visitInsn(state.sig.getReturnType().getOpcode(Opcodes.IRETURN));
  }

  public void getfield(String name) {
    ClassEmitter.FieldInfo info = ce.getFieldInfo(name);
    int opcode = Modifier.isStatic(info.access) ? Opcodes.GETSTATIC : Opcodes.GETFIELD;
    emit_field(opcode, ce.getClassType(), name, info.type);
  }

  public void putfield(String name) {
    ClassEmitter.FieldInfo info = ce.getFieldInfo(name);
    int opcode = Modifier.isStatic(info.access) ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD;
    emit_field(opcode, ce.getClassType(), name, info.type);
  }

  public void super_getfield(String name, Type type) {
    emit_field(Opcodes.GETFIELD, ce.getSuperType(), name, type);
  }

  public void super_putfield(String name, Type type) {
    emit_field(Opcodes.PUTFIELD, ce.getSuperType(), name, type);
  }

  public void super_getstatic(String name, Type type) {
    emit_field(Opcodes.GETSTATIC, ce.getSuperType(), name, type);
  }

  public void super_putstatic(String name, Type type) {
    emit_field(Opcodes.PUTSTATIC, ce.getSuperType(), name, type);
  }

  public void getfield(Type owner, String name, Type type) {
    emit_field(Opcodes.GETFIELD, owner, name, type);
  }

  public void putfield(Type owner, String name, Type type) {
    emit_field(Opcodes.PUTFIELD, owner, name, type);
  }

  public void getstatic(Type owner, String name, Type type) {
    emit_field(Opcodes.GETSTATIC, owner, name, type);
  }

  public void putstatic(Type owner, String name, Type type) {
    emit_field(Opcodes.PUTSTATIC, owner, name, type);
  }

  // package-protected for EmitUtils, try to fix
  void emit_field(int opcode, Type ctype, String name, Type ftype) {
    mv.visitFieldInsn(opcode, ctype.getInternalName(), name, ftype.getDescriptor());
  }

  public void super_invoke() {
    super_invoke(state.sig);
  }

  public void super_invoke(MethodSignature sig) {
    emit_invoke(Opcodes.INVOKESPECIAL, ce.getSuperType(), sig, false);
  }

  public void invoke_constructor(Type type) {
    invoke_constructor(type, MethodSignature.EMPTY_CONSTRUCTOR);
  }

  public void super_invoke_constructor() {
    invoke_constructor(ce.getSuperType());
  }

  public void invoke_constructor_this() {
    invoke_constructor(ce.getClassType());
  }

  private void emit_invoke(int opcode, Type type, MethodSignature sig, boolean isInterface) {

//      if (sig.getName().equals(Opcodes.CONSTRUCTOR_NAME)
//            && ((opcode == Opcodes.INVOKEVIRTUAL) || (opcode == Opcodes.INVOKESTATIC))) {
//          TODO: error
//      }
    mv.visitMethodInsn(opcode,
                       type.getInternalName(),
                       sig.getName(),
                       sig.getDescriptor(),
                       isInterface//
    );
  }

  public void invoke_interface(Type owner, MethodSignature sig) {
    emit_invoke(Opcodes.INVOKEINTERFACE, owner, sig, true);
  }

  public void invoke_virtual(Type owner, MethodSignature sig) {
    emit_invoke(Opcodes.INVOKEVIRTUAL, owner, sig, false);
  }

  public void invoke_static(Type owner, MethodSignature sig) {
    emit_invoke(Opcodes.INVOKESTATIC, owner, sig, false);
  }

  public void invoke_static(Type owner, MethodSignature sig, boolean isInterface) {
    emit_invoke(Opcodes.INVOKESTATIC, owner, sig, isInterface);
  }

  public void invoke_virtual_this(MethodSignature sig) {
    invoke_virtual(ce.getClassType(), sig);
  }

  public void invoke_static_this(MethodSignature sig) {
    invoke_static(ce.getClassType(), sig);
  }

  public void invoke_constructor(Type type, MethodSignature sig) {
    emit_invoke(Opcodes.INVOKESPECIAL, type, sig, false);
  }

  public void invoke_constructor_this(MethodSignature sig) {
    invoke_constructor(ce.getClassType(), sig);
  }

  public void super_invoke_constructor(MethodSignature sig) {
    invoke_constructor(ce.getSuperType(), sig);
  }

  public void new_instance_this() {
    new_instance(ce.getClassType());
  }

  public void new_instance(Type type) {
    emit_type(Opcodes.NEW, type);
  }

  private void emit_type(int opcode, Type type) {
    mv.visitTypeInsn(opcode, type.isArray() ? type.getDescriptor() : type.getInternalName());
  }

  public void aaload(int index) {
    push(index);
    aaload();
  }

  public void aaload() {
    mv.visitInsn(Opcodes.AALOAD);
  }

  public void aastore() {
    mv.visitInsn(Opcodes.AASTORE);
  }

  public void athrow() {
    mv.visitInsn(Opcodes.ATHROW);
  }

  public Label make_label() {
    return new Label();
  }

  public Local make_local() {
    return make_local(Type.TYPE_OBJECT);
  }

  public Local make_local(Type type) {
    return new Local(newLocal(type), type);
  }

  public void checkcast_this() {
    checkcast(ce.getClassType());
  }

  public void checkcast(Type type) {
    if (!type.equals(Type.TYPE_OBJECT)) {
      emit_type(Opcodes.CHECKCAST, type);
    }
  }

  public void instance_of(Type type) {
    emit_type(Opcodes.INSTANCEOF, type);
  }

  public void instance_of_this() {
    instance_of(ce.getClassType());
  }

  public void process_switch(int[] keys, TableSwitchGenerator callback) {
    float density;
    if (keys.length == 0) {
      density = 0;
    }
    else {
      density = (float) keys.length / (keys[keys.length - 1] - keys[0] + 1);
    }
    process_switch(keys, callback, density >= 0.5f);
  }

  public void process_switch(int[] keys, TableSwitchGenerator callback, boolean useTable) {
    Assert.isTrue(isSorted(keys), "keys to switch must be sorted ascending");
    Label def = make_label();
    Label end = make_label();

    try {
      if (keys.length > 0) {
        int len = keys.length;
        int min = keys[0];
        int max = keys[len - 1];
        int range = max - min + 1;

        if (useTable) {
          Label[] labels = new Label[range];
          Arrays.fill(labels, def);
          for (int i = 0; i < len; i++) {
            labels[keys[i] - min] = make_label();
          }
          mv.visitTableSwitchInsn(min, max, def, labels);
          for (int i = 0; i < range; i++) {
            Label label = labels[i];
            if (label != def) {
              mark(label);
              callback.generateCase(i + min, end);
            }
          }
        }
        else {
          Label[] labels = new Label[len];
          for (int i = 0; i < len; i++) {
            labels[i] = make_label();
          }
          mv.visitLookupSwitchInsn(def, keys, labels);
          for (int i = 0; i < len; i++) {
            mark(labels[i]);
            callback.generateCase(keys[i], end);
          }
        }
      }

      mark(def);
      callback.generateDefault();
      mark(end);

    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Error e) {
      throw e;
    }
    catch (Exception e) {
      throw new CodeGenerationException(e);
    }
  }

  private static boolean isSorted(int[] keys) {
    for (int i = 1; i < keys.length; i++) {
      if (keys[i] < keys[i - 1])
        return false;
    }
    return true;
  }

  public void mark(Label label) {
    mv.visitLabel(label);
  }

  public void push(boolean value) {
    push(value ? 1 : 0);
  }

  /**
   * Toggles the integer on the top of the stack from 1 to 0 or vice versa
   */
  public void not() {
    push(1);
    math(XOR, Type.INT_TYPE);
  }

  public void throw_exception(Type type, String msg) {
    new_instance(type);
    dup();
    push(msg);
    invoke_constructor(type, CSTRUCT_STRING);
    athrow();
  }

  /**
   * If the argument is a primitive class, replaces the primitive value on the top
   * of the stack with the wrapped (Object) equivalent. For example, char ->
   * Character. If the class is Void, a null is pushed onto the stack instead.
   *
   * @param type
   *         the class indicating the current type of the top stack value
   */
  public void box(Type type) {
    if (type.isPrimitive()) {
      if (type == Type.VOID_TYPE) {
        aconst_null();
      }
      else {
        Type boxed = type.getBoxedType();
        visitMethodInsn(Opcodes.INVOKESTATIC,
                        boxed.getInternalName(),
                        "valueOf",
                        Type.getMethodDescriptor(boxed, Type.array(type)),
                        false);

        //newInstanceBox(type);
      }
    }
  }

  /**
   *
   */
  @Deprecated
  private void newInstanceBox(Type type) {
    Type boxed = type.getBoxedType();

    new_instance(boxed);
    if (type.getSize() == 2) {
      // Pp -> Ppo -> oPpo -> ooPpo -> ooPp -> o
      dup_x2();
      dup_x2();
      pop();
    }
    else {
      // p -> po -> opo -> oop -> o
      dup_x1();
      swap();
    }
    invoke_constructor(boxed, new MethodSignature(MethodSignature.CONSTRUCTOR_NAME, Type.VOID_TYPE, Type.array(type)));
  }

  /**
   * If the argument is a primitive class, replaces the object on the top of the
   * stack with the unwrapped (primitive) equivalent. For example, Character ->
   * char.
   *
   * @param type
   *         the class indicating the desired type of the top stack value
   *
   * @return true if the value was unboxed
   */
  public void unbox(Type type) {
    Type t = Type.TYPE_NUMBER;
    MethodSignature sig = null;
    switch (type.getSort()) {
      case Type.VOID:
        return;
      case Type.CHAR:
        t = Type.TYPE_CHARACTER;
        sig = CHAR_VALUE;
        break;
      case Type.BOOLEAN:
        t = Type.TYPE_BOOLEAN;
        sig = BOOLEAN_VALUE;
        break;
      case Type.DOUBLE:
        sig = DOUBLE_VALUE;
        break;
      case Type.FLOAT:
        sig = FLOAT_VALUE;
        break;
      case Type.LONG:
        sig = LONG_VALUE;
        break;
      case Type.INT:
      case Type.SHORT:
      case Type.BYTE:
        sig = INT_VALUE;
    }

    if (sig == null) {
      checkcast(type);
    }
    else {
      checkcast(t);
      invoke_virtual(t, sig);
    }
  }

  /**
   * Allocates and fills an Object[] array with the arguments to the current
   * method. Primitive values are inserted as their boxed (Object) equivalents.
   */
  public void create_arg_array() {
    /* generates: Object[] args = new Object[]{ arg1, new Integer(arg2) }; */

    final Type[] argumentTypes = state.argumentTypes;
    push(argumentTypes.length);
    newArray();
    for (int i = 0; i < argumentTypes.length; i++) {
      dup();
      push(i);
      load_arg(i);
      box(argumentTypes[i]);
      aastore();
    }
  }

  /**
   * Pushes a zero onto the stack if the argument is a primitive class, or a null
   * otherwise.
   */
  public void zero_or_null(Type type) {
    if (type.isPrimitive()) {
      switch (type.getSort()) {
        case Type.DOUBLE:
          push(0d);
          break;
        case Type.LONG:
          push(0L);
          break;
        case Type.FLOAT:
          push(0f);
          break;
        case Type.VOID:
          aconst_null();
        default:
          push(0);
      }
    }
    else {
      aconst_null();
    }
  }

  /**
   * Unboxes the object on the top of the stack. If the object is null, the
   * unboxed primitive value becomes zero.
   */
  public void unbox_or_zero(Type type) {
    if (type.isPrimitive()) {
      if (type != Type.VOID_TYPE) {
        Label nonNull = make_label();
        Label end = make_label();
        dup();
        ifnonnull(nonNull);
        pop();
        zero_or_null(type);
        goTo(end);
        mark(nonNull);
        unbox(type);
        mark(end);
      }
    }
    else {
      checkcast(type);
    }
  }

  public void visitMaxs(int maxStack, int maxLocals) {
    if (!Modifier.isAbstract(state.access)) {
      mv.visitMaxs(0, 0);
    }
  }

  public void invoke(MethodInfo method, Type virtualType) {
    ClassInfo classInfo = method.getClassInfo();
    Type type = classInfo.getType();
    MethodSignature sig = method.getSignature();
    if (sig.getName().equals(MethodSignature.CONSTRUCTOR_NAME)) {
      invoke_constructor(type, sig);
    }
    else if (Modifier.isStatic(method.getModifiers())) {
      invoke_static(type, sig, Modifier.isInterface(classInfo.getModifiers()));
    }
    else if (Modifier.isInterface(classInfo.getModifiers())) {
      invoke_interface(type, sig);
    }
    else {
      invoke_virtual(virtualType, sig);
    }
  }

  public void invoke(MethodInfo method) {
    invoke(method, method.getClassInfo().getType());
  }

  // static
  public static int iconst(int value) {
    switch (value) //@off
    {
      case -1 :   return Opcodes.ICONST_M1;
      case 0 :    return Opcodes.ICONST_0;
      case 1 :    return Opcodes.ICONST_1;
      case 2 :    return Opcodes.ICONST_2;
      case 3 :    return Opcodes.ICONST_3;
      case 4 :    return Opcodes.ICONST_4;
      case 5 :    return Opcodes.ICONST_5;
      default:
        return -1; // error @on
    }
  }

  public static int lconst(long value) {
    if (value == 0L) {
      return Opcodes.LCONST_0;
    }
    if (value == 1L) {
      return Opcodes.LCONST_1;
    }
    return -1; // error
  }

  public static int fconst(float value) {
    if (value == 0f) {
      return Opcodes.FCONST_0;
    }
    if (value == 1f) {
      return Opcodes.FCONST_1;
    }
    if (value == 2f) {
      return Opcodes.FCONST_2;
    }
    return -1; // error
  }

  public static int dconst(double value) {
    if (value == 0d) {
      return Opcodes.DCONST_0;
    }
    if (value == 1d) {
      return Opcodes.DCONST_1;
    }
    return -1; // error
  }

  public static int newArrayFromType(Type type) {
    switch (type.getSort()) { //@off
      case Type.BYTE :    return Opcodes.T_BYTE;
      case Type.CHAR :    return Opcodes.T_CHAR;
      case Type.DOUBLE :  return Opcodes.T_DOUBLE;
      case Type.FLOAT :   return Opcodes.T_FLOAT;
      case Type.INT :     return Opcodes.T_INT;
      case Type.LONG :    return Opcodes.T_LONG;
      case Type.SHORT :   return Opcodes.T_SHORT;
      case Type.BOOLEAN : return Opcodes.T_BOOLEAN;
      default:
        return -1; // error @on
    }
  }

}
