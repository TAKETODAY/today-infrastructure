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

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.AnnotationSupport;
import cn.taketoday.context.AttributeAccessorSupport;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.NumberUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestParam;

import static cn.taketoday.context.utils.ClassUtils.getAnnotationAttributes;

/**
 * @author TODAY
 * @version 2.3.7 <br>
 */
public class MethodParameter
        extends AttributeAccessorSupport implements AnnotationSupport {

  private final int parameterIndex;
  private final Class<?> parameterClass;
  private final Parameter parameter; // reflect parameter instance

  private String name;
  private boolean required;
  /** the default value */
  private String defaultValue;
  private Type[] generics;
  private HandlerMethod handlerMethod;

  public MethodParameter(HandlerMethod handlerMethod, MethodParameter other) {
    this.name = other.name;
    this.generics = other.generics;
    this.required = other.required;
    this.parameter = other.parameter;
    this.defaultValue = other.defaultValue;
    this.parameterIndex = other.parameterIndex;
    this.parameterClass = other.parameterClass;

    this.handlerMethod = handlerMethod;
  }

  public MethodParameter(int index, Parameter parameter) {
    this.parameter = parameter;
    this.parameterIndex = index;
    this.parameterClass = parameter.getType();

    AnnotationAttributes attributes = getAnnotationAttributes(RequestParam.class, parameter);
    if (attributes != null) {
      this.name = attributes.getString(Constant.VALUE);
      this.required = attributes.getBoolean("required");
      this.defaultValue = attributes.getString("defaultValue");
    }
    if (StringUtils.isEmpty(defaultValue) && NumberUtils.isNumber(parameterClass)) {
      this.defaultValue = "0"; // fix default value
    }
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
    this(index, ClassUtils.getParameter(method, index), parameterName);
  }

  public boolean isArray() {
    return parameterClass.isArray();
  }

  public boolean isCollection() {
    return CollectionUtils.isCollection(parameterClass);
  }

  public boolean isInterface() {
    return parameterClass.isInterface();
  }

  public boolean is(final Class<?> type) {
    return type == this.parameterClass;
  }

  public boolean isAssignableFrom(final Class<?> superClass) {
    return superClass.isAssignableFrom(parameterClass);
  }

  public boolean isInstance(final Object obj) {
    return parameterClass.isInstance(obj);
  }

  public Type getGenerics(final int index) {
    final Type[] generics = getGenerics();
    if (generics != null && generics.length > index) {
      return generics[index];
    }
    return null;
  }

  public boolean isGenericPresent(final Type requiredType, final int index) {
    return requiredType.equals(getGenerics(index));
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
    return parameter.getType().getSimpleName() + " " + getName();
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj instanceof MethodParameter
            && Objects.equals(parameter, ((MethodParameter) obj).parameter));
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
    return parameterClass.getComponentType();
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setGenerics(Type[] generics) {
    this.generics = generics;
  }

  public Type[] getGenerics() {
    if (generics == null) {
      this.generics = ClassUtils.getGenericTypes(parameter);
    }
    return generics;
  }

  public Parameter getParameter() {
    return parameter;
  }

  public HandlerMethod getHandlerMethod() {
    return handlerMethod;
  }

  public void setHandlerMethod(HandlerMethod handlerMethod) {
    this.handlerMethod = handlerMethod;
  }
}
