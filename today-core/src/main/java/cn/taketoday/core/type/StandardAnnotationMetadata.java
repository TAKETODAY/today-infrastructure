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

package cn.taketoday.core.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.core.annotation.RepeatableContainers;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * {@link AnnotationMetadata} implementation that uses standard reflection
 * to introspect a given {@link Class}.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 */
public class StandardAnnotationMetadata extends StandardClassMetadata implements AnnotationMetadata {

  private final MergedAnnotations mergedAnnotations;

  private final boolean nestedAnnotationsAsMap;

  @Nullable
  private Set<String> annotationTypes;

  /**
   * Create a new {@code StandardAnnotationMetadata} wrapper for the given Class.
   *
   * @param introspectedClass the Class to introspect
   * @see #StandardAnnotationMetadata(Class, boolean)
   */
  public StandardAnnotationMetadata(Class<?> introspectedClass) {
    this(introspectedClass, false);
  }

  /**
   * Create a new {@link StandardAnnotationMetadata} wrapper for the given Class,
   * providing the option to return any nested annotations or annotation arrays in the
   * form of {@link cn.taketoday.core.annotation.AnnotationAttributes} instead
   * of actual {@link Annotation} instances.
   *
   * @param introspectedClass the Class to introspect
   * @param nestedAnnotationsAsMap return nested annotations and annotation arrays as
   * {@link cn.taketoday.core.annotation.AnnotationAttributes} for compatibility
   * with ASM-based {@link AnnotationMetadata} implementations
   */
  public StandardAnnotationMetadata(Class<?> introspectedClass, boolean nestedAnnotationsAsMap) {
    super(introspectedClass);
    this.mergedAnnotations = MergedAnnotations.from(
            introspectedClass, SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none());
    this.nestedAnnotationsAsMap = nestedAnnotationsAsMap;
  }

  @Override
  public MergedAnnotations getAnnotations() {
    return this.mergedAnnotations;
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
  @Nullable
  public Map<String, Object> getAnnotationAttributes(String annotationName, boolean classValuesAsString) {
    if (this.nestedAnnotationsAsMap) {
      return AnnotationMetadata.super.getAnnotationAttributes(annotationName, classValuesAsString);
    }
    return AnnotatedElementUtils.getMergedAnnotationAttributes(
            getIntrospectedClass(), annotationName, classValuesAsString, false);
  }

  @Override
  @Nullable
  public MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName, boolean classValuesAsString) {
    if (this.nestedAnnotationsAsMap) {
      return AnnotationMetadata.super.getAllAnnotationAttributes(annotationName, classValuesAsString);
    }
    return AnnotatedElementUtils.getAllAnnotationAttributes(
            getIntrospectedClass(), annotationName, classValuesAsString, false);
  }

  @Override
  public boolean hasAnnotatedMethods(String annotationName) {
    if (AnnotationUtils.isCandidateClass(getIntrospectedClass(), annotationName)) {
      try {
        Method[] methods = ReflectionUtils.getDeclaredMethods(getIntrospectedClass());
        for (Method method : methods) {
          if (isAnnotatedMethod(method, annotationName)) {
            return true;
          }
        }
      }
      catch (Throwable ex) {
        throw new IllegalStateException(
                "Failed to introspect annotated methods on " + getIntrospectedClass(), ex);
      }
    }
    return false;
  }

  @Override
  public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
    LinkedHashSet<MethodMetadata> annotatedMethods = null;
    if (AnnotationUtils.isCandidateClass(getIntrospectedClass(), annotationName)) {
      try {
        Method[] methods = ReflectionUtils.getDeclaredMethods(getIntrospectedClass());
        for (Method method : methods) {
          if (isAnnotatedMethod(method, annotationName)) {
            if (annotatedMethods == null) {
              annotatedMethods = new LinkedHashSet<>(4);
            }
            annotatedMethods.add(mapMethod(method));
          }
        }
      }
      catch (Throwable ex) {
        throw new IllegalStateException(
                "Failed to introspect annotated methods on " + getIntrospectedClass(), ex);
      }
    }
    return annotatedMethods != null ? annotatedMethods : Collections.emptySet();
  }

  @Override
  protected MethodMetadata mapMethod(Method method) {
    return new StandardMethodMetadata(method, this.nestedAnnotationsAsMap);
  }

  private static boolean isAnnotatedMethod(Method method, String annotationName) {
    return !method.isBridge() && method.getAnnotations().length > 0
            && AnnotatedElementUtils.isAnnotated(method, annotationName);
  }

  static AnnotationMetadata from(Class<?> introspectedClass) {
    return new StandardAnnotationMetadata(introspectedClass, true);
  }

}
