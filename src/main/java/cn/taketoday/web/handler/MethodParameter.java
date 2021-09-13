/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.web.handler;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Objects;

import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.AnnotationSupport;
import cn.taketoday.core.AttributeAccessorSupport;
import cn.taketoday.core.Constant;
import cn.taketoday.core.Nullable;
import cn.taketoday.core.Required;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.NumberUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.util.TypeDescriptor;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestParam;

/**
 * Abstraction for Parameter
 *
 * @author TODAY
 * @see Parameter
 * @since 2.3.7
 */
public class MethodParameter
        extends AttributeAccessorSupport implements AnnotationSupport {

  private final int parameterIndex;
  private final Class<?> parameterClass;
  private final Parameter parameter; // reflect parameter instance

  private String name;
  private boolean required;
  /** the default value */
  @Nullable
  private String defaultValue;

  @Nullable
  private Type[] generics;

  @Nullable
  private HandlerMethod handlerMethod;
  /**
   * @since 3.0.1
   */
  protected TypeDescriptor typeDescriptor;

  public MethodParameter(@Nullable HandlerMethod handlerMethod, MethodParameter other) {
    this.name = other.name;
    this.generics = other.generics;
    this.required = other.required;
    this.parameter = other.parameter;
    this.defaultValue = other.defaultValue;
    this.parameterIndex = other.parameterIndex;
    this.parameterClass = other.parameterClass;

    this.handlerMethod = handlerMethod;
    this.typeDescriptor = other.typeDescriptor; // @since 3.0.1
  }

  /**
   * @since 4.0
   */
  public MethodParameter(MethodParameter other) {
    this.name = other.name;
    this.generics = other.generics;
    this.required = other.required;
    this.parameter = other.parameter;
    this.defaultValue = other.defaultValue;
    this.parameterIndex = other.parameterIndex;
    this.parameterClass = other.parameterClass;

    this.handlerMethod = other.handlerMethod;
    this.typeDescriptor = other.typeDescriptor; // @since 3.0.1
  }

  public MethodParameter(int index, Parameter parameter) {
    this.parameter = parameter;
    this.parameterIndex = index;
    this.parameterClass = parameter.getType();

    initRequestParam(parameter);
  }

  public MethodParameter(int index, Parameter parameter, String parameterName) {
    this(index, parameter);
    if (StringUtils.isEmpty(this.name)) {
      this.name = parameterName; // use method parameter name
    }
  }

  /**
   * @since 3.0
   */
  public MethodParameter(int index, Method method, String parameterName) {
    this(index, ReflectionUtils.getParameter(method, index), parameterName);
  }

  /**
   * init name, required, defaultValue
   *
   * @param element
   *         AnnotatedElement may annotated RequestParam
   *
   * @since 4.0
   */
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

  public boolean isArray() {
    return getParameterClass().isArray();
  }

  public boolean isCollection() {
    return CollectionUtils.isCollection(getParameterClass());
  }

  public boolean isInterface() {
    return getParameterClass().isInterface();
  }

  public boolean is(final Class<?> type) {
    return type == getParameterClass();
  }

  public boolean isAssignableTo(final Class<?> superClass) {
    return superClass.isAssignableFrom(getParameterClass());
  }

  public boolean isInstance(final Object obj) {
    return getParameterClass().isInstance(obj);
  }

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

  @Override
  public AnnotatedElement getAnnotationSource() {
    return parameter;
  }

  // ----- resolver

  /**
   * simple impl
   *
   * @param request
   *         Current request context
   *
   * @return parameter object
   */
  protected Object resolveParameter(final RequestContext request) throws Throwable {
    return request.getParameter(getName());
  }

  public int getParameterIndex() {
    return parameterIndex;
  }

  @Override
  public int hashCode() {
    return parameter.hashCode();
  }

  @Override
  public String toString() {
    return getParameterClass().getSimpleName() + " " + getName();
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj instanceof MethodParameter
            && Objects.equals(parameter, ((MethodParameter) obj).parameter)
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

  public Class<?> getParameterClass() {
    return parameterClass;
  }

  public Class<?> getComponentType() {
    return getParameterClass().getComponentType();
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
      generics = ClassUtils.getGenericTypes(parameter);
      if (generics == null) {
        generics = Constant.EMPTY_CLASS_ARRAY;
      }
      this.generics = generics;
    }
    return generics;
  }

  public Parameter getParameter() {
    return parameter;
  }

  @Nullable
  public HandlerMethod getHandlerMethod() {
    return handlerMethod;
  }

  public void setHandlerMethod(@Nullable HandlerMethod handlerMethod) {
    this.handlerMethod = handlerMethod;
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
    return TypeDescriptor.fromParameter(parameter);
  }

}
