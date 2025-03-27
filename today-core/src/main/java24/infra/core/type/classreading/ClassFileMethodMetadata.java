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

package infra.core.type.classreading;

import java.lang.classfile.AccessFlags;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.util.Collections;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.type.MethodMetadata;
import infra.lang.Nullable;
import infra.util.ClassUtils;

/**
 * {@link MethodMetadata} extracted from class bytecode using the
 * {@link java.lang.classfile.ClassFile} API.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
class ClassFileMethodMetadata implements MethodMetadata {

  private final String methodName;

  private final AccessFlags accessFlags;

  @Nullable
  private final String declaringClassName;

  private final String returnTypeName;

  // The source implements equals(), hashCode(), and toString() for the underlying method.
  private final Object source;

  private final MergedAnnotations annotations;

  ClassFileMethodMetadata(String methodName, AccessFlags accessFlags, @Nullable String declaringClassName, String returnTypeName, Object source, MergedAnnotations annotations) {
    this.methodName = methodName;
    this.accessFlags = accessFlags;
    this.declaringClassName = declaringClassName;
    this.returnTypeName = returnTypeName;
    this.source = source;
    this.annotations = annotations;
  }

  @Override
  public String getMethodName() {
    return this.methodName;
  }

  @Nullable
  @Override
  public String getDeclaringClassName() {
    return this.declaringClassName;
  }

  @Override
  public String getReturnTypeName() {
    return this.returnTypeName;
  }

  @Override
  public boolean isAbstract() {
    return this.accessFlags.has(AccessFlag.ABSTRACT);
  }

  @Override
  public boolean isStatic() {
    return this.accessFlags.has(AccessFlag.STATIC);
  }

  @Override
  public boolean isFinal() {
    return this.accessFlags.has(AccessFlag.FINAL);
  }

  @Override
  public boolean isOverridable() {
    return !isStatic() && !isFinal() && !isPrivate();
  }

  private boolean isPrivate() {
    return this.accessFlags.has(AccessFlag.PRIVATE);
  }

  public boolean isSynthetic() {
    return this.accessFlags.has(AccessFlag.SYNTHETIC);
  }

  public boolean isDefaultConstructor() {
    return this.methodName.equals("<init>");
  }

  @Override
  public MergedAnnotations getAnnotations() {
    return this.annotations;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof ClassFileMethodMetadata that && this.source.equals(that.source)));
  }

  @Override
  public int hashCode() {
    return this.source.hashCode();
  }

  @Override
  public String toString() {
    return this.source.toString();
  }

  static ClassFileMethodMetadata of(MethodModel methodModel, @Nullable ClassLoader classLoader) {
    String methodName = methodModel.methodName().stringValue();
    AccessFlags flags = methodModel.flags();
    String declaringClassName = methodModel.parent().map(parent -> ClassUtils.convertResourcePathToClassName(parent.thisClass().name().stringValue())).orElse(null);
    ClassDesc returnType = methodModel.methodTypeSymbol().returnType();
    String returnTypeName = returnType.packageName() + "." + returnType.displayName();
    Source source = new Source(declaringClassName, flags, methodName, methodModel.methodTypeSymbol());
    MergedAnnotations annotations = methodModel.elementStream()
            .filter(element -> element instanceof RuntimeVisibleAnnotationsAttribute)
            .findFirst()
            .map(element -> ClassFileAnnotationMetadata.createMergedAnnotations(methodName, (RuntimeVisibleAnnotationsAttribute) element, classLoader))
            .orElse(MergedAnnotations.valueOf(Collections.emptyList()));
    return new ClassFileMethodMetadata(methodName, flags, declaringClassName, returnTypeName, source, annotations);
  }

  /**
   * {@link MergedAnnotation} source.
   *
   * @param declaringClassName the name of the declaring class
   * @param flags the access flags
   * @param methodName the name of the method
   * @param descriptor the bytecode descriptor for this method
   */
  record Source(@Nullable String declaringClassName, AccessFlags flags, String methodName, MethodTypeDesc descriptor) {

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      this.flags.flags().forEach(flag -> {
        builder.append(flag.name().toLowerCase(Locale.ROOT));
        builder.append(' ');
      });
      builder.append(this.descriptor.returnType().packageName());
      builder.append(".");
      builder.append(this.descriptor.returnType().displayName());
      builder.append(' ');
      builder.append(this.declaringClassName);
      builder.append('.');
      builder.append(this.methodName);
      builder.append('(');
      builder.append(Stream.of(this.descriptor.parameterArray())
              .map(desc -> desc.packageName() + "." + desc.displayName())
              .collect(Collectors.joining(",")));
      builder.append(')');
      return builder.toString();
    }
  }

}
