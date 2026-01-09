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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import infra.bytecode.Opcodes;
import infra.core.annotation.MergedAnnotations;
import infra.core.type.AnnotationMetadata;
import infra.core.type.MethodMetadata;
import infra.lang.Constant;
import infra.util.StringUtils;

/**
 * {@link AnnotationMetadata} created from a
 * {@link SimpleAnnotationMetadataReadingVisitor}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 */
final class SimpleAnnotationMetadata implements AnnotationMetadata {

  private final String className;

  private final int access;

  @Nullable
  private final String enclosingClassName;

  @Nullable
  private final String superClassName;

  private final boolean independentInnerClass;

  @Nullable
  private final Set<String> interfaceNames;

  @Nullable
  private final Set<String> memberClassNames;

  @Nullable
  private final Set<MethodMetadata> declaredMethods;

  private final MergedAnnotations annotations;

  @Nullable
  private Set<String> annotationTypes;

  SimpleAnnotationMetadata(
          String className, int access,
          @Nullable String enclosingClassName,
          @Nullable String superClassName,
          boolean independentInnerClass,
          @Nullable Set<String> interfaceNames,
          @Nullable Set<String> memberClassNames,
          @Nullable Set<MethodMetadata> declaredMethods, MergedAnnotations annotations
  ) {

    this.access = access;
    this.className = className;
    this.annotations = annotations;
    this.superClassName = superClassName;
    this.enclosingClassName = enclosingClassName;
    this.independentInnerClass = independentInnerClass;

    this.interfaceNames = interfaceNames;
    this.declaredMethods = declaredMethods;
    this.memberClassNames = memberClassNames;
  }

  @Override
  public String getClassName() {
    return this.className;
  }

  @Override
  public boolean isInterface() {
    return (this.access & Opcodes.ACC_INTERFACE) != 0;
  }

  @Override
  public boolean isAnnotation() {
    return (this.access & Opcodes.ACC_ANNOTATION) != 0;
  }

  @Override
  public boolean isAbstract() {
    return (this.access & Opcodes.ACC_ABSTRACT) != 0;
  }

  @Override
  public boolean isFinal() {
    return (this.access & Opcodes.ACC_FINAL) != 0;
  }

  @Override
  public boolean isIndependent() {
    return this.enclosingClassName == null || this.independentInnerClass;
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
    if (interfaceNames == null) {
      return Constant.EMPTY_STRING_ARRAY;
    }
    return StringUtils.toStringArray(interfaceNames);
  }

  @Override
  public String[] getMemberClassNames() {
    if (memberClassNames == null) {
      return Constant.EMPTY_STRING_ARRAY;
    }
    return StringUtils.toStringArray(memberClassNames);
  }

  @Override
  public Set<String> getAnnotationTypes() {
    Set<String> annotationTypes = this.annotationTypes;
    if (annotationTypes == null) {
      annotationTypes = Collections.unmodifiableSet(AnnotationMetadata.super.getAnnotationTypes());
      this.annotationTypes = annotationTypes;
    }
    return annotationTypes;
  }

  @Override
  public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
    if (declaredMethods != null) {
      LinkedHashSet<MethodMetadata> annotatedMethods = new LinkedHashSet<>(declaredMethods.size());
      for (MethodMetadata annotatedMethod : declaredMethods) {
        if (annotatedMethod.isAnnotated(annotationName)) {
          annotatedMethods.add(annotatedMethod);
        }
      }
      return annotatedMethods;
    }
    return Collections.emptySet();
  }

  @Override
  public Set<MethodMetadata> getDeclaredMethods() {
    if (declaredMethods == null) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(declaredMethods);
  }

  @Override
  public MergedAnnotations getAnnotations() {
    return this.annotations;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return (this == obj) || (
            (obj instanceof SimpleAnnotationMetadata)
                    && this.className.equals(((SimpleAnnotationMetadata) obj).className)
    );
  }

  @Override
  public int hashCode() {
    return this.className.hashCode();
  }

  @Override
  public String toString() {
    return this.className;
  }

}
