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

package cn.taketoday.beans.dependency;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.Map;

import cn.taketoday.beans.factory.PropertyValueRetriever;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Autowired;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Required;
import cn.taketoday.util.ObjectUtils;

/**
 * Dependency InjectionPoint
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/16 21:29</a>
 * @since 4.0
 */
public abstract class InjectionPoint implements Serializable {

  /**
   * It shows that the value is not set
   */
  public static final Object DO_NOT_SET = PropertyValueRetriever.DO_NOT_SET;

  protected Boolean required = null;
  protected MergedAnnotations annotations;

  @Nullable
  private transient ResolvableType resolvableType;

  @Nullable
  private transient TypeDescriptor typeDescriptor;

  public int nestingLevel;

  public void increaseNestingLevel() {
    nestingLevel++;
  }

  public void decreaseNestingLevel() {
    nestingLevel--;
  }

  public abstract Class<?> getDependencyType();

  /**
   * Obtain the annotations associated with the wrapped field or method/constructor parameter.
   */
  public MergedAnnotations getAnnotations() {
    if (annotations == null) {
      annotations = doGetAnnotations();
    }
    return annotations;
  }

  protected MergedAnnotations doGetAnnotations() {
    if (this.field != null) {
      return MergedAnnotations.from(field);
    }
    else {
      return MergedAnnotations.from(obtainMethodParameter().getParameterAnnotations());
    }
  }

  public <A extends Annotation> boolean isAnnotationPresent(Class<A> annotationType) {
    return getAnnotations().isPresent(annotationType);
  }

  /**
   * Retrieve a field/parameter annotation of the given type, if any.
   *
   * @param annotationType the annotation type to retrieve
   * @return the MergedAnnotation
   */
  public <A extends Annotation> MergedAnnotation<A> getAnnotation(Class<A> annotationType) {
    return getAnnotations().get(annotationType);
  }

  public boolean isRequired() {
    if (required == null) {
      required = doGetRequiredStatus();
    }
    return required;
  }

  protected boolean doGetRequiredStatus() {
    MergedAnnotations annotations = getAnnotations();
    MergedAnnotation<Autowired> annotation = annotations.get(Autowired.class);
    if (annotation.isPresent()) {
      return annotation.getBoolean("required");
    }
    return annotations.isPresent(Required.class);
  }

  /**
   * Build a {@link ResolvableType} object for the wrapped parameter/field.
   */
  public ResolvableType getResolvableType() {
    if (resolvableType == null) {
      resolvableType = doGetResolvableType();
    }
    return resolvableType;
  }

  protected abstract ResolvableType doGetResolvableType();

  /**
   * Build a {@link TypeDescriptor} object for the wrapped parameter/field.
   */
  public TypeDescriptor getTypeDescriptor() {
    if (typeDescriptor == null) {
      typeDescriptor = doGetTypeDescriptor();
    }
    return typeDescriptor;
  }

  protected abstract TypeDescriptor doGetTypeDescriptor();

  public boolean isArray() {
    return getDependencyType().isArray();
  }

  public boolean isMap() {
    return Map.class.isAssignableFrom(getDependencyType());
  }

  public boolean dependencyIs(Class<?> type) {
    return type == getDependencyType();
  }

  public abstract Object getTarget();

  public boolean isProperty() {
    return false;
  }

  //

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
    Assert.notNull(methodParameter, "MethodParameter must not be null");
    this.methodParameter = methodParameter;
  }

  /**
   * Create an injection point descriptor for a field.
   *
   * @param field the field to wrap
   */
  public InjectionPoint(Field field) {
    Assert.notNull(field, "Field must not be null");
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
  protected InjectionPoint() { }

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
   * Return the type declared by the underlying field or method/constructor parameter,
   * indicating the injection type.
   */
  public Class<?> getDeclaredType() {
    return (this.field != null ? this.field.getType() : obtainMethodParameter().getParameterType());
  }

  /**
   * Returns the wrapped member, containing the injection point.
   *
   * @return the Field / Method / Constructor as Member
   */
  public Member getMember() {
    return (this.field != null ? this.field : obtainMethodParameter().getMember());
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

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    InjectionPoint otherPoint = (InjectionPoint) other;
    return (ObjectUtils.nullSafeEquals(this.field, otherPoint.field) &&
            ObjectUtils.nullSafeEquals(this.methodParameter, otherPoint.methodParameter));
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
