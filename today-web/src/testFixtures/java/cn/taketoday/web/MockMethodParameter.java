/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Objects;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author TODAY 2021/9/26 23:13
 */
@Setter
@Accessors(chain = false)
@SuppressWarnings("serial")
public class MockMethodParameter extends ResolvableMethodParameter {
  private MethodParameter parameter;

  private int parameterIndex;
  private Class<?> parameterClass;

  private String name;
  private boolean required;
  /** the default value */
  @Nullable
  private String defaultValue;

  protected TypeDescriptor typeDescriptor;

  private AnnotatedElement annotatedElement;

  public MockMethodParameter(ResolvableMethodParameter other) {
    super(other);
  }

  @Override
  public Object resolveParameter(RequestContext request) throws Throwable {
    return super.resolveParameter(request);
  }

  @Override
  public int getParameterIndex() {
    return parameterIndex;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isRequired() {
    return required;
  }

  @Override
  public Class<?> getParameterType() {
    return parameterClass;
  }

  @Nullable
  @Override
  public String getDefaultValue() {
    return defaultValue;
  }

  @Override
  public MethodParameter getParameter() {
    return parameter;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  @Override
  public Annotation[] getMethodAnnotations() {
    return parameter.getMethodAnnotations();
  }

  @Nullable
  @Override
  public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
    return parameter.getMethodAnnotation(annotationType);
  }

  @Override
  public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
    return parameter.hasMethodAnnotation(annotationType);
  }

  @Override
  public Annotation[] getParameterAnnotations() {
    return parameter.getParameterAnnotations();
  }

  @Override
  public boolean hasParameterAnnotation(Class<? extends Annotation> annotationType) {
    return parameter.hasParameterAnnotation(annotationType);
  }

  @Override
  public <A extends Annotation> A getParameterAnnotation(Class<A> annotationType) {
    return parameter.getParameterAnnotation(annotationType);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(super.hashCode(), parameterIndex,
            parameterClass, name, required, defaultValue, typeDescriptor, annotatedElement);
    result = 31 * result;
    return result;
  }

  @Override
  public String toString() {
    return super.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final MockMethodParameter parameter))
      return false;
    return parameterIndex == parameter.parameterIndex
            && required == parameter.required
            && Objects.equals(name, parameter.name)
            && Objects.equals(defaultValue, parameter.defaultValue)
            && Objects.equals(parameterClass, parameter.parameterClass)
            && Objects.equals(typeDescriptor, parameter.typeDescriptor)
            && Objects.equals(annotatedElement, parameter.annotatedElement);
  }
}
