/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.validation;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * A simple implementation of the {@link Errors} interface, managing global
 * errors and field errors for a top-level target object. Flexibly retrieves
 * field values through bean property getter methods, and automatically
 * falls back to raw field access if necessary.
 *
 * <p>Note that this {@link Errors} implementation comes without support for
 * nested paths. It is exclusively designed for the validation of individual
 * top-level objects, not aggregating errors from multiple sources.
 * If this is insufficient for your purposes, use a binding-capable
 * {@link Errors} implementation such as {@link BeanPropertyBindingResult}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Validator#validateObject(Object)
 * @see BeanPropertyBindingResult
 * @see DirectFieldBindingResult
 * @since 4.0 2023/8/6 21:00
 */
@SuppressWarnings("serial")
public class SimpleErrors implements Errors, Serializable {

  private final Object target;

  private final String objectName;

  private final List<ObjectError> globalErrors = new ArrayList<>();

  private final List<FieldError> fieldErrors = new ArrayList<>();

  /**
   * Create a new {@link SimpleErrors} holder for the given target,
   * using the simple name of the target class as the object name.
   *
   * @param target the target to wrap
   */
  public SimpleErrors(Object target) {
    Assert.notNull(target, "Target must not be null");
    this.target = target;
    this.objectName = this.target.getClass().getSimpleName();
  }

  /**
   * Create a new {@link SimpleErrors} holder for the given target.
   *
   * @param target the target to wrap
   * @param objectName the name of the target object for error reporting
   */
  public SimpleErrors(Object target, String objectName) {
    Assert.notNull(target, "Target must not be null");
    this.target = target;
    this.objectName = objectName;
  }

  @Override
  public String getObjectName() {
    return this.objectName;
  }

  @Override
  public void reject(String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage) {
    this.globalErrors.add(new ObjectError(getObjectName(), new String[] { errorCode }, errorArgs, defaultMessage));
  }

  @Override
  public void rejectValue(@Nullable String field, String errorCode,
          @Nullable Object[] errorArgs, @Nullable String defaultMessage) {

    if (StringUtils.isEmpty(field)) {
      reject(errorCode, errorArgs, defaultMessage);
      return;
    }

    Object newVal = getFieldValue(field);
    this.fieldErrors.add(new FieldError(getObjectName(), field, newVal, false,
            new String[] { errorCode }, errorArgs, defaultMessage));
  }

  @Override
  public void addAllErrors(Errors errors) {
    this.globalErrors.addAll(errors.getGlobalErrors());
    this.fieldErrors.addAll(errors.getFieldErrors());
  }

  @Override
  public List<ObjectError> getGlobalErrors() {
    return this.globalErrors;
  }

  @Override
  public List<FieldError> getFieldErrors() {
    return this.fieldErrors;
  }

  @Override
  @Nullable
  public Object getFieldValue(String field) {
    FieldError fieldError = getFieldError(field);
    if (fieldError != null) {
      return fieldError.getRejectedValue();
    }

    PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(this.target.getClass(), field);
    if (pd != null && pd.getReadMethod() != null) {
      ReflectionUtils.makeAccessible(pd.getReadMethod());
      return ReflectionUtils.invokeMethod(pd.getReadMethod(), this.target);
    }

    Field rawField = ReflectionUtils.findField(this.target.getClass(), field);
    if (rawField != null) {
      ReflectionUtils.makeAccessible(rawField);
      return ReflectionUtils.getField(rawField, this.target);
    }

    throw new IllegalArgumentException("Cannot retrieve value for field '" + field +
            "' - neither a getter method nor a raw field found");
  }

  @Override
  public Class<?> getFieldType(String field) {
    PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(this.target.getClass(), field);
    if (pd != null) {
      return pd.getPropertyType();
    }
    Field rawField = ReflectionUtils.findField(this.target.getClass(), field);
    if (rawField != null) {
      return rawField.getType();
    }
    return null;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return this == other || (
            other instanceof SimpleErrors that
                    && ObjectUtils.nullSafeEquals(this.target, that.target)
                    && this.globalErrors.equals(that.globalErrors)
                    && this.fieldErrors.equals(that.fieldErrors));
  }

  @Override
  public int hashCode() {
    return this.target.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (ObjectError error : this.globalErrors) {
      sb.append('\n').append(error);
    }
    for (ObjectError error : this.fieldErrors) {
      sb.append('\n').append(error);
    }
    return sb.toString();
  }

}
