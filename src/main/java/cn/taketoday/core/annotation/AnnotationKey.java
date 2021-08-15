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

package cn.taketoday.core.annotation;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.util.Objects;

import cn.taketoday.core.Assert;

/**
 * @author TODAY 2021/8/15 22:40
 * @since 4.0
 */
public final class AnnotationKey<T> implements Serializable {
  private static final long serialVersionUID = 1L;

  private final int hash;
  final Class<T> annotationClass;
  final AnnotatedElement element;

  public AnnotationKey(AnnotatedElement element, Class<T> annotationClass) {
    Assert.notNull(element, "AnnotatedElement can't be null");
    this.element = element;
    this.annotationClass = annotationClass;
    this.hash = Objects.hash(element, annotationClass);
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof AnnotationKey) {
      final AnnotationKey<?> other = (AnnotationKey<?>) obj;
      return Objects.equals(element, other.element) //
              && Objects.equals(annotationClass, other.annotationClass);
    }
    return false;
  }
}
