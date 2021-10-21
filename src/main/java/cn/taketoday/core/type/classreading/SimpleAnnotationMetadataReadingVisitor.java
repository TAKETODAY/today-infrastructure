/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.taketoday.core.type.classreading;

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

import java.util.ArrayList;
import java.util.LinkedHashSet;

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
