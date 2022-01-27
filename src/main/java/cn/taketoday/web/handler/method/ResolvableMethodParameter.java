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
import java.lang.reflect.Method;
import java.util.Objects;

import cn.taketoday.core.AttributeAccessorSupport;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Experimental;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
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
 * @see Nullable
 * @since 2.3.7
 */
public class ResolvableMethodParameter extends AttributeAccessorSupport {

  /**
   * @since 3.0.1
   */
  protected TypeDescriptor typeDescriptor;

  // @since 4.0
  private final MethodParameter parameter;

  @Nullable
  private NamedValueInfo namedValueInfo;

  private ResolvableType resolvableType;

  /**
   * @since 4.0
   */
  public ResolvableMethodParameter(ResolvableMethodParameter other) {
    this.parameter = other.parameter;
    this.resolvableType = other.resolvableType;
    this.namedValueInfo = other.namedValueInfo;
    this.typeDescriptor = other.typeDescriptor; // @since 3.0.1
  }

  public ResolvableMethodParameter(MethodParameter parameter) {
    this.parameter = parameter;
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

  // AnnotatedElement @since 3.0

  /**
   * Return the annotations associated with the target method itself.
   */
  public Annotation[] getMethodAnnotations() {
    return parameter.getMethodAnnotations();
  }

  /**
   * Return the method annotation of the given type, if available.
   *
   * @param annotationType the annotation type to look for
   * @return the annotation object, or {@code null} if not found
   */
  @Nullable
  public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
    return parameter.getMethodAnnotation(annotationType);
  }

  /**
   * Return whether the method is annotated with the given type.
   *
   * @param annotationType the annotation type to look for
   * @see #getMethodAnnotation(Class)
   */
  public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
    return parameter.hasMethodAnnotation(annotationType);
  }

  /**
   * Return the annotations associated with the specific method parameter.
   */
  public Annotation[] getParameterAnnotations() {
    return parameter.getParameterAnnotations();
  }

  /**
   * Return whether the parameter is declared with the given annotation type.
   *
   * @param annotationType the annotation type to look for
   * @see #getParameterAnnotation(Class)
   */
  public boolean hasParameterAnnotation(Class<? extends Annotation> annotationType) {
    return parameter.hasParameterAnnotation(annotationType);
  }

  /**
   * Return the parameter annotation of the given type, if available.
   *
   * @param annotationType the annotation type to look for
   * @return the annotation object, or {@code null} if not found
   */
  public <A extends Annotation> A getParameterAnnotation(Class<A> annotationType) {
    return parameter.getParameterAnnotation(annotationType);
  }

  public ResolvableType getResolvableType() {
    if (resolvableType == null) {
      resolvableType = ResolvableType.forMethodParameter(getParameter());
    }
    return resolvableType;
  }

  // NamedValueInfo

  /**
   * Obtain the named value for the given method parameter.
   */
  public NamedValueInfo getNamedValueInfo() {
    NamedValueInfo namedValueInfo = this.namedValueInfo;
    if (namedValueInfo == null) {
      namedValueInfo = createNamedValueInfo();
      namedValueInfo = updateNamedValueInfo(namedValueInfo);
      this.namedValueInfo = namedValueInfo;
    }
    return namedValueInfo;
  }

  @Experimental
  public boolean isRequired() {
    return getNamedValueInfo().required;
  }

  @Experimental
  public String getName() {
    return getNamedValueInfo().name;
  }

  @Experimental
  public String getDefaultValue() {
    return getNamedValueInfo().defaultValue;
  }

  /**
   * Create the {@link NamedValueInfo} object for the given
   * method parameter. Implementations typically
   * retrieve the method annotation by means of
   * {@link MethodParameter#getParameterAnnotation(Class)}.
   *
   * @return the named value information
   */
  protected NamedValueInfo createNamedValueInfo() {
    RequestParam requestParam = getParameterAnnotation(RequestParam.class);
    if (requestParam == null) {
      return new NamedValueInfo(getParameterName());
    }
    return new NamedValueInfo(requestParam.name(), requestParam.required(), requestParam.defaultValue());
  }

  /**
   * Create a new NamedValueInfo based on the given NamedValueInfo with sanitized values.
   *
   * @see Nullable
   */
  private NamedValueInfo updateNamedValueInfo(NamedValueInfo info) {
    String name = info.name;
    if (StringUtils.isEmpty(name) || Constant.DEFAULT_NONE.equals(name)) {
      // default value
      name = getParameterName();
    }
    boolean required = info.required;
    if (required) {
      required = !parameter.isNullable();
    }
    String defaultValue = Constant.DEFAULT_NONE.equals(info.defaultValue) ? null : info.defaultValue;
    return new NamedValueInfo(name, required, defaultValue);
  }

  @NonNull
  public String getParameterName() {
    String name = parameter.getParameterName();
    if (name == null) {
      throw new IllegalArgumentException(
              "Name for argument of type [" + parameter.getNestedParameterType().getName() +
                      "] not specified, and parameter name information not found in class file either.");
    }
    return name;
  }

  public int getParameterIndex() {
    return parameter.getParameterIndex();
  }

  /**
   * Return the wrapped Method, if any.
   *
   * @return the Method
   */
  public Method getMethod() {
    return parameter.getMethod();
  }

  // resolver

  /**
   * simple impl
   *
   * @param request Current request context
   * @return parameter object
   */
  protected Object resolveParameter(RequestContext request) throws Throwable {
    return request.getParameter(getName());
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

  public Class<?> getParameterType() {
    return parameter.getParameterType();
  }

  public Class<?> getComponentType() {
    return getParameterType().getComponentType();
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
