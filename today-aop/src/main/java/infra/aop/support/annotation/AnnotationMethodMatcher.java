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
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import infra.aop.support.AopUtils;
import infra.aop.support.StaticMethodMatcher;
import infra.core.annotation.AnnotatedElementUtils;
import infra.lang.Assert;

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
    Assert.notNull(annotationType, "Annotation type is required");
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
