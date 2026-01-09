/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core.type.classreading;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import infra.bytecode.AnnotationVisitor;
import infra.bytecode.ClassVisitor;
import infra.bytecode.MethodVisitor;
import infra.bytecode.Opcodes;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.type.MethodMetadata;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.util.ClassUtils;
import infra.util.ObjectUtils;

/**
 * ASM class visitor that creates {@link SimpleAnnotationMetadata}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
  public void visit(int version, int access, String name, @Nullable String signature,
          @Nullable String supername, String @Nullable [] interfaces) {

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
  public MethodVisitor visitMethod(int access, String name, String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
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
