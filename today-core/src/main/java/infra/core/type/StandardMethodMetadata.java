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

package infra.core.type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import infra.core.annotation.AnnotatedElementUtils;
import infra.core.annotation.AnnotationAttributes;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.RepeatableContainers;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.MultiValueMap;

/**
 * {@link MethodMetadata} implementation that uses standard reflection
 * to introspect a given {@code Method}.
 *
 * @author Juergen Hoeller
 * @author Mark Pollack
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0
 */
public class StandardMethodMetadata implements MethodMetadata {

  private final Method introspectedMethod;

  private final boolean nestedAnnotationsAsMap;

  private final MergedAnnotations mergedAnnotations;

  /**
   * Create a new StandardMethodMetadata wrapper for the given Method.
   *
   * @param introspectedMethod the Method to introspect
   */
  public StandardMethodMetadata(Method introspectedMethod) {
    this(introspectedMethod, false);
  }

  /**
   * Create a new StandardMethodMetadata wrapper for the given Method,
   * providing the option to return any nested annotations or annotation arrays in the
   * form of {@link AnnotationAttributes} instead
   * of actual {@link java.lang.annotation.Annotation} instances.
   *
   * @param introspectedMethod the Method to introspect
   * @param nestedAnnotationsAsMap return nested annotations and annotation arrays as
   * {@link AnnotationAttributes} for compatibility
   * with ASM-based {@link AnnotationMetadata} implementations
   */
  public StandardMethodMetadata(Method introspectedMethod, boolean nestedAnnotationsAsMap) {
    Assert.notNull(introspectedMethod, "Method is required");
    this.introspectedMethod = introspectedMethod;
    this.nestedAnnotationsAsMap = nestedAnnotationsAsMap;
    this.mergedAnnotations = MergedAnnotations.from(introspectedMethod, MergedAnnotations.SearchStrategy.DIRECT, RepeatableContainers.NONE);
  }

  @Override
  public MergedAnnotations getAnnotations() {
    return this.mergedAnnotations;
  }

  /**
   * Return the underlying Method.
   */
  public final Method getIntrospectedMethod() {
    return this.introspectedMethod;
  }

  @Override
  public String getMethodName() {
    return this.introspectedMethod.getName();
  }

  @Override
  public String getDeclaringClassName() {
    return this.introspectedMethod.getDeclaringClass().getName();
  }

  @Override
  public String getReturnTypeName() {
    return this.introspectedMethod.getReturnType().getName();
  }

  @Override
  public boolean isAbstract() {
    return Modifier.isAbstract(this.introspectedMethod.getModifiers());
  }

  @Override
  public boolean isStatic() {
    return Modifier.isStatic(this.introspectedMethod.getModifiers());
  }

  @Override
  public boolean isFinal() {
    return Modifier.isFinal(this.introspectedMethod.getModifiers());
  }

  @Override
  public boolean isOverridable() {
    return !isStatic() && !isFinal() && !isPrivate();
  }

  private boolean isPrivate() {
    return Modifier.isPrivate(this.introspectedMethod.getModifiers());
  }

  @Override
  @Nullable
  public Map<String, Object> getAnnotationAttributes(String annotationName, boolean classValuesAsString) {
    if (this.nestedAnnotationsAsMap) {
      return MethodMetadata.super.getAnnotationAttributes(annotationName, classValuesAsString);
    }
    return AnnotatedElementUtils.getMergedAnnotationAttributes(
            this.introspectedMethod, annotationName, classValuesAsString, false);
  }

  @Override
  @Nullable
  public MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName, boolean classValuesAsString) {
    if (this.nestedAnnotationsAsMap) {
      return MethodMetadata.super.getAllAnnotationAttributes(annotationName, classValuesAsString);
    }
    return AnnotatedElementUtils.getAllAnnotationAttributes(
            this.introspectedMethod, annotationName, classValuesAsString, false);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return ((this == obj) || ((obj instanceof StandardMethodMetadata) &&
            this.introspectedMethod.equals(((StandardMethodMetadata) obj).introspectedMethod)));
  }

  @Override
  public int hashCode() {
    return this.introspectedMethod.hashCode();
  }

  @Override
  public String toString() {
    return this.introspectedMethod.toString();
  }

}
