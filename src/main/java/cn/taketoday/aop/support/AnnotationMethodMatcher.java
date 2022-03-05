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

package cn.taketoday.aop.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.lang.Assert;

/**
 * Simple MethodMatcher that looks for a specific Java 5 annotation
 * being present on a method (checking both the method on the invoked
 * interface, if any, and the corresponding method on the target class).
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author TODAY 2021/2/2 13:00
 * @see AnnotationMatchingPointcut
 * @since 3.0
 */
public class AnnotationMethodMatcher extends StaticMethodMatcher {

  private final Class<? extends Annotation> annotationType;

  private final boolean checkInherited;

  /**
   * Create a new AnnotationClassFilter for the given annotation type.
   *
   * @param annotationType the annotation type to look for
   */
  public AnnotationMethodMatcher(Class<? extends Annotation> annotationType) {
    this(annotationType, false);
  }

  /**
   * Create a new AnnotationClassFilter for the given annotation type.
   *
   * @param annotationType the annotation type to look for
   * @param checkInherited whether to also check the superclasses and
   * interfaces as well as meta-annotations for the annotation type
   * (i.e. whether to use {@link AnnotatedElementUtils#hasAnnotation(AnnotatedElement, Class)}
   * semantics instead of standard Java {@link Method#isAnnotationPresent})
   */
  public AnnotationMethodMatcher(Class<? extends Annotation> annotationType, boolean checkInherited) {
    Assert.notNull(annotationType, "Annotation type must not be null");
    this.annotationType = annotationType;
    this.checkInherited = checkInherited;
  }

  @Override
  public boolean matches(Method method, Class<?> targetClass) {
    if (matchesMethod(method)) {
      return true;
    }
    // Proxy classes never have annotations on their redeclared methods.
    if (Proxy.isProxyClass(targetClass)) {
      return false;
    }
    // The method may be on an interface, so let's check on the target class as well.
    Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
    return specificMethod != method && matchesMethod(specificMethod);
  }

  private boolean matchesMethod(Method method) {
    return this.checkInherited
           ? AnnotatedElementUtils.hasAnnotation(method, annotationType)
           : method.isAnnotationPresent(annotationType);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof AnnotationMethodMatcher otherMm)) {
      return false;
    }
    return checkInherited == otherMm.checkInherited
            && annotationType.equals(otherMm.annotationType);
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
