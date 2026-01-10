/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans.factory;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.Objects;

import infra.beans.factory.config.DependencyDescriptor;
import infra.core.MethodParameter;
import infra.lang.Assert;
import infra.util.ObjectUtils;

/**
 * A simple descriptor for an injection point, pointing to a method/constructor
 * parameter or a field. Exposed by {@link UnsatisfiedDependencyException}.
 * Also available as an argument for factory methods, reacting to the
 * requesting injection point for building a customized bean instance.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/16 21:29</a>
 * @see UnsatisfiedDependencyException#getInjectionPoint()
 * @see DependencyDescriptor
 * @since 4.0
 */
public class InjectionPoint {

  private volatile Annotation @Nullable [] fieldAnnotations;

  @Nullable
  protected MethodParameter methodParameter;

  @Nullable
  protected Field field;

  /**
   * Create an injection point descriptor for a method or constructor parameter.
   *
   * @param methodParameter the MethodParameter to wrap
   */
  public InjectionPoint(MethodParameter methodParameter) {
    Assert.notNull(methodParameter, "MethodParameter is required");
    this.methodParameter = methodParameter;
  }

  /**
   * Create an injection point descriptor for a field.
   *
   * @param field the field to wrap
   */
  public InjectionPoint(Field field) {
    Assert.notNull(field, "Field is required");
    this.field = field;
  }

  /**
   * Copy constructor.
   *
   * @param original the original descriptor to create a copy from
   */
  protected InjectionPoint(InjectionPoint original) {
    this.methodParameter =
            original.methodParameter != null ? new MethodParameter(original.methodParameter) : null;
    this.field = original.field;
  }

  /**
   * Just available for serialization purposes in subclasses.
   */
  protected InjectionPoint() {
  }

  /**
   * Return the wrapped MethodParameter, if any.
   * <p>Note: Either MethodParameter or Field is available.
   *
   * @return the MethodParameter, or {@code null} if none
   */
  @Nullable
  public MethodParameter getMethodParameter() {
    return this.methodParameter;
  }

  /**
   * Return the wrapped Field, if any.
   * <p>Note: Either MethodParameter or Field is available.
   *
   * @return the Field, or {@code null} if none
   */
  @Nullable
  public Field getField() {
    return this.field;
  }

  /**
   * Return the wrapped MethodParameter, assuming it is present.
   *
   * @return the MethodParameter (never {@code null})
   * @throws IllegalStateException if no MethodParameter is available
   */
  protected final MethodParameter obtainMethodParameter() {
    Assert.state(this.methodParameter != null, "Neither Field nor MethodParameter");
    return this.methodParameter;
  }

  /**
   * Returns the wrapped member, containing the injection point.
   *
   * @return the Field / Method / Constructor as Member
   */
  public Member getMember() {
    return this.field != null ? this.field : obtainMethodParameter().getMember();
  }

  /**
   * Return the type declared by the underlying field or method/constructor parameter,
   * indicating the injection type.
   */
  public Class<?> getDeclaredType() {
    return this.field != null ? this.field.getType() : obtainMethodParameter().getParameterType();
  }

  /**
   * Return the wrapped annotated element.
   * <p>Note: In case of a method/constructor parameter, this exposes
   * the annotations declared on the method or constructor itself
   * (i.e. at the method/constructor level, not at the parameter level).
   * Use {@link #getAnnotations()} to obtain parameter-level annotations in
   * such a scenario, transparently with corresponding field annotations.
   *
   * @return the Field / Method / Constructor as AnnotatedElement
   */
  public AnnotatedElement getAnnotatedElement() {
    return this.field != null ? this.field : obtainMethodParameter().getAnnotatedElement();
  }

  /**
   * Obtain the annotations associated with the wrapped field or method/constructor parameter.
   */
  public Annotation[] getAnnotations() {
    if (this.field != null) {
      Annotation[] fieldAnnotations = this.fieldAnnotations;
      if (fieldAnnotations == null) {
        fieldAnnotations = this.field.getAnnotations();
        this.fieldAnnotations = fieldAnnotations;
      }
      return fieldAnnotations;
    }
    else {
      return obtainMethodParameter().getParameterAnnotations();
    }
  }

  /**
   * Retrieve a field/parameter annotation of the given type, if any.
   *
   * @param annotationType the annotation type to retrieve
   * @return the annotation instance, or {@code null} if none found
   */
  @Nullable
  public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
    return this.field != null ? this.field.getAnnotation(annotationType) :
            obtainMethodParameter().getParameterAnnotation(annotationType);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    InjectionPoint otherPoint = (InjectionPoint) other;
    return Objects.equals(this.field, otherPoint.field)
            && Objects.equals(this.methodParameter, otherPoint.methodParameter);
  }

  @Override
  public int hashCode() {
    return this.field != null ? this.field.hashCode() : ObjectUtils.nullSafeHashCode(this.methodParameter);
  }

  @Override
  public String toString() {
    return this.field != null ? "field '" + this.field.getName() + "'" : String.valueOf(this.methodParameter);
  }

}
