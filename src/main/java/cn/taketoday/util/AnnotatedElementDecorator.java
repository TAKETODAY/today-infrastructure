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

import cn.taketoday.lang.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 * @author TODAY 2021/11/9 15:49
 * @since 4.0
 */
public class AnnotatedElementDecorator implements AnnotatedElement {
  private final AnnotatedElement delegate;

  public AnnotatedElementDecorator(AnnotatedElement delegate) {
    this.delegate = delegate;
  }

  @Override
  public <T extends Annotation> T getAnnotation(@NonNull Class<T> annotationClass) {
    return delegate.getAnnotation(annotationClass);
  }

  @Override
  public Annotation[] getAnnotations() {
    return delegate.getAnnotations();
  }

  @Override
  public Annotation[] getDeclaredAnnotations() {
    return delegate.getDeclaredAnnotations();
  }

  @Override
  public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
    return delegate.getDeclaredAnnotation(annotationClass);
  }

  @Override
  public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
    return delegate.getAnnotationsByType(annotationClass);
  }

  @Override
  public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
    return delegate.getDeclaredAnnotationsByType(annotationClass);
  }

  @Override
  public boolean isAnnotationPresent(@NonNull Class<? extends Annotation> annotationClass) {
    return delegate.isAnnotationPresent(annotationClass);
  }

  public AnnotatedElement getDelegate() {
    return delegate;
  }

}
