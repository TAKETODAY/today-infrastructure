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
import java.lang.reflect.Parameter;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.annotation.MergedAnnotations;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/20 16:11</a>
 * @since 4.0
 */
public class ParameterInjectionPoint extends DependencyInjectionPoint {
  private final Parameter parameter;

  public ParameterInjectionPoint(Parameter parameter) {
    this.parameter = parameter;
  }

  @Override
  public Class<?> getDependencyType() {
    return parameter.getType();
  }

  @Override
  public AnnotatedElement getAnnotatedElement() {
    return parameter;
  }

  @Override
  protected MergedAnnotations doGetAnnotations() {
    return MergedAnnotations.from(parameter);
  }

  @Override
  protected ResolvableType doGetResolvableType() {
    return ResolvableType.fromParameter(parameter);
  }

  @Override
  protected TypeDescriptor doGetTypeDescriptor() {
    return TypeDescriptor.fromParameter(parameter);
  }

  @Override
  public Object getTarget() {
    return parameter;
  }

  @Override
  public String toString() {
    return "ParameterInjectionPoint{" +
            "parameter=" + parameter +
            '}';
  }
}
