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

package cn.taketoday.classify.util;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * MethodResolver implementation that finds a <em>single</em> Method on the given Class
 * that contains the specified annotation type.
 *
 * @author Mark Fisher
 */
public class AnnotationMethodResolver implements MethodResolver {

  private Class<? extends Annotation> annotationType;

  /**
   * Create a MethodResolver for the specified Method-level annotation type
   *
   * @param annotationType the type of the annotation
   */
  public AnnotationMethodResolver(Class<? extends Annotation> annotationType) {
    Assert.notNull(annotationType, "annotationType must not be null");
    Assert.isTrue(
            ObjectUtils.containsElement(annotationType.getAnnotation(Target.class).value(), ElementType.METHOD),
            "Annotation [" + annotationType + "] is not a Method-level annotation.");
    this.annotationType = annotationType;
  }

  /**
   * Find a <em>single</em> Method on the Class of the given candidate object that
   * contains the annotation type for which this resolver is searching.
   *
   * @param candidate the instance whose Class will be checked for the annotation
   * @return a single matching Method instance or <code>null</code> if the candidate's
   * Class contains no Methods with the specified annotation
   * @throws IllegalArgumentException if more than one Method has the specified
   * annotation
   */
  public Method findMethod(Object candidate) {
    Assert.notNull(candidate, "candidate object must not be null");
    Class<?> targetClass = AopUtils.getTargetClass(candidate);
    if (targetClass == null) {
      targetClass = candidate.getClass();
    }
    return this.findMethod(targetClass);
  }

  /**
   * Find a <em>single</em> Method on the given Class that contains the annotation type
   * for which this resolver is searching.
   *
   * @param clazz the Class instance to check for the annotation
   * @return a single matching Method instance or <code>null</code> if the Class
   * contains no Methods with the specified annotation
   * @throws IllegalArgumentException if more than one Method has the specified
   * annotation
   */
  public Method findMethod(final Class<?> clazz) {
    Assert.notNull(clazz, "class must not be null");
    final AtomicReference<Method> annotatedMethod = new AtomicReference<Method>();
    ReflectionUtils.doWithMethods(clazz, new ReflectionUtils.MethodCallback() {
      public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
        Annotation annotation = AnnotationUtils.findAnnotation(method, annotationType);
        if (annotation != null) {
          Assert.isNull(annotatedMethod.get(), "found more than one method on target class [" + clazz
                  + "] with the annotation type [" + annotationType + "]");
          annotatedMethod.set(method);
        }
      }
    });
    return annotatedMethod.get();
  }

}
