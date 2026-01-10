/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Objects;

import infra.core.MethodParameter;
import infra.core.TypeDescriptor;
import org.jspecify.annotations.Nullable;
import infra.web.handler.method.ResolvableMethodParameter;

/**
 * @author TODAY 2021/9/26 23:13
 */
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

  public void setTypeDescriptor(TypeDescriptor typeDescriptor) {
    this.typeDescriptor = typeDescriptor;
  }

  public void setAnnotatedElement(AnnotatedElement annotatedElement) {
    this.annotatedElement = annotatedElement;
  }

  public void setDefaultValue(@Nullable String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setParameter(MethodParameter parameter) {
    this.parameter = parameter;
  }

  public void setParameterClass(Class<?> parameterClass) {
    this.parameterClass = parameterClass;
  }

  public void setParameterIndex(int parameterIndex) {
    this.parameterIndex = parameterIndex;
  }

  public void setRequired(boolean required) {
    this.required = required;
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
