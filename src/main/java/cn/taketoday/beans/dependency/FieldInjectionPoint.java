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

package cn.taketoday.beans.dependency;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.annotation.MergedAnnotations;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/20 15:27</a>
 * @since 4.0
 */
public class FieldInjectionPoint extends InjectionPoint {

  private final Field property;

  public FieldInjectionPoint(Field property) {
    this.property = property;
  }

  @Override
  public Class<?> getDependencyType() {
    return property.getType();
  }

  @Override
  public AnnotatedElement getAnnotatedElement() {
    return property;
  }

  @Override
  protected MergedAnnotations doGetAnnotations() {
    return MergedAnnotations.from(property);
  }

  @Override
  protected ResolvableType doGetResolvableType() {
    return ResolvableType.fromField(property);
  }

  @Override
  protected TypeDescriptor doGetTypeDescriptor() {
    return TypeDescriptor.fromField(property);
  }

  @Override
  public Object getTarget() {
    return property;
  }

  @Override
  public boolean isProperty() {
    return true;
  }

  @Override
  public String toString() {
    return "FieldInjectionPoint{" +
            "property=" + property +
            '}';
  }
}
