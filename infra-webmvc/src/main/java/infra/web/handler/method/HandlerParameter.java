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

import infra.core.DefaultAttributeAccessor;
import infra.core.MethodParameter;
import infra.core.ResolvableType;
import infra.core.TypeDescriptor;
import infra.core.annotation.MergedAnnotations;
import infra.lang.Constant;
import infra.util.Assert;
import infra.util.CollectionUtils;
import infra.util.StringUtils;
import infra.web.HttpContext;
import infra.web.annotation.RequestParam;

/**
 * Web-specific descriptor for a handler method parameter that can resolve an
 * argument value from an {@link HttpContext}.
 *
 * <p>This class decorates a {@link MethodParameter} and exposes commonly used
 * reflection metadata, including the parameter type, annotations, generic type,
 * and {@link TypeDescriptor}. It also provides named-value metadata such as the
 * binding name, whether a value is required, and the configured default value.
 * Named-value metadata is derived from {@link RequestParam} by default, including
 * composed annotations discovered through {@link MergedAnnotations}.
 *
 * <p>Type metadata and named-value metadata are resolved lazily and cached for
 * subsequent access. Subclasses may customize named-value creation through
 * {@link #createNamedValueInfo()} and argument resolution through
 * {@link #resolveArgument(HttpContext)}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MethodParameter
 * @see NamedValueInfo
 * @see RequestParam
 * @since 2.3.7
 */
public class HandlerParameter extends DefaultAttributeAccessor {

  // @since 4.0
  private final MethodParameter parameter;

  private @Nullable NamedValueInfo namedValueInfo;

  private @Nullable ResolvableType resolvableType;

  /**
   * @since 3.0.1
   */
  protected @Nullable TypeDescriptor typeDescriptor;

  /**
   * Create a shallow copy of the supplied resolvable parameter.
   *
   * <p>The underlying {@link MethodParameter}, previously resolved metadata,
   * and attributes are shared with the supplied instance.
   *
   * @param other the resolvable method parameter to copy
   * @since 4.0
   */
  public HandlerParameter(HandlerParameter other) {
    this.attributes = other.attributes;
    this.parameter = other.parameter;
    this.resolvableType = other.resolvableType;
    this.namedValueInfo = other.namedValueInfo;
    this.typeDescriptor = other.typeDescriptor; // @since 3.0.1
  }

  /**
   * Create a resolvable descriptor for the supplied method parameter.
   *
   * @param parameter the method parameter to wrap
   * @throws IllegalArgumentException if {@code parameter} is {@code null}
   */
  public HandlerParameter(MethodParameter parameter) {
    Assert.notNull(parameter, "parameter is required");
    this.parameter = parameter;
  }

  /**
   * Determine whether the declared parameter type is an array.
   */
  public boolean isArray() {
    return getParameterType().isArray();
  }

  /**
   * Determine whether the declared parameter type is a collection type.
   */
  public boolean isCollection() {
    return CollectionUtils.isCollection(getParameterType());
  }

  /**
   * Determine whether the declared parameter type is an interface.
   */
  public boolean isInterface() {
    return getParameterType().isInterface();
  }

  /**
   * Determine whether the declared parameter type is exactly the supplied type.
   *
   * @param type the type to compare against
   */
  public boolean is(final Class<?> type) {
    return type == getParameterType();
  }

  /**
   * Determine whether the declared parameter type can be assigned to the
   * supplied target type.
   *
   * @param superClass the target superclass or interface
   */
  public boolean isAssignableTo(final Class<?> superClass) {
    return superClass.isAssignableFrom(getParameterType());
  }

  /**
   * Determine whether the supplied value is an instance of the declared
   * parameter type.
   *
   * @param val the value to check
   */
  public boolean isInstance(final Object val) {
    return getParameterType().isInstance(val);
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
  public <A extends Annotation> @Nullable A getMethodAnnotation(Class<A> annotationType) {
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
  public <A extends Annotation> @Nullable A getParameterAnnotation(Class<A> annotationType) {
    return parameter.getParameterAnnotation(annotationType);
  }

  /**
   * Return the generic-aware type of this method parameter.
   *
   * <p>The result is resolved lazily and cached.
   *
   * @return the resolvable parameter type
   */
  public ResolvableType getResolvableType() {
    ResolvableType resolvableType = this.resolvableType;
    if (resolvableType == null) {
      resolvableType = ResolvableType.forMethodParameter(getMethodParameter());
      this.resolvableType = resolvableType;
    }
    return resolvableType;
  }

  // NamedValueInfo

  /**
   * Return whether named-value metadata has already been initialized or
   * explicitly supplied.
   *
   * <p>This method reports cached state and does not trigger metadata creation.
   */
  public boolean hasNamedValueInfo() {
    return namedValueInfo != null;
  }

  /**
   * Obtain the named-value metadata for this method parameter.
   *
   * <p>If necessary, the metadata is created through
   * {@link #createNamedValueInfo()}, normalized, and then cached.
   *
   * @return the named-value metadata
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

  /**
   * Return whether a value is required for this method parameter.
   */
  public boolean isRequired() {
    return getNamedValueInfo().required;
  }

  /**
   * Return whether a value is optional for this method parameter.
   */
  public boolean isNotRequired() {
    return !isRequired();
  }

  /**
   * Return the name used to resolve and bind the argument value.
   *
   * <p>This may be an explicitly configured name or the discovered Java
   * parameter name.
   */
  public String getName() {
    return getNamedValueInfo().name;
  }

  /**
   * Return the configured default value, if any.
   *
   * @return the default value, or {@code null} if none is configured
   */
  public @Nullable String getDefaultValue() {
    return getNamedValueInfo().defaultValue;
  }

  /**
   * Replace the named-value metadata associated with this parameter.
   *
   * <p>The supplied metadata is used directly and is not normalized through
   * the standard metadata creation process.
   *
   * @param namedValueInfo the named-value metadata to use
   */
  public void withNamedValueInfo(NamedValueInfo namedValueInfo) {
    this.namedValueInfo = namedValueInfo;
  }

  /**
   * Create the named-value metadata for this method parameter.
   *
   * <p>The default implementation obtains a merged {@link RequestParam}
   * annotation, if present, and uses its name, required flag, and default
   * value. Otherwise, it creates metadata using the discovered Java parameter
   * name. Subclasses may override this method to support other named-value
   * annotations.
   *
   * @return the named-value metadata; never {@code null}
   * @see MergedAnnotations
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
   * Normalize the supplied metadata by applying the discovered parameter name,
   * nullability rules, and the framework's default-value sentinel.
   *
   * @param info the metadata to normalize
   * @return a normalized metadata instance
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

  /**
   * Return the discovered Java parameter name.
   *
   * @return the Java parameter name
   * @throws IllegalArgumentException if no parameter name is available
   * @see MethodParameter#getParameterName()
   */
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

  /**
   * Return the index of this parameter in the declaring method's parameter list.
   */
  public int getParameterIndex() {
    return parameter.getParameterIndex();
  }

  /**
   * Return the method that declares this parameter.
   *
   * @return the declaring method
   */
  @SuppressWarnings("NullAway")
  public Method getMethod() {
    return parameter.getMethod();
  }

  // resolver

  /**
   * Resolve an argument value for this method parameter from the supplied HTTP
   * context.
   *
   * <p>The default implementation returns the request parameter identified by
   * {@link #getName()}. Subclasses may override this method to delegate to a
   * complete argument resolution strategy, including binding and conversion.
   *
   * @param request the current HTTP context
   * @return the resolved argument value, or {@code null} if no value is available
   * @throws Exception if argument resolution fails
   */
  public @Nullable Object resolveArgument(HttpContext request) throws Exception {
    return request.getParameter(getName());
  }

  // Getter Setter

  /**
   * Return the declared type of this method parameter.
   */
  public Class<?> getParameterType() {
    return parameter.getParameterType();
  }

  /**
   * Return the underlying method parameter descriptor.
   */
  public MethodParameter getMethodParameter() {
    return parameter;
  }

  //

  /**
   * Return the type descriptor for this method parameter.
   *
   * <p>The descriptor is created lazily through {@link #createTypeDescriptor()}
   * and cached for subsequent access.
   *
   * @return the parameter type descriptor
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
   * Create the type descriptor for this method parameter.
   *
   * <p>Subclasses may override this method to provide a specialized descriptor.
   *
   * @return a new type descriptor
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
  public boolean equals(@Nullable Object obj) {
    return obj == this || (obj instanceof HandlerParameter
            && Objects.equals(parameter, ((HandlerParameter) obj).parameter)
    );
  }
}
