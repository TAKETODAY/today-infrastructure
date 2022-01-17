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

package cn.taketoday.beans.factory.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.util.CollectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/1 11:43
 */
public class AnnotationDependencyResolvingDecorator
        extends DependencyResolvingDecorator implements DependencyResolvingStrategy {

  private final Set<Class<? extends Annotation>> supportedAnnotations = new LinkedHashSet<>();

  @SafeVarargs
  public AnnotationDependencyResolvingDecorator(
          DependencyResolvingStrategy strategy, Class<? extends Annotation>... supportedAnnotations) {
    super(strategy);
    CollectionUtils.addAll(this.supportedAnnotations, supportedAnnotations);
  }

  @Override
  public boolean supports(Field field) {
    return supportsInternal(field);
  }

  @Override
  public boolean supports(Executable executable) {
    return supportsInternal(executable);
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
