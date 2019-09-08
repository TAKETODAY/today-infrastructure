/*
 * Copyright 2003 The Apache Software Foundation
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
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.Constant;
import cn.taketoday.context.asm.ClassVisitor;
import cn.taketoday.context.asm.FieldVisitor;
import cn.taketoday.context.asm.MethodVisitor;
import cn.taketoday.context.asm.Type;
import cn.taketoday.context.cglib.transform.ClassTransformer;

/**
 * @author Juozas Baliuka, Chris Nokleberg
 */
@SuppressWarnings("all")
public class ClassEmitter extends ClassTransformer {

    private ClassInfo classInfo;
    private Map fieldInfo;

    private static int hookCounter;
    private MethodVisitor rawStaticInit;
    private CodeEmitter staticInit;
    private CodeEmitter staticHook;
    private Signature staticHookSig;

    public ClassEmitter(ClassVisitor cv) {
        setTarget(cv);
    }

    public ClassEmitter() {
//		super(Constant.ASM_API);
    }

    public void setTarget(ClassVisitor cv) {
        this.cv = cv;
        fieldInfo = new HashMap();

        // just to be safe
        staticInit = staticHook = null;
        staticHookSig = null;
    }

    synchronized private static int getNextHook() {
        return ++hookCounter;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    public void beginClass(int version, final int access, String className, final Type superType,
                           final Type[] interfaces, String source) //
    {
        final Type classType = Type.getType('L' + className.replace('.', '/') + ';');
        classInfo = new ClassInfo() {
            public Type getType() {
                return classType;
            }

            public Type getSuperType() {
                return (superType != null) ? superType : Constant.TYPE_OBJECT;
            }

            public Type[] getInterfaces() {
                return interfaces;
            }

            public int getModifiers() {
                return access;
            }
        };
        cv.visit(version, access, classInfo.getType().getInternalName(), null,
                 classInfo.getSuperType().getInternalName(), TypeUtils.toInternalNames(interfaces));

        if (source != null) cv.visitSource(source, null);
        init();
    }

    public CodeEmitter getStaticHook() {
        if (Modifier.isInterface(getAccess())) {
            throw new IllegalStateException("static hook is invalid for this class");
        }
        if (staticHook == null) {
            staticHookSig = new Signature("TODAY$STATICHOOK" + getNextHook(), "()V");
            staticHook = beginMethod(Constant.ACC_STATIC, staticHookSig, null);
            if (staticInit != null) {
                staticInit.invoke_static_this(staticHookSig);
            }
        }
        return staticHook;
    }

    protected void init() {}

    public int getAccess() {
        return classInfo.getModifiers();
    }

    public Type getClassType() {
        return classInfo.getType();
    }

    public Type getSuperType() {
        return classInfo.getSuperType();
    }

    public void endClass() {
        if (staticHook != null && staticInit == null) {
            // force creation of static init
            begin_static();
        }
        if (staticInit != null) {
            staticHook.return_value();
            staticHook.end_method();
            rawStaticInit.visitInsn(Constant.RETURN);
            rawStaticInit.visitMaxs(0, 0);
            staticInit = staticHook = null;
            staticHookSig = null;
        }
        cv.visitEnd();
    }

    public CodeEmitter beginMethod(int access, Signature sig, Type... exceptions) {

        if (classInfo == null) throw new IllegalStateException("classInfo is null! " + this);

        final MethodVisitor visitor = cv.visitMethod(//
                                                     access, //
                                                     sig.getName(), //
                                                     sig.getDescriptor(), //
                                                     null, //
                                                     TypeUtils.toInternalNames(exceptions)//
        );

        if (sig.equals(Constant.SIG_STATIC) && !Modifier.isInterface(getAccess())) {

            rawStaticInit = visitor;
            final MethodVisitor wrapped = new MethodVisitor(visitor) {

                @Override
                public void visitMaxs(int maxStack, int maxLocals) {
                    // ignore
                }

                @Override
                public void visitInsn(int insn) {
                    if (insn != Constant.RETURN) {
                        super.visitInsn(insn);
                    }
                }
            };
            staticInit = new CodeEmitter(this, wrapped, access, sig, exceptions);
            if (staticHook == null) {
                // force static hook creation
                getStaticHook();
            }
            else {
                staticInit.invoke_static_this(staticHookSig);
            }
            return staticInit;
        }
        else if (sig.equals(staticHookSig)) {

            return new CodeEmitter(this, visitor, access, sig, exceptions) {

                public boolean isStaticHook() {
                    return true;
                }
            };
        }
        else {
            return new CodeEmitter(this, visitor, access, sig, exceptions);
        }
    }

    public CodeEmitter begin_static() {
        return beginMethod(Constant.ACC_STATIC, Constant.SIG_STATIC, null);
    }

    public void declare_field(int access, String name, Type type, Object value) {
        FieldInfo existing = (FieldInfo) fieldInfo.get(name);
        FieldInfo info = new FieldInfo(access, name, type, value);
        if (existing != null) {
            if (!info.equals(existing)) {
                throw new IllegalArgumentException("Field \"" + name + "\" has been declared differently");
            }
        }
        else {
            fieldInfo.put(name, info);
            cv.visitField(access, name, type.getDescriptor(), null, value);
        }
    }

    // TODO: make public?
    boolean isFieldDeclared(String name) {
        return fieldInfo.get(name) != null;
    }

    FieldInfo getFieldInfo(String name) {
        FieldInfo field = (FieldInfo) fieldInfo.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Field " + name + " is not declared in " + getClassType().getClassName());
        }
        return field;
    }

    static class FieldInfo {
        int access;
        String name;
        Type type;
        Object value;

        public FieldInfo(int access, String name, Type type, Object value) {
            this.access = access;
            this.name = name;
            this.type = type;
            this.value = value;
        }

        public boolean equals(Object o) {
            if (o == null) return false;
            if (!(o instanceof FieldInfo)) return false;
            FieldInfo other = (FieldInfo) o;
            if (access != other.access || !name.equals(other.name) || !type.equals(other.type)) {
                return false;
            }
            if ((value == null) ^ (other.value == null)) return false;
            if (value != null && !value.equals(other.value)) return false;
            return true;
        }

        public int hashCode() {
            return access ^ name.hashCode() ^ type.hashCode() ^ ((value == null) ? 0 : value.hashCode());
        }
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        beginClass(version, access, name.replace('/', '.'), TypeUtils.fromInternalName(superName),
                   TypeUtils.fromInternalNames(interfaces), null); // TODO
    }

    public void visitEnd() {
        endClass();
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        declare_field(access, name, Type.getType(desc), value);
        return null; // TODO
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return beginMethod(access, new Signature(name, desc), TypeUtils.fromInternalNames(exceptions));
    }
}
