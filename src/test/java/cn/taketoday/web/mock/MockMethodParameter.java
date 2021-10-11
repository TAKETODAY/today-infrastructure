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

package cn.taketoday.web.mock;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Required;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.util.NumberUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.handler.MethodParameter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author TODAY 2021/9/26 23:13
 */
@Setter
@Accessors(chain = false)
public class MockMethodParameter extends MethodParameter {
  private Parameter parameter;

  private int parameterIndex;
  private Class<?> parameterClass;

  private String name;
  private boolean required;
  /** the default value */
  @Nullable
  private String defaultValue;

  @Nullable
  private Type[] generics;

  @Nullable
  private HandlerMethod handlerMethod;

  protected TypeDescriptor typeDescriptor;

  private AnnotatedElement annotatedElement;

  public MockMethodParameter(@Nullable HandlerMethod handlerMethod, MethodParameter other) {
    super(handlerMethod, other);
  }

  public MockMethodParameter(MethodParameter other) {
    super(other);
  }

  public MockMethodParameter(int index, Parameter parameter) {
    super(index, parameter);
  }

  public MockMethodParameter(int index, Parameter parameter, String parameterName) {
    super(index, parameter, parameterName);
  }

  public MockMethodParameter(int index, Method method, String parameterName) {
    super(index, method, parameterName);
  }

  @Override
  protected void initRequestParam(AnnotatedElement element) {
    AnnotationAttributes attributes = AnnotationUtils.getAttributes(RequestParam.class, element);
    if (attributes != null) {
      this.name = attributes.getString(Constant.VALUE);
      this.required = attributes.getBoolean("required");
      this.defaultValue = attributes.getString("defaultValue");
    }
    if (!this.required) { // @since 3.0 Required
      this.required = AnnotationUtils.isPresent(element, Required.class);
    }
    if (StringUtils.isEmpty(defaultValue) && NumberUtils.isNumber(parameterClass)) {
      this.defaultValue = "0"; // fix default value
    }
  }

  @Override
  public AnnotatedElement getAnnotationSource() {
    return annotatedElement;
  }

  @Override
  protected Object resolveParameter(RequestContext request) throws Throwable {
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
  public Class<?> getParameterClass() {
    return parameterClass;
  }

  @Nullable
  @Override
  public String getDefaultValue() {
    return defaultValue;
  }

  @Nullable
  @Override
  public Type[] getGenerics() {
    return generics;
  }

  @Override
  public Parameter getParameter() {
    return parameter;
  }

  @Nullable
  @Override
  public HandlerMethod getHandlerMethod() {
    return handlerMethod;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(super.hashCode(), parameterIndex, parameterClass, name, required, defaultValue, handlerMethod, typeDescriptor, annotatedElement);
    result = 31 * result + Arrays.hashCode(generics);
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
    if (!(o instanceof MockMethodParameter))
      return false;
    final MockMethodParameter parameter = (MockMethodParameter) o;
    return parameterIndex == parameter.parameterIndex
            && required == parameter.required
            && Objects.equals(name, parameter.name)
            && Arrays.equals(generics, parameter.generics)
            && Objects.equals(defaultValue, parameter.defaultValue)
            && Objects.equals(handlerMethod, parameter.handlerMethod)
            && Objects.equals(parameterClass, parameter.parameterClass)
            && Objects.equals(typeDescriptor, parameter.typeDescriptor)
            && Objects.equals(annotatedElement, parameter.annotatedElement);
  }
}
