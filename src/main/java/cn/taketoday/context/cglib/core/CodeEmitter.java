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
package cn.taketoday.context.cglib.core;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import cn.taketoday.context.Constant;
import cn.taketoday.context.asm.Attribute;
import cn.taketoday.context.asm.Label;
import cn.taketoday.context.asm.MethodVisitor;
import cn.taketoday.context.asm.Type;

/**
 * @author Juozas Baliuka, Chris Nokleberg
 */
@SuppressWarnings("all")
public class CodeEmitter extends LocalVariablesSorter {

    private static final Signature BOOLEAN_VALUE = TypeUtils.parseSignature("boolean booleanValue()");
    private static final Signature CHAR_VALUE = TypeUtils.parseSignature("char charValue()");
    private static final Signature LONG_VALUE = TypeUtils.parseSignature("long longValue()");
    private static final Signature DOUBLE_VALUE = TypeUtils.parseSignature("double doubleValue()");
    private static final Signature FLOAT_VALUE = TypeUtils.parseSignature("float floatValue()");
    private static final Signature INT_VALUE = TypeUtils.parseSignature("int intValue()");
    private static final Signature CSTRUCT_NULL = TypeUtils.parseConstructor(Constant.BLANK);
    private static final Signature CSTRUCT_STRING = TypeUtils.parseConstructor("String");

    public static final int ADD = Constant.IADD;
    public static final int MUL = Constant.IMUL;
    public static final int XOR = Constant.IXOR;
    public static final int USHR = Constant.IUSHR;
    public static final int SUB = Constant.ISUB;
    public static final int DIV = Constant.IDIV;
    public static final int NEG = Constant.INEG;
    public static final int REM = Constant.IREM;
    public static final int AND = Constant.IAND;
    public static final int OR = Constant.IOR;

    public static final int GT = Constant.IFGT;
    public static final int LT = Constant.IFLT;
    public static final int GE = Constant.IFGE;
    public static final int LE = Constant.IFLE;
    public static final int NE = Constant.IFNE;
    public static final int EQ = Constant.IFEQ;

    private ClassEmitter ce;
    private State state;

    private static class State extends MethodInfo {

        private ClassInfo classInfo;
        private int access;
        private Signature sig;
        private Type[] argumentTypes;
        private int localOffset;
        private Type[] exceptionTypes;

        State(ClassInfo classInfo, int access, Signature sig, Type[] exceptionTypes) {
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

        public Signature getSignature() {
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

    CodeEmitter(ClassEmitter ce, MethodVisitor mv, int access, Signature sig, Type[] exceptionTypes) {
        super(access, sig.getDescriptor(), mv);
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

    public Signature getSignature() {
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
        mv.visitJumpInsn(Constant.GOTO, label);
    }

    public void ifnull(Label label) {
        mv.visitJumpInsn(Constant.IFNULL, label);
    }

    public void ifnonnull(Label label) {
        mv.visitJumpInsn(Constant.IFNONNULL, label);
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
            case GE :
                jumpmode = LT;
                break;
            case LE :
                jumpmode = GT;
                break;
        }
        switch (type.getSort()) {
            case Type.LONG :
                mv.visitInsn(Constant.LCMP);
                break;
            case Type.DOUBLE :
                mv.visitInsn(Constant.DCMPG);
                break;
            case Type.FLOAT :
                mv.visitInsn(Constant.FCMPG);
                break;
            case Type.ARRAY :
            case Type.OBJECT :
                switch (mode) {
                    case EQ :
                        mv.visitJumpInsn(Constant.IF_ACMPEQ, label);
                        return;
                    case NE :
                        mv.visitJumpInsn(Constant.IF_ACMPNE, label);
                        return;
                }
                throw new IllegalArgumentException("Bad comparison for type " + type);
            default:
                switch (mode) {
                    case EQ :
                        intOp = Constant.IF_ICMPEQ;
                        break;
                    case NE :
                        intOp = Constant.IF_ICMPNE;
                        break;
                    case GE :
                        swap(); /* fall through */
                    case LT :
                        intOp = Constant.IF_ICMPLT;
                        break;
                    case LE :
                        swap(); /* fall through */
                    case GT :
                        intOp = Constant.IF_ICMPGT;
                        break;
                }
                mv.visitJumpInsn(intOp, label);
                return;
        }
        if_jump(jumpmode, label);
    }

    public void pop() {
        mv.visitInsn(Constant.POP);
    }

    public void pop2() {
        mv.visitInsn(Constant.POP2);
    }

    public void dup() {
        mv.visitInsn(Constant.DUP);
    }

    public void dup2() {
        mv.visitInsn(Constant.DUP2);
    }

    public void dup_x1() {
        mv.visitInsn(Constant.DUP_X1);
    }

    public void dup_x2() {
        mv.visitInsn(Constant.DUP_X2);
    }

    public void dup2_x1() {
        mv.visitInsn(Constant.DUP2_X1);
    }

    public void dup2_x2() {
        mv.visitInsn(Constant.DUP2_X2);
    }

    public void swap() {
        mv.visitInsn(Constant.SWAP);
    }

    public void aconst_null() {
        mv.visitInsn(Constant.ACONST_NULL);
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
        mv.visitInsn(Constant.MONITORENTER);
    }

    public void monitorexit() {
        mv.visitInsn(Constant.MONITOREXIT);
    }

    public void math(int op, Type type) {
        mv.visitInsn(type.getOpcode(op));
    }

    public void array_load(Type type) {
        mv.visitInsn(type.getOpcode(Constant.IALOAD));
    }

    public void array_store(Type type) {
        mv.visitInsn(type.getOpcode(Constant.IASTORE));
    }

    /**
     * Casts from one primitive numeric type to another
     */
    public void cast_numeric(Type from, Type to) {
        if (from != to) {
            if (from == Type.DOUBLE_TYPE) {
                if (to == Type.FLOAT_TYPE) {
                    mv.visitInsn(Constant.D2F);
                }
                else if (to == Type.LONG_TYPE) {
                    mv.visitInsn(Constant.D2L);
                }
                else {
                    mv.visitInsn(Constant.D2I);
                    cast_numeric(Type.INT_TYPE, to);
                }
            }
            else if (from == Type.FLOAT_TYPE) {
                if (to == Type.DOUBLE_TYPE) {
                    mv.visitInsn(Constant.F2D);
                }
                else if (to == Type.LONG_TYPE) {
                    mv.visitInsn(Constant.F2L);
                }
                else {
                    mv.visitInsn(Constant.F2I);
                    cast_numeric(Type.INT_TYPE, to);
                }
            }
            else if (from == Type.LONG_TYPE) {
                if (to == Type.DOUBLE_TYPE) {
                    mv.visitInsn(Constant.L2D);
                }
                else if (to == Type.FLOAT_TYPE) {
                    mv.visitInsn(Constant.L2F);
                }
                else {
                    mv.visitInsn(Constant.L2I);
                    cast_numeric(Type.INT_TYPE, to);
                }
            }
            else {
                if (to == Type.BYTE_TYPE) {
                    mv.visitInsn(Constant.I2B);
                }
                else if (to == Type.CHAR_TYPE) {
                    mv.visitInsn(Constant.I2C);
                }
                else if (to == Type.DOUBLE_TYPE) {
                    mv.visitInsn(Constant.I2D);
                }
                else if (to == Type.FLOAT_TYPE) {
                    mv.visitInsn(Constant.I2F);
                }
                else if (to == Type.LONG_TYPE) {
                    mv.visitInsn(Constant.I2L);
                }
                else if (to == Type.SHORT_TYPE) {
                    mv.visitInsn(Constant.I2S);
                }
            }
        }
    }

    public void push(int i) {
        if (i < -1) {
            mv.visitLdcInsn(Integer.valueOf(i));
        }
        else if (i <= 5) {
            mv.visitInsn(TypeUtils.ICONST(i));
        }
        else if (i <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Constant.BIPUSH, i);
        }
        else if (i <= Short.MAX_VALUE) {
            mv.visitIntInsn(Constant.SIPUSH, i);
        }
        else {
            mv.visitLdcInsn(Integer.valueOf(i));
        }
    }

    public void push(long value) {
        if (value == 0L || value == 1L) {
            mv.visitInsn(TypeUtils.LCONST(value));
        }
        else {
            mv.visitLdcInsn(new Long(value));
        }
    }

    public void push(float value) {
        if (value == 0f || value == 1f || value == 2f) {
            mv.visitInsn(TypeUtils.FCONST(value));
        }
        else {
            mv.visitLdcInsn(new Float(value));
        }
    }

    public void push(double value) {
        if (value == 0d || value == 1d) {
            mv.visitInsn(TypeUtils.DCONST(value));
        }
        else {
            mv.visitLdcInsn(new Double(value));
        }
    }

    public void push(String value) {
        mv.visitLdcInsn(value);
    }

    public void newArray() {
        newArray(Constant.TYPE_OBJECT);
    }

    public void newArray(Type type) {
        if (TypeUtils.isPrimitive(type)) {
            mv.visitIntInsn(Constant.NEWARRAY, TypeUtils.NEWARRAY(type));
        }
        else {
            emit_type(Constant.ANEWARRAY, type);
        }
    }

    public void arraylength() {
        mv.visitInsn(Constant.ARRAYLENGTH);
    }

    public void load_this() {
        if (Modifier.isStatic(state.access)) {
            throw new IllegalStateException("no 'this' pointer within static method");
        }
        mv.visitVarInsn(Constant.ALOAD, 0);
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
     *            the zero-based index into the argument list
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
        mv.visitVarInsn(t.getOpcode(Constant.ILOAD), pos);
    }

    private void store_local(Type t, int pos) {
        // TODO: make t == null ok?
        mv.visitVarInsn(t.getOpcode(Constant.ISTORE), pos);
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
        mv.visitInsn(state.sig.getReturnType().getOpcode(Constant.IRETURN));
    }

    public void getfield(String name) {
        ClassEmitter.FieldInfo info = ce.getFieldInfo(name);
        int opcode = Modifier.isStatic(info.access) ? Constant.GETSTATIC : Constant.GETFIELD;
        emit_field(opcode, ce.getClassType(), name, info.type);
    }

    public void putfield(String name) {
        ClassEmitter.FieldInfo info = ce.getFieldInfo(name);
        int opcode = Modifier.isStatic(info.access) ? Constant.PUTSTATIC : Constant.PUTFIELD;
        emit_field(opcode, ce.getClassType(), name, info.type);
    }

    public void super_getfield(String name, Type type) {
        emit_field(Constant.GETFIELD, ce.getSuperType(), name, type);
    }

    public void super_putfield(String name, Type type) {
        emit_field(Constant.PUTFIELD, ce.getSuperType(), name, type);
    }

    public void super_getstatic(String name, Type type) {
        emit_field(Constant.GETSTATIC, ce.getSuperType(), name, type);
    }

    public void super_putstatic(String name, Type type) {
        emit_field(Constant.PUTSTATIC, ce.getSuperType(), name, type);
    }

    public void getfield(Type owner, String name, Type type) {
        emit_field(Constant.GETFIELD, owner, name, type);
    }

    public void putfield(Type owner, String name, Type type) {
        emit_field(Constant.PUTFIELD, owner, name, type);
    }

    public void getstatic(Type owner, String name, Type type) {
        emit_field(Constant.GETSTATIC, owner, name, type);
    }

    public void putstatic(Type owner, String name, Type type) {
        emit_field(Constant.PUTSTATIC, owner, name, type);
    }

    // package-protected for EmitUtils, try to fix
    void emit_field(int opcode, Type ctype, String name, Type ftype) {
        mv.visitFieldInsn(opcode, ctype.getInternalName(), name, ftype.getDescriptor());
    }

    public void super_invoke() {
        super_invoke(state.sig);
    }

    public void super_invoke(Signature sig) {
        emit_invoke(Constant.INVOKESPECIAL, ce.getSuperType(), sig, false);
    }

    public void invoke_constructor(Type type) {
        invoke_constructor(type, CSTRUCT_NULL);
    }

    public void super_invoke_constructor() {
        invoke_constructor(ce.getSuperType());
    }

    public void invoke_constructor_this() {
        invoke_constructor(ce.getClassType());
    }

    private void emit_invoke(int opcode, Type type, Signature sig, boolean isInterface) {

        if (sig.getName().equals(Constant.CONSTRUCTOR_NAME)
            && ((opcode == Constant.INVOKEVIRTUAL) || (opcode == Constant.INVOKESTATIC))) {

            // TODO: error
        }
        mv.visitMethodInsn(opcode, //
                           type.getInternalName(), //
                           sig.getName(), //
                           sig.getDescriptor(),
                           isInterface//
        );
    }

    public void invoke_interface(Type owner, Signature sig) {
        emit_invoke(Constant.INVOKEINTERFACE, owner, sig, true);
    }

    public void invoke_virtual(Type owner, Signature sig) {
        emit_invoke(Constant.INVOKEVIRTUAL, owner, sig, false);
    }

    public void invoke_static(Type owner, Signature sig) {
        emit_invoke(Constant.INVOKESTATIC, owner, sig, false);
    }

    public void invoke_static(Type owner, Signature sig, boolean isInterface) {
        emit_invoke(Constant.INVOKESTATIC, owner, sig, isInterface);
    }

    public void invoke_virtual_this(Signature sig) {
        invoke_virtual(ce.getClassType(), sig);
    }

    public void invoke_static_this(Signature sig) {
        invoke_static(ce.getClassType(), sig);
    }

    public void invoke_constructor(Type type, Signature sig) {
        emit_invoke(Constant.INVOKESPECIAL, type, sig, false);
    }

    public void invoke_constructor_this(Signature sig) {
        invoke_constructor(ce.getClassType(), sig);
    }

    public void super_invoke_constructor(Signature sig) {
        invoke_constructor(ce.getSuperType(), sig);
    }

    public void new_instance_this() {
        new_instance(ce.getClassType());
    }

    public void new_instance(Type type) {
        emit_type(Constant.NEW, type);
    }

    private void emit_type(int opcode, Type type) {
        mv.visitTypeInsn(opcode, TypeUtils.isArray(type) ? type.getDescriptor() : type.getInternalName());
    }

    public void aaload(int index) {
        push(index);
        aaload();
    }

    public void aaload() {
        mv.visitInsn(Constant.AALOAD);
    }

    public void aastore() {
        mv.visitInsn(Constant.AASTORE);
    }

    public void athrow() {
        mv.visitInsn(Constant.ATHROW);
    }

    public Label make_label() {
        return new Label();
    }

    public Local make_local() {
        return make_local(Constant.TYPE_OBJECT);
    }

    public Local make_local(Type type) {
        return new Local(newLocal(type.getSize()), type);
    }

    public void checkcast_this() {
        checkcast(ce.getClassType());
    }

    public void checkcast(Type type) {
        if (!type.equals(Constant.TYPE_OBJECT)) {
            emit_type(Constant.CHECKCAST, type);
        }
    }

    public void instance_of(Type type) {
        emit_type(Constant.INSTANCEOF, type);
    }

    public void instance_of_this() {
        instance_of(ce.getClassType());
    }

    public void process_switch(int[] keys, ProcessSwitchCallback callback) {
        float density;
        if (keys.length == 0) {
            density = 0;
        }
        else {
            density = (float) keys.length / (keys[keys.length - 1] - keys[0] + 1);
        }
        process_switch(keys, callback, density >= 0.5f);
    }

    public void process_switch(int[] keys, ProcessSwitchCallback callback, boolean useTable) {
        if (!isSorted(keys)) throw new IllegalArgumentException("keys to switch must be sorted ascending");
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
                            callback.processCase(i + min, end);
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
                        callback.processCase(keys[i], end);
                    }
                }
            }

            mark(def);
            callback.processDefault();
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
            if (keys[i] < keys[i - 1]) return false;
        }
        return true;
    }

    public void mark(Label label) {
        mv.visitLabel(label);
    }

    Label mark() {
        Label label = make_label();
        mv.visitLabel(label);
        return label;
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
     *            the class indicating the current type of the top stack value
     */
    public void box(Type type) {
        if (TypeUtils.isPrimitive(type)) {
            if (type == Type.VOID_TYPE) {
                aconst_null();
            }
            else {
                Type boxed = TypeUtils.getBoxedType(type);

                visitMethodInsn(Constant.INVOKESTATIC,
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
        Type boxed = TypeUtils.getBoxedType(type);

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
        invoke_constructor(boxed, new Signature(Constant.CONSTRUCTOR_NAME, Type.VOID_TYPE, Type.array(type)));
    }

    /**
     * If the argument is a primitive class, replaces the object on the top of the
     * stack with the unwrapped (primitive) equivalent. For example, Character ->
     * char.
     * 
     * @param type
     *            the class indicating the desired type of the top stack value
     * @return true if the value was unboxed
     */
    public void unbox(Type type) {
        Type t = Constant.TYPE_NUMBER;
        Signature sig = null;
        switch (type.getSort()) {
            case Type.VOID :
                return;
            case Type.CHAR :
                t = Constant.TYPE_CHARACTER;
                sig = CHAR_VALUE;
                break;
            case Type.BOOLEAN :
                t = Constant.TYPE_BOOLEAN;
                sig = BOOLEAN_VALUE;
                break;
            case Type.DOUBLE :
                sig = DOUBLE_VALUE;
                break;
            case Type.FLOAT :
                sig = FLOAT_VALUE;
                break;
            case Type.LONG :
                sig = LONG_VALUE;
                break;
            case Type.INT :
            case Type.SHORT :
            case Type.BYTE :
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

        push(state.argumentTypes.length);
        newArray();
        for (int i = 0; i < state.argumentTypes.length; i++) {
            dup();
            push(i);
            load_arg(i);
            box(state.argumentTypes[i]);
            aastore();
        }
    }

    /**
     * Pushes a zero onto the stack if the argument is a primitive class, or a null
     * otherwise.
     */
    public void zero_or_null(Type type) {
        if (TypeUtils.isPrimitive(type)) {
            switch (type.getSort()) {
                case Type.DOUBLE :
                    push(0d);
                    break;
                case Type.LONG :
                    push(0L);
                    break;
                case Type.FLOAT :
                    push(0f);
                    break;
                case Type.VOID :
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
        if (TypeUtils.isPrimitive(type)) {
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
        Signature sig = method.getSignature();
        if (sig.getName().equals(Constant.CONSTRUCTOR_NAME)) {
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
}
