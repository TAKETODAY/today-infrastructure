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
import java.lang.reflect.AnnotatedElement;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.lang.Assert;

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
