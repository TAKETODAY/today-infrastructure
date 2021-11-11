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

package cn.taketoday.core.type.classreading;

import java.util.LinkedHashSet;

import cn.taketoday.core.bytecode.AnnotationVisitor;
import cn.taketoday.core.bytecode.Attribute;
import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.FieldVisitor;
import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.type.ClassMetadata;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * ASM class visitor which looks only for the class name and implemented types,
 * exposing them through the {@link cn.taketoday.core.type.ClassMetadata}
 * interface.
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @author Mark Fisher
 * @author Ramnivas Laddad
 * @author Chris Beams
 * @since 4.0
 */
class ClassMetadataReadingVisitor extends ClassVisitor implements ClassMetadata {

  private String className = "";

  private boolean isInterface;

  private boolean isAnnotation;

  private boolean isAbstract;

  private boolean isFinal;

  private int access;

  @Nullable
  private String enclosingClassName;

  private boolean independentInnerClass;

  @Nullable
  private String superClassName;

  private String[] interfaces = Constant.EMPTY_STRING_ARRAY;

  private final LinkedHashSet<String> memberClassNames = new LinkedHashSet<>(4);

  @Override
  public void visit(
          int version, int access, String name, String signature, @Nullable String supername, String[] interfaces) {

    this.access = access;
    this.className = ClassUtils.convertResourcePathToClassName(name);
    this.isInterface = ((access & Opcodes.ACC_INTERFACE) != 0);
    this.isAnnotation = ((access & Opcodes.ACC_ANNOTATION) != 0);
    this.isAbstract = ((access & Opcodes.ACC_ABSTRACT) != 0);
    this.isFinal = ((access & Opcodes.ACC_FINAL) != 0);

    if (supername != null && !this.isInterface) {
      this.superClassName = ClassUtils.convertResourcePathToClassName(supername);
    }

    this.interfaces = new String[interfaces.length];
    for (int i = 0; i < interfaces.length; i++) {
      this.interfaces[i] = ClassUtils.convertResourcePathToClassName(interfaces[i]);
    }
  }

  @Override
  public void visitOuterClass(String owner, String name, String desc) {
    this.enclosingClassName = ClassUtils.convertResourcePathToClassName(owner);
  }

  @Override
  public void visitInnerClass(String name, @Nullable String outerName, String innerName, int access) {
    if (outerName != null) {
      String fqName = ClassUtils.convertResourcePathToClassName(name);
      String fqOuterName = ClassUtils.convertResourcePathToClassName(outerName);
      if (this.className.equals(fqName)) {
        this.enclosingClassName = fqOuterName;
        this.independentInnerClass = ((access & Opcodes.ACC_STATIC) != 0);
      }
      else if (this.className.equals(fqOuterName)) {
        this.memberClassNames.add(fqName);
      }
    }
  }

  @Override
  public void visitSource(String source, String debug) { }

  @Override
  @Nullable
  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    return new EmptyAnnotationVisitor();
  }

  @Override
  public void visitAttribute(Attribute attr) { }

  @Override
  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    return new FieldVisitor();
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    return new MethodVisitor();
  }

  @Override
  public void visitEnd() { }

  @Override
  public String getClassName() {
    return this.className;
  }

  @Override
  public boolean isInterface() {
    return this.isInterface;
  }

  @Override
  public boolean isAnnotation() {
    return this.isAnnotation;
  }

  @Override
  public boolean isAbstract() {
    return this.isAbstract;
  }

  @Override
  public boolean isFinal() {
    return this.isFinal;
  }

  @Override
  public boolean isIndependent() {
    return (this.enclosingClassName == null || this.independentInnerClass);
  }

  @Override
  public boolean hasEnclosingClass() {
    return (this.enclosingClassName != null);
  }

  @Override
  @Nullable
  public String getEnclosingClassName() {
    return this.enclosingClassName;
  }

  @Override
  @Nullable
  public String getSuperClassName() {
    return this.superClassName;
  }

  @Override
  public String[] getInterfaceNames() {
    return this.interfaces;
  }

  @Override
  public int getModifiers() {
    return access;
  }

  @Override
  public String[] getMemberClassNames() {
    return StringUtils.toStringArray(this.memberClassNames);
  }

  private static class EmptyAnnotationVisitor extends AnnotationVisitor {

    @Override
    public AnnotationVisitor visitAnnotation(String name, String desc) {
      return this;
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
      return this;
    }
  }

}
