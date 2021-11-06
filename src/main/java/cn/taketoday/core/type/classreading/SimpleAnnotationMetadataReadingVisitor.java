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
import cn.taketoday.util.StringUtils;

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

  private String[] interfaceNames = Constant.EMPTY_STRING_ARRAY;

  @Nullable
  private String enclosingClassName;

  private boolean independentInnerClass;

  private final LinkedHashSet<String> memberClassNames = new LinkedHashSet<>(4);

  private final ArrayList<MergedAnnotation<?>> annotations = new ArrayList<>();

  private final ArrayList<SimpleMethodMetadata> annotatedMethods = new ArrayList<>();

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
    this.interfaceNames = new String[interfaces.length];
    for (int i = 0; i < interfaces.length; i++) {
      this.interfaceNames[i] = toClassName(interfaces[i]);
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
        this.independentInnerClass = ((access & Opcodes.ACC_STATIC) != 0);
      }
      else if (this.className.equals(outerClassName)) {
        this.memberClassNames.add(className);
      }
    }
  }

  @Override
  @Nullable
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    return MergedAnnotationReadingVisitor.get(this.classLoader, getSource(),
            descriptor, visible, this.annotations::add);
  }

  @Override
  @Nullable
  public MethodVisitor visitMethod(
          int access, String name, String descriptor, String signature, String[] exceptions) {

    // Skip bridge methods - we're only interested in original
    // annotation-defining user methods. On JDK 8, we'd otherwise run into
    // double detection of the same annotated method...
    if (isBridge(access)) {
      return null;
    }
    return new SimpleMethodMetadataReadingVisitor(
            this.classLoader, this.className, access, name, descriptor, this.annotatedMethods::add);
  }

  @Override
  public void visitEnd() {
    String[] memberClassNames = StringUtils.toStringArray(this.memberClassNames);
    MethodMetadata[] annotatedMethods = this.annotatedMethods.toArray(new MethodMetadata[0]);
    MergedAnnotations annotations = MergedAnnotations.of(this.annotations);
    this.metadata = new SimpleAnnotationMetadata(
            this.className, this.access, this.enclosingClassName,
            this.superClassName, this.independentInnerClass,
            this.interfaceNames, memberClassNames, annotatedMethods, annotations);
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
  private static final class Source {

    private final String className;

    Source(String className) {
      this.className = className;
    }

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
