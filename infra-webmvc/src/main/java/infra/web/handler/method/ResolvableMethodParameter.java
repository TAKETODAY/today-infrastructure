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

package infra.web.handler.method;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;

import infra.core.AttributeAccessorSupport;
import infra.core.MethodParameter;
import infra.core.ResolvableType;
import infra.core.TypeDescriptor;
import infra.core.annotation.MergedAnnotations;
import infra.lang.Constant;
import infra.util.CollectionUtils;
import infra.util.StringUtils;
import infra.web.RequestContext;
import infra.web.annotation.RequestParam;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MethodParameter
 * @see #resolveParameter(RequestContext)
 * @see Nullable
 * @since 2.3.7
 */
public class ResolvableMethodParameter extends AttributeAccessorSupport {

  /**
   * @since 3.0.1
   */
  @Nullable
  protected TypeDescriptor typeDescriptor;

  // @since 4.0
  private final MethodParameter parameter;

  @Nullable
  private NamedValueInfo namedValueInfo;

  @Nullable
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
   * Return {@code true} if the parameter has at least one annotation,
   * {@code false} if it has none.
   *
   * @see #getParameterAnnotations()
   */
  public boolean hasParameterAnnotations() {
    return parameter.hasParameterAnnotations();
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
  @Nullable
  public <A extends Annotation> A getParameterAnnotation(Class<A> annotationType) {
    return parameter.getParameterAnnotation(annotationType);
  }

  public ResolvableType getResolvableType() {
    ResolvableType resolvableType = this.resolvableType;
    if (resolvableType == null) {
      resolvableType = ResolvableType.forMethodParameter(getParameter());
      this.resolvableType = resolvableType;
    }
    return resolvableType;
  }

  // NamedValueInfo

  public boolean hasNamedValueInfo() {
    return namedValueInfo != null;
  }

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

  public boolean isRequired() {
    return getNamedValueInfo().required;
  }

  public boolean isNotRequired() {
    return !isRequired();
  }

  public String getName() {
    return getNamedValueInfo().name;
  }

  @Nullable
  public String getDefaultValue() {
    return getNamedValueInfo().defaultValue;
  }

  public void withNamedValueInfo(NamedValueInfo namedValueInfo) {
    this.namedValueInfo = namedValueInfo;
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
    var requestParam = MergedAnnotations.from(getParameterAnnotations()).get(RequestParam.class);
    if (requestParam.isPresent()) {
      String name = requestParam.getString("name");
      boolean required = requestParam.getBoolean("required");
      String defaultValue = requestParam.getString("defaultValue");
      return new NamedValueInfo(name, required, defaultValue);
    }
    return new NamedValueInfo(getParameterName());
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

  public String getParameterName() {
    String name = parameter.getParameterName();
    if (name == null) {
      throw new IllegalArgumentException("""
              Name for argument of type [%s] not specified, and parameter name information not \
              available via reflection. Ensure that the compiler uses the '-parameters' flag."""
              .formatted(parameter.getNestedParameterType().getName()));
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
  @SuppressWarnings("NullAway")
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
  @Nullable
  public Object resolveParameter(RequestContext request) throws Throwable {
    return request.getParameter(getName());
  }

  // Getter Setter

  public Class<?> getParameterType() {
    return parameter.getParameterType();
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

  @Override
  public int hashCode() {
    return parameter.hashCode();
  }

  @Override
  public String toString() {
    return "method '%s' parameter %d".formatted(getMethod().getName(), getParameterIndex());
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj instanceof ResolvableMethodParameter
            && Objects.equals(parameter, ((ResolvableMethodParameter) obj).parameter)
    );
  }
}
