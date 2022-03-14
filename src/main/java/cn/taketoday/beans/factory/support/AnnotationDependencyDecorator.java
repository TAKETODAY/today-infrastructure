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

package cn.taketoday.beans.factory.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.util.CollectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/14 16:13
 */
public class AnnotationDependencyDecorator extends DependencyResolvingDecorator {

  private final ArrayList<Class<? extends Annotation>> supportedAnnotations = new ArrayList<>();

  @SafeVarargs
  public AnnotationDependencyDecorator(DependencyResolvingStrategy delegate, Class<? extends Annotation>... supportedAnnotations) {
    super(delegate);
    addAnnotationTypes(supportedAnnotations);
  }

  public AnnotationDependencyDecorator(DependencyResolvingStrategy delegate) {
    super(delegate);
  }

  public void addAnnotationTypes(Class<? extends Annotation>... supportedAnnotations) {
    CollectionUtils.addAll(this.supportedAnnotations, supportedAnnotations);
    this.supportedAnnotations.trimToSize();
  }

  public void setAnnotationTypes(Collection<Class<? extends Annotation>> supportedAnnotations) {
    this.supportedAnnotations.clear();
    CollectionUtils.addAll(this.supportedAnnotations, supportedAnnotations);
    this.supportedAnnotations.trimToSize();
  }

  @SafeVarargs
  public final void setAnnotationTypes(Class<? extends Annotation>... supportedAnnotations) {
    this.supportedAnnotations.clear();
    CollectionUtils.addAll(this.supportedAnnotations, supportedAnnotations);
    this.supportedAnnotations.trimToSize();
  }

  @Override
  public boolean supports(Field field) {
    return supportsInternal(field);
  }

  @Override
  public boolean supports(Executable method) {
    return supportsInternal(method);
  }

  private boolean supportsInternal(AccessibleObject accessible) {
    MergedAnnotations annotations = MergedAnnotations.from(accessible);
    for (Class<? extends Annotation> supportedAnnotation : supportedAnnotations) {
      if (annotations.isPresent(supportedAnnotation)) {
        return true;
      }
    }
    return false;
  }

}
