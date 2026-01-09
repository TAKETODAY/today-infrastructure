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

package infra.aop.support.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import infra.aop.ClassFilter;
import infra.core.annotation.AnnotatedElementUtils;
import infra.lang.Assert;

/**
 * Simple ClassFilter that looks for a specific Java 5 annotation
 * being present on a class.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/2/2 13:00
 * @see AnnotationMatchingPointcut
 * @since 3.0
 */
public class AnnotationClassFilter implements ClassFilter {

  private final boolean checkInherited;
  private final Class<? extends Annotation> annotationType;

  /**
   * Create a new AnnotationClassFilter for the given annotation type.
   *
   * @param annotationType the annotation type to look for
   */
  public AnnotationClassFilter(Class<? extends Annotation> annotationType) {
    this(annotationType, false);
  }

  /**
   * Create a new AnnotationClassFilter for the given annotation type.
   *
   * @param annotationType the annotation type to look for
   * @param checkInherited whether to also check the superclasses and
   * interfaces as well as meta-annotations for the annotation type
   * (i.e. whether to use {@link AnnotatedElementUtils#hasAnnotation(AnnotatedElement, Class)}
   * semantics instead of standard Java {@link Class#isAnnotationPresent})
   */
  public AnnotationClassFilter(Class<? extends Annotation> annotationType, boolean checkInherited) {
    Assert.notNull(annotationType, "Annotation type is required");
    this.annotationType = annotationType;
    this.checkInherited = checkInherited;
  }

  @Override
  public boolean matches(Class<?> clazz) {
    return checkInherited
           ? AnnotatedElementUtils.hasAnnotation(clazz, this.annotationType)
           : clazz.isAnnotationPresent(this.annotationType);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof AnnotationClassFilter otherCf)) {
      return false;
    }
    return checkInherited == otherCf.checkInherited
            && annotationType.equals(otherCf.annotationType);
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
