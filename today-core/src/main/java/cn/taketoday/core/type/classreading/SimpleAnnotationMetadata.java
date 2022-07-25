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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

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
  public int getModifiers() {
    return access;
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
