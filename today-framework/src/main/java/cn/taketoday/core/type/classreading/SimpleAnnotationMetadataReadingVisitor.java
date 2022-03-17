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

import java.util.ArrayList;
import java.util.LinkedHashSet;

import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.bytecode.AnnotationVisitor;
import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * ASM class visitor that creates {@link SimpleAnnotationMetadata}.
 *
 * @author Phillip Webb
 * @since 4.0
 */
final class SimpleAnnotationMetadataReadingVisitor extends ClassVisitor {

  @Nullable
  private final ClassLoader classLoader;

  private String className = Constant.BLANK;

  private int access;

  @Nullable
  private String superClassName;

  @Nullable
  private LinkedHashSet<String> interfaceNames;

  @Nullable
  private String enclosingClassName;

  private boolean independentInnerClass;

  @Nullable
  private ArrayList<MergedAnnotation<?>> annotations;

  @Nullable
  private LinkedHashSet<String> memberClassNames;

  @Nullable
  private LinkedHashSet<MethodMetadata> declaredMethods;

  @Nullable
  private SimpleAnnotationMetadata metadata;

  @Nullable
  private Source source;

  SimpleAnnotationMetadataReadingVisitor(@Nullable ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public void visit(int version, int access, String name, String signature,
                    @Nullable String supername, String[] interfaces) {

    this.className = toClassName(name);
    this.access = access;
    if (supername != null && !isInterface(access)) {
      this.superClassName = toClassName(supername);
    }

    if (ObjectUtils.isNotEmpty(interfaces)) {
      if (interfaceNames == null) {
        interfaceNames = new LinkedHashSet<>(interfaces.length);
      }
      for (String anInterface : interfaces) {
        interfaceNames.add(toClassName(anInterface));
      }
    }
  }

  @Override
  public void visitOuterClass(String owner, String name, String desc) {
    this.enclosingClassName = toClassName(owner);
  }

  @Override
  public void visitInnerClass(String name, @Nullable String outerName, String innerName, int access) {
    if (outerName != null) {
      String className = toClassName(name);
      String outerClassName = toClassName(outerName);
      if (this.className.equals(className)) {
        this.enclosingClassName = outerClassName;
        this.independentInnerClass = (access & Opcodes.ACC_STATIC) != 0;
      }
      else if (this.className.equals(outerClassName)) {
        if (memberClassNames == null) {
          this.memberClassNames = new LinkedHashSet<>(4);
        }
        memberClassNames.add(className);
      }
    }
  }

  @Override
  @Nullable
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    if (annotations == null) {
      annotations = new ArrayList<>();
    }
    return MergedAnnotationReadingVisitor.get(this.classLoader, getSource(),
            descriptor, visible, this.annotations::add);
  }

  @Override
  @Nullable
  public MethodVisitor visitMethod(
          int access, String name, String descriptor, String signature, String[] exceptions) {
    // Skip bridge methods and constructors - we're only interested in original user methods.
    if (isBridge(access) || "<init>".equals(name)) {
      return null;
    }
    if (declaredMethods == null) {
      declaredMethods = new LinkedHashSet<>();
    }
    return new SimpleMethodMetadataReadingVisitor(
            classLoader, className, access, name, descriptor, declaredMethods::add);
  }

  @Override
  public void visitEnd() {
    this.metadata = new SimpleAnnotationMetadata(
            className, access, enclosingClassName, superClassName, independentInnerClass,
            interfaceNames, memberClassNames, declaredMethods, MergedAnnotations.valueOf(annotations));
  }

  public SimpleAnnotationMetadata getMetadata() {
    Assert.state(this.metadata != null, "AnnotationMetadata not initialized");
    return this.metadata;
  }

  private Source getSource() {
    Source source = this.source;
    if (source == null) {
      source = new Source(this.className);
      this.source = source;
    }
    return source;
  }

  private String toClassName(String name) {
    return ClassUtils.convertResourcePathToClassName(name);
  }

  private boolean isBridge(int access) {
    return (access & Opcodes.ACC_BRIDGE) != 0;
  }

  private boolean isInterface(int access) {
    return (access & Opcodes.ACC_INTERFACE) != 0;
  }

  /**
   * {@link MergedAnnotation} source.
   */
  private record Source(String className) {

    @Override
    public int hashCode() {
      return this.className.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      return this.className.equals(((Source) obj).className);
    }

    @Override
    public String toString() {
      return this.className;
    }

  }

}
