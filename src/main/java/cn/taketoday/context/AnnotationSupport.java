/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import cn.taketoday.context.utils.ClassUtils;

/**
 * @author TODAY 2021/3/20 22:59
 * @since 3.0
 */
public interface AnnotationSupport extends AnnotatedElement {

  AnnotatedElement getAnnotationSource();

  @Override
  default boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
    return ClassUtils.isAnnotationPresent(getAnnotationSource(), annotationClass);
  }

  @Override
  default <A extends Annotation> A getAnnotation(final Class<A> annotationClass) {
    return ClassUtils.getAnnotation(annotationClass, getAnnotationSource());
  }

  // AnnotatedElement @since 3.0

  @Override
  default Annotation[] getAnnotations() {
    return getAnnotationSource().getAnnotations();
  }

  @Override
  default Annotation[] getDeclaredAnnotations() {
    return getAnnotationSource().getDeclaredAnnotations();
  }

  @Override
  default <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
    return getAnnotationSource().getDeclaredAnnotation(annotationClass);
  }

  @Override
  default <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
    return getAnnotationSource().getAnnotationsByType(annotationClass);
  }

  @Override
  default <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
    return getAnnotationSource().getDeclaredAnnotationsByType(annotationClass);
  }
}
