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

package cn.taketoday.util;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;

import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;

/**
 * Adapter class for exposing annotations as an {@link AnnotatedElement}
 *
 * @author TODAY 2021/3/26 13:42
 * @see AnnotationUtils#isPresent(AnnotatedElement, Class)
 * @see AnnotationUtils#getAnnotation(AnnotatedElement, Class)
 */
public class AnnotationsAnnotatedElementAdapter implements AnnotatedElement, Serializable {

  @Nullable
  private final Annotation[] annotations;

  public AnnotationsAnnotatedElementAdapter(@Nullable Annotation[] annotations) {
    this.annotations = annotations;
  }

  @Override
  public boolean isAnnotationPresent(@NonNull Class<? extends Annotation> annotationClass) {
    for (Annotation annotation : getAnnotations()) {
      if (annotation.annotationType() == annotationClass) {
        return true;
      }
    }
    return false;
  }

  @Override
  @Nullable
  @SuppressWarnings("unchecked")
  public <T extends Annotation> T getAnnotation(@NonNull Class<T> annotationClass) {
    for (Annotation annotation : getAnnotations()) {
      if (annotation.annotationType() == annotationClass) {
        return (T) annotation;
      }
    }
    return null;
  }

  @Override
  public Annotation[] getAnnotations() {
    return this.annotations != null ? this.annotations.clone() : Constant.EMPTY_ANNOTATIONS;
  }

  @Override
  public Annotation[] getDeclaredAnnotations() {
    return getAnnotations();
  }

  public boolean isEmpty() {
    return ObjectUtils.isEmpty(this.annotations);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof AnnotationsAnnotatedElementAdapter &&
            Arrays.equals(this.annotations, ((AnnotationsAnnotatedElementAdapter) other).annotations)));
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(this.annotations);
  }

}
