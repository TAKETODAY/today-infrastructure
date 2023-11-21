/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.aop.support.annotation;

import java.lang.annotation.Annotation;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.MethodMatcher;
import cn.taketoday.aop.Pointcut;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Assert;

/**
 * Simple Pointcut that looks for a specific Java 5 annotation
 * being present on a {@link #forClassAnnotation class} or
 * {@link #forMethodAnnotation method}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author TODAY 2021/2/2 13:00
 * @see AnnotationClassFilter
 * @see AnnotationMethodMatcher
 * @since 3.0
 */
public class AnnotationMatchingPointcut implements Pointcut {

  private final ClassFilter classFilter;
  private final MethodMatcher methodMatcher;

  /**
   * Create a new AnnotationMatchingPointcut for the given annotation type.
   *
   * @param classAnnotationType the annotation type to look for at the class level
   */
  public AnnotationMatchingPointcut(Class<? extends Annotation> classAnnotationType) {
    this(classAnnotationType, false);
  }

  /**
   * Create a new AnnotationMatchingPointcut for the given annotation type.
   *
   * @param classAnnotationType the annotation type to look for at the class level
   * @param checkInherited whether to also check the superclasses and interfaces
   * as well as meta-annotations for the annotation type
   * @see AnnotationClassFilter#AnnotationClassFilter(Class, boolean)
   */
  public AnnotationMatchingPointcut(Class<? extends Annotation> classAnnotationType, boolean checkInherited) {
    this.classFilter = new AnnotationClassFilter(classAnnotationType, checkInherited);
    this.methodMatcher = MethodMatcher.TRUE;
  }

  /**
   * Create a new AnnotationMatchingPointcut for the given annotation types.
   *
   * @param classAnnotationType the annotation type to look for at the class level
   * (can be {@code null})
   * @param methodAnnotationType the annotation type to look for at the method level
   * (can be {@code null})
   */
  public AnnotationMatchingPointcut(
          Class<? extends Annotation> classAnnotationType, Class<? extends Annotation> methodAnnotationType) {
    this(classAnnotationType, methodAnnotationType, false);
  }

  /**
   * Create a new AnnotationMatchingPointcut for the given annotation types.
   *
   * @param classAnnotationType the annotation type to look for at the class level
   * (can be {@code null})
   * @param methodAnnotationType the annotation type to look for at the method level
   * (can be {@code null})
   * @param checkInherited whether to also check the superclasses and interfaces
   * as well as meta-annotations for the annotation type
   * @see AnnotationClassFilter#AnnotationClassFilter(Class, boolean)
   * @see AnnotationMethodMatcher#AnnotationMethodMatcher(Class, boolean)
   */
  public AnnotationMatchingPointcut(
          Class<? extends Annotation> classAnnotationType,
          Class<? extends Annotation> methodAnnotationType, boolean checkInherited) {

    Assert.isTrue((classAnnotationType != null || methodAnnotationType != null),
            "Either Class annotation type or Method annotation type needs to be specified (or both)");

    if (classAnnotationType != null) {
      this.classFilter = new AnnotationClassFilter(classAnnotationType, checkInherited);
    }
    else {
      this.classFilter = new AnnotationCandidateClassFilter(methodAnnotationType);
    }

    if (methodAnnotationType != null) {
      this.methodMatcher = new AnnotationMethodMatcher(methodAnnotationType, checkInherited);
    }
    else {
      this.methodMatcher = MethodMatcher.TRUE;
    }
  }

  @Override
  public ClassFilter getClassFilter() {
    return this.classFilter;
  }

  @Override
  public MethodMatcher getMethodMatcher() {
    return this.methodMatcher;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof AnnotationMatchingPointcut otherPointcut)) {
      return false;
    }
    return classFilter.equals(otherPointcut.classFilter)
            && methodMatcher.equals(otherPointcut.methodMatcher);
  }

  @Override
  public int hashCode() {
    return this.classFilter.hashCode() * 37 + this.methodMatcher.hashCode();
  }

  @Override
  public String toString() {
    return "AnnotationMatchingPointcut: " + this.classFilter + ", " + this.methodMatcher;
  }

  /**
   * Factory method for an AnnotationMatchingPointcut that matches
   * for the specified annotation at the class level.
   *
   * @param annotationType the annotation type to look for at the class level
   * @return the corresponding AnnotationMatchingPointcut
   */
  public static AnnotationMatchingPointcut forClassAnnotation(Class<? extends Annotation> annotationType) {
    Assert.notNull(annotationType, "Annotation type is required");
    return new AnnotationMatchingPointcut(annotationType);
  }

  /**
   * Factory method for an AnnotationMatchingPointcut that matches
   * for the specified annotation at the method level.
   *
   * @param annotationType the annotation type to look for at the method level
   * @return the corresponding AnnotationMatchingPointcut
   */
  public static AnnotationMatchingPointcut forMethodAnnotation(Class<? extends Annotation> annotationType) {
    Assert.notNull(annotationType, "Annotation type is required");
    return new AnnotationMatchingPointcut(null, annotationType);
  }

  /**
   * {@link ClassFilter} that delegates to {@link AnnotationUtils#isCandidateClass}
   * for filtering classes whose methods are not worth searching to begin with.
   */
  private record AnnotationCandidateClassFilter(Class<? extends Annotation> annotationType)
          implements ClassFilter {

    @Override
    public boolean matches(Class<?> clazz) {
      return AnnotationUtils.isCandidateClass(clazz, this.annotationType);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof AnnotationCandidateClassFilter that)) {
        return false;
      }
      return this.annotationType.equals(that.annotationType);
    }

    @Override
    public int hashCode() {
      return this.annotationType.hashCode();
    }

    @Override
    public String toString() {
      return getClass().getName() + ": " + this.annotationType;
    }

  }

}
