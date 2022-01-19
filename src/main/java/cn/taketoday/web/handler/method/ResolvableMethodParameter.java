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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.handler.method;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Objects;

import cn.taketoday.core.AttributeAccessorSupport;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Required;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.NumberUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestParam;

/**
 * Abstraction for Parameter
 * <p>
 * this Class supports resolving its parameter in a {@link RequestContext}
 * </p>
 * <p>
 * provides some common information like 'required','defaultValue', see more
 * {@link RequestParam}
 * </p>
 *
 * @author TODAY
 * @see MethodParameter
 * @see #resolveParameter(RequestContext)
 * @since 2.3.7
 */
public class ResolvableMethodParameter extends AttributeAccessorSupport {

  private String name;
  private boolean required;
  /** the default value */
  @Nullable
  private String defaultValue;

  @Nullable
  private Type[] generics;

  /**
   * @since 3.0.1
   */
  protected TypeDescriptor typeDescriptor;

  // @since 4.0
  private final MethodParameter parameter;

  @Nullable
  private NamedValueInfo namedValueInfo;

  /**
   * @since 4.0
   */
  public ResolvableMethodParameter(ResolvableMethodParameter other) {
    this.name = other.name;
    this.generics = other.generics;
    this.required = other.required;
    this.parameter = other.parameter;
    this.defaultValue = other.defaultValue;

    this.typeDescriptor = other.typeDescriptor; // @since 3.0.1
  }

  public ResolvableMethodParameter(MethodParameter parameter, String name) {
    this(parameter);
    if (StringUtils.isEmpty(this.name)) {
      this.name = name; // use method parameter name
    }
  }

  public ResolvableMethodParameter(MethodParameter parameter) {
    initRequestParam(parameter);
    this.parameter = parameter;
  }

  /**
   * init name, required, defaultValue
   *
   * @since 4.0
   */
  protected void initRequestParam(MethodParameter parameter) {
    MergedAnnotations annotations = MergedAnnotations.from(parameter.getParameterAnnotations());
    MergedAnnotation<RequestParam> requestParam = annotations.get(RequestParam.class);
    if (requestParam.isPresent()) {
      this.name = requestParam.getStringValue();
      this.required = requestParam.getBoolean("required");
      this.defaultValue = requestParam.getString("defaultValue");
    }
    if (!this.required) { // @since 3.0 Required
      this.required = annotations.isPresent(Required.class);
    }
    if (StringUtils.isEmpty(defaultValue) && NumberUtils.isNumber(parameter.getParameterType())) {
      this.defaultValue = "0"; // fix default value
    }
  }

  public boolean isArray() {
    return getParameterType().isArray();
  }

  public boolean isCollection() {
    return CollectionUtils.isCollection(getParameterType());
  }

  public boolean isInterface() {
    return getParameterType().isInterface();
  }

  public boolean is(final Class<?> type) {
    return type == getParameterType();
  }

  public boolean isAssignableTo(final Class<?> superClass) {
    return superClass.isAssignableFrom(getParameterType());
  }

  public boolean isInstance(final Object obj) {
    return getParameterType().isInstance(obj);
  }

  @Nullable
  public Type getGeneric(final int index) {
    final Type[] generics = getGenerics();
    if (generics != null && generics.length > index) {
      return generics[index];
    }
    return null;
  }

  public boolean isGenericPresent(final Type requiredType, final int index) {
    return requiredType.equals(getGeneric(index));
  }

  public boolean isGenericPresent(final Type requiredType) {
    final Type[] generics = getGenerics();
    if (generics != null) {
      for (final Type type : generics) {
        if (type.equals(requiredType)) {
          return true;
        }
      }
    }
    return false;
  }

  // AnnotatedElement @since 3.0

  public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
    return parameter.hasParameterAnnotation(annotationClass);
  }

  public <A extends Annotation> A getAnnotation(final Class<A> annotationClass) {
    return parameter.getParameterAnnotation(annotationClass);
  }

  public void setNamedValueInfo(@Nullable NamedValueInfo namedValueInfo) {
    this.namedValueInfo = namedValueInfo;
  }

  @Nullable
  public NamedValueInfo getNamedValueInfo() {
    return namedValueInfo;
  }

  // ----- resolver

  /**
   * simple impl
   *
   * @param request Current request context
   * @return parameter object
   */
  protected Object resolveParameter(final RequestContext request) throws Throwable {
    return request.getParameter(getName());
  }

  public int getParameterIndex() {
    return parameter.getParameterIndex();
  }

  @Override
  public int hashCode() {
    return parameter.hashCode();
  }

  @Override
  public String toString() {
    return getParameterType().getSimpleName() + " " + getName();
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj instanceof ResolvableMethodParameter
            && Objects.equals(parameter, ((ResolvableMethodParameter) obj).parameter)
    );
  }

  // Getter Setter

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public boolean isRequired() {
    return required;
  }

  public Class<?> getParameterType() {
    return parameter.getParameterType();
  }

  public Class<?> getComponentType() {
    return getParameterType().getComponentType();
  }

  public void setDefaultValue(@Nullable String defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Nullable
  public String getDefaultValue() {
    return defaultValue;
  }

  public void setGenerics(@Nullable Type[] generics) {
    this.generics = generics;
  }

  @Nullable
  public Type[] getGenerics() {
    Type[] generics = this.generics;
    if (generics == null) {
      generics = ClassUtils.getGenericTypes(parameter.getParameter());
      if (generics == null) {
        generics = Constant.EMPTY_CLASS_ARRAY;
      }
      this.generics = generics;
    }
    return generics;
  }

  public MethodParameter getParameter() {
    return parameter;
  }

  //

  /**
   * @since 3.0.1
   */
  public TypeDescriptor getTypeDescriptor() {
    TypeDescriptor typeDescriptor = this.typeDescriptor;
    if (typeDescriptor == null) {
      typeDescriptor = createTypeDescriptor();
      this.typeDescriptor = typeDescriptor;
    }
    return typeDescriptor;
  }

  /**
   * @since 4.0
   */
  protected TypeDescriptor createTypeDescriptor() {
    return new TypeDescriptor(parameter);
  }

}
