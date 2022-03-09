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

import cn.taketoday.core.OrderedSupport;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.Order;

/**
 * Supports order
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Order
 * @see cn.taketoday.core.Ordered
 * @since 4.0 2022/1/1 11:56
 */
public abstract class AnnotationDependencyResolvingStrategy
        extends OrderedSupport implements DependencyResolvingStrategy {

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
    Class<? extends Annotation>[] supportedAnnotations = getSupportedAnnotations();
    for (Class<? extends Annotation> supportedAnnotation : supportedAnnotations) {
      if (annotations.isPresent(supportedAnnotation)) {
        return true;
      }
    }
    return false;
  }

  protected abstract Class<? extends Annotation>[] getSupportedAnnotations();

}
