/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.bytecode.core;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import infra.bytecode.ClassVisitor;
import infra.bytecode.FieldVisitor;
import infra.bytecode.MethodVisitor;
import infra.bytecode.Opcodes;
import infra.bytecode.Type;
import infra.bytecode.commons.MethodSignature;
import infra.bytecode.transform.ClassTransformer;
import infra.lang.Constant;

/**
 * @author Juozas Baliuka, Chris Nokleberg
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class ClassEmitter extends ClassTransformer {

  private ClassInfo classInfo;

  private Map<String, FieldInfo> fieldInfo;

  private MethodVisitor rawStaticInit;

  private @Nullable CodeEmitter staticInit;

  public ClassEmitter(ClassVisitor cv) {
    setTarget(cv);
  }

  public ClassEmitter() {

  }

  public void setTarget(ClassVisitor cv) {
    this.cv = cv;
    fieldInfo = new HashMap<>();

    // just to be safe
    staticInit = null;
    rawStaticInit = null;
  }

  public ClassInfo getClassInfo() {
    return classInfo;
  }

  public void beginClass(int version, int access, String className,
          @Nullable Type superType, Type @Nullable [] interfaces, @Nullable String source) {

    Type classType = Type.forDescriptor('L' + className.replace('.', '/') + ';');
    classInfo = new ClassInfo() {
      public Type getType() {
        return classType;
      }

      public Type getSuperType() {
        return superType != null ? superType : Type.TYPE_OBJECT;
      }

      public Type @Nullable [] getInterfaces() {
        return interfaces;
      }

      public int getModifiers() {
        return access;
      }
    };

    cv.visit(version, access, classInfo.getType().getInternalName(), null,
            classInfo.getSuperType().getInternalName(), Type.toInternalNames(interfaces));

    if (source != null) {
      cv.visitSource(source, null);
    }
    postBeginClass();
  }

  public void beginClass(int access, String name, String superName, String... interfaces) {
    beginClass(Opcodes.JAVA_VERSION, access, name, superName, interfaces);
  }

  public void beginClass(int version, int access, String name, String superName, String... interfaces) {
    beginClass(version, access, name, Constant.SOURCE_FILE, superName, interfaces);
  }

  /**
   * @param name class full name
   */
  public void beginClass(final int version, final int access,
          final String name, // class name
          final String source,
          final String superName, // typeDescriptor
          final String... interfaces) //typeDescriptor
  {
    Type superType = Type.forDescriptor(superName);
    final Type[] array = Type.getTypes(interfaces);
    Type type = Type.forInternalName(name.replace('.', '/'));

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
    postBeginClass();
  }

  public CodeEmitter getStaticInit() {
    if (Modifier.isInterface(getAccess())) {
      throw new IllegalStateException("static hook is invalid for this class");
    }
    if (staticInit == null) {
      final MethodSignature sigStatic = MethodSignature.STATIC_INIT;
      rawStaticInit = cv.visitMethod(Opcodes.ACC_STATIC, sigStatic.getName(), sigStatic.getDescriptor(), null, null);

      MethodVisitor wrapped = new MethodVisitor(rawStaticInit) {
        public void visitMaxs(int maxStack, int maxLocals) {
        }

        public void visitInsn(int insn) {
          if (insn != Opcodes.RETURN) {
            super.visitInsn(insn);
          }
        }
      };

      staticInit = new CodeEmitter(this, wrapped, Opcodes.ACC_STATIC, sigStatic, null);
    }

    return staticInit;
  }

  protected void postBeginClass() {
  }

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
    if (staticInit != null) {
      rawStaticInit.visitInsn(Opcodes.RETURN);
      rawStaticInit.visitMaxs(0, 0);
      staticInit = null;
    }
    cv.visitEnd();
  }

  public CodeEmitter beginMethod(int access, Method method) {
    return beginMethod(access, MethodSignature.from(method), Type.forExceptionTypes(method));
  }

  public CodeEmitter beginMethod(int access, MethodSignature sig, Type @Nullable ... exceptions) {
    if (classInfo == null) {
      throw new IllegalStateException("classInfo is null! " + this);
    }

    final MethodVisitor visitor = cv.visitMethod(
            access, sig.getName(), sig.getDescriptor(), null, Type.toInternalNames(exceptions));
    return new CodeEmitter(this, visitor, access, sig, exceptions);
  }

  public void declare_field(int access, String name, Type type, @Nullable Object value) {
    FieldInfo existing = fieldInfo.get(name);
    FieldInfo info = new FieldInfo(access, name, type, value);
    if (existing != null) {
      if (!info.equals(existing)) {
        throw new IllegalArgumentException("Field \"%s\" has been declared differently".formatted(name));
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
      throw new IllegalArgumentException("Field %s is not declared in %s".formatted(name, getClassType().getClassName()));
    }
    return field;
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    beginClass(version, access, name.replace('/', '.'),
            Type.forInternalName(superName),
            Type.forInternalNames(interfaces), null); // TODO
  }

  @Override
  public void visitEnd() {
    endClass();
  }

  @Override
  public FieldVisitor visitField(int access, String name, String desc, String signature, @Nullable Object value) {
    declare_field(access, name, Type.forDescriptor(desc), value);
    return null; // TODO
  }

  @Override
  public MethodVisitor visitMethod(
          int access, String name, String desc, String signature, String[] exceptions) {
    return beginMethod(access, new MethodSignature(name, desc), Type.forInternalNames(exceptions));
  }

  static class FieldInfo {
    public final int access;
    public final Type type;
    public final String name;

    public final @Nullable Object value;

    public FieldInfo(int access, String name, Type type, @Nullable Object value) {
      this.access = access;
      this.name = name;
      this.type = type;
      this.value = value;
    }

  }

}
