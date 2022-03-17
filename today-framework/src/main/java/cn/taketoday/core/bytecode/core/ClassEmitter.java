/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core.bytecode.core;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.FieldVisitor;
import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.core.bytecode.transform.ClassTransformer;
import cn.taketoday.lang.Constant;

/**
 * @author Juozas Baliuka, Chris Nokleberg
 */
public class ClassEmitter extends ClassTransformer {

  private ClassInfo classInfo;
  private Map<String, FieldInfo> fieldInfo;

  private static int hookCounter;
  private MethodVisitor rawStaticInit;
  private CodeEmitter staticInit;
  private CodeEmitter staticHook;
  private MethodSignature staticHookSig;

  public ClassEmitter(ClassVisitor cv) {
    setTarget(cv);
  }

  public ClassEmitter() {
    //		super(Constant.ASM_API);
  }

  public void setTarget(ClassVisitor cv) {
    this.cv = cv;
    fieldInfo = new HashMap<>();

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

  public void beginClass(final int access,
                         final String className,
                         final Class<?> superType, final Class<?>... interfaces) {
    beginClass(Opcodes.JAVA_VERSION, access, className, Type.fromClass(superType), Type.getTypes(interfaces), Constant.SOURCE_FILE);
  }

  public void beginClass(final int access,
                         final String className,
                         final Class<?> superType,
                         final String source, final Class<?>... interfaces) {

    beginClass(Opcodes.JAVA_VERSION, access, className, Type.fromClass(superType), Type.getTypes(interfaces), source);
  }

  public void beginClass(final int version,
                         final int access,
                         final String className,
                         final Class<?> superType,
                         final String source, final Class<?>... interfaces) {

    beginClass(version, access, className, Type.fromClass(superType), Type.getTypes(interfaces), source);
  }

  public void beginClass(final int version,
                         final int access,
                         final String className,
                         final Type superType,
                         final String source, final Type... interfaces) {

    beginClass(version, access, className, superType, interfaces, source);
  }

  public void beginClass(final int version,
                         final int access,
                         final String className,
                         final Type superType,
                         final Type[] interfaces, String source) //
  {
    final Type classType = Type.fromDescriptor('L' + className.replace('.', '/') + ';');
    classInfo = new ClassInfo() {
      public Type getType() {
        return classType;
      }

      public Type getSuperType() {
        return (superType != null) ? superType : Type.TYPE_OBJECT;
      }

      public Type[] getInterfaces() {
        return interfaces;
      }

      public int getModifiers() {
        return access;
      }
    };
    cv.visit(version, access, classInfo.getType().getInternalName(), null,
             classInfo.getSuperType().getInternalName(), Type.toInternalNames(interfaces));

    if (source != null)
      cv.visitSource(source, null);
    init();
  }

  public void beginClass(final int access,
                         final String name,
                         final String superName,
                         final String... interfaces) //
  {
    beginClass(Opcodes.JAVA_VERSION, access, name, superName, interfaces);
  }

  public void beginClass(final int version,
                         final int access,
                         final String name,
                         final String superName,
                         final String... interfaces) //
  {
    beginClass(version, access, name, Constant.SOURCE_FILE, superName, interfaces);
  }

  /**
   * @param name class full name
   */
  public void beginClass(final int version,
                         final int access,
                         final String name, // class name
                         final String source,
                         final String superName, // typeDescriptor
                         final String... interfaces) //typeDescriptor
  {
    Type superType = Type.fromDescriptor(superName);
    final Type[] array = Type.getTypes(interfaces);
    Type type = Type.fromInternalName(name.replace('.', '/'));

    classInfo = new ClassInfo() {

      public Type getType() {
        return type;
      }

      public Type getSuperType() {
        return superType;
      }

      public Type[] getInterfaces() {
        return array;
      }

      public int getModifiers() {
        return access;
      }
    };
    cv.visit(version, access, name, null, superType.getInternalName(), Type.toInternalNames(array));

    if (source != null) {
      cv.visitSource(source, null);
    }
    init();
  }

  public CodeEmitter getStaticHook() {
    if (Modifier.isInterface(getAccess())) {
      throw new IllegalStateException("static hook is invalid for this class");
    }
    if (staticHook == null) {
      staticHookSig = new MethodSignature("today$StaticHook" + getNextHook(), "()V");
      staticHook = beginMethod(Opcodes.ACC_STATIC, staticHookSig);
      if (staticInit != null) {
        staticInit.invoke_static_this(staticHookSig);
      }
    }
    return staticHook;
  }

  protected void init() { }

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
      if (staticHook != null) {
        staticHook.returnValue();
        staticHook.end_method();
      }
      rawStaticInit.visitInsn(Opcodes.RETURN);
      rawStaticInit.visitMaxs(0, 0);
      staticInit = staticHook = null;
      staticHookSig = null;
    }
    cv.visitEnd();
  }

  public CodeEmitter beginMethod(int access, Method method) {
    return beginMethod(access, MethodSignature.from(method), Type.getExceptionTypes(method));
  }

  public CodeEmitter beginMethod(int access, MethodSignature sig, Type... exceptions) {
    if (classInfo == null) {
      throw new IllegalStateException("classInfo is null! " + this);
    }

    final MethodVisitor visitor = cv.visitMethod(
            access, sig.getName(), sig.getDescriptor(), null, Type.toInternalNames(exceptions));

    if (sig.equals(MethodSignature.SIG_STATIC) && !Modifier.isInterface(getAccess())) {
      return begin_static(true, visitor);
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
    return begin_static(true);
  }

  public CodeEmitter begin_static(boolean hook) {
    final MethodSignature sigStatic = MethodSignature.SIG_STATIC;
    return begin_static(hook, cv.visitMethod(
            Opcodes.ACC_STATIC, sigStatic.getName(), sigStatic.getDescriptor(), null, null));
  }

  public CodeEmitter begin_static(boolean hook, MethodVisitor visitor) {
    rawStaticInit = visitor;
    final MethodVisitor wrapped = new MethodVisitor(visitor) {
      public void visitMaxs(int maxStack, int maxLocals) { }

      public void visitInsn(int insn) {
        if (insn != Opcodes.RETURN) {
          super.visitInsn(insn);
        }
      }
    };
    staticInit = new CodeEmitter(this, wrapped, Opcodes.ACC_STATIC, MethodSignature.SIG_STATIC, null);
    if (hook) {
      if (staticHook == null) {
        getStaticHook(); // force static hook creation
      }
      else {
        staticInit.invoke_static_this(staticHookSig);
      }
    }
    return staticInit;
  }

  public void declare_field(int access, String name, Type type, Object value) {
    FieldInfo existing = fieldInfo.get(name);
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

  public boolean isFieldDeclared(String name) {
    return fieldInfo.get(name) != null;
  }

  FieldInfo getFieldInfo(String name) {
    FieldInfo field = fieldInfo.get(name);
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
      if (!(o instanceof final FieldInfo other)) {
        return false;
      }
      if (access != other.access || !name.equals(other.name) || !type.equals(other.type)) {
        return false;
      }
      final Object value = this.value;
      if ((value == null) ^ (other.value == null)) {
        return false;
      }
      return value == null || value.equals(other.value);
    }

    public int hashCode() {
      return access ^ name.hashCode() ^ type.hashCode() ^ ((value == null) ? 0 : value.hashCode());
    }
  }

  @Override
  public void visit(
          int version, int access, String name, String signature, String superName, String[] interfaces) {
    beginClass(version, access, name.replace('/', '.'),
               Type.fromInternalName(superName),
               Type.fromInternalNames(interfaces), null); // TODO
  }

  @Override
  public void visitEnd() {
    endClass();
  }

  @Override
  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    declare_field(access, name, Type.fromDescriptor(desc), value);
    return null; // TODO
  }

  @Override
  public MethodVisitor visitMethod(
          int access, String name, String desc, String signature, String[] exceptions) {
    return beginMethod(access, new MethodSignature(name, desc), Type.fromInternalNames(exceptions));
  }
}
