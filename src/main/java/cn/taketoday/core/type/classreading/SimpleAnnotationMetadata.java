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

import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.lang.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

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

  private final String[] interfaceNames;

  private final String[] memberClassNames;

  private final MethodMetadata[] annotatedMethods;

  private final MergedAnnotations annotations;

  @Nullable
  private Set<String> annotationTypes;

  SimpleAnnotationMetadata(
          String className, int access, @Nullable String enclosingClassName,
          @Nullable String superClassName, boolean independentInnerClass, String[] interfaceNames,
          String[] memberClassNames, MethodMetadata[] annotatedMethods, MergedAnnotations annotations) {

    this.className = className;
    this.access = access;
    this.enclosingClassName = enclosingClassName;
    this.superClassName = superClassName;
    this.independentInnerClass = independentInnerClass;
    this.interfaceNames = interfaceNames;
    this.memberClassNames = memberClassNames;
    this.annotatedMethods = annotatedMethods;
    this.annotations = annotations;
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
    return (this.enclosingClassName == null || this.independentInnerClass);
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
    return this.interfaceNames.clone();
  }

  @Override
  public String[] getMemberClassNames() {
    return this.memberClassNames.clone();
  }

  @Override
  public Set<String> getAnnotationTypes() {
    Set<String> annotationTypes = this.annotationTypes;
    if (annotationTypes == null) {
      annotationTypes = Collections.unmodifiableSet(
              AnnotationMetadata.super.getAnnotationTypes());
      this.annotationTypes = annotationTypes;
    }
    return annotationTypes;
  }

  @Override
  public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
    LinkedHashSet<MethodMetadata> annotatedMethods = null;
    for (MethodMetadata annotatedMethod : this.annotatedMethods) {
      if (annotatedMethod.isAnnotated(annotationName)) {
        if (annotatedMethods == null) {
          annotatedMethods = new LinkedHashSet<>(4);
        }
        annotatedMethods.add(annotatedMethod);
      }
    }
    return annotatedMethods != null ? annotatedMethods : Collections.emptySet();
  }

  @Override
  public MethodMetadata[] getMethods() {
    return annotatedMethods;
  }

  @Override
  public MergedAnnotations getAnnotations() {
    return this.annotations;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return ((this == obj) || ((obj instanceof SimpleAnnotationMetadata) &&
            this.className.equals(((SimpleAnnotationMetadata) obj).className)));
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
