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

package cn.taketoday.validation.method;

import java.util.List;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.validation.Errors;
import cn.taketoday.validation.FieldError;
import cn.taketoday.validation.ObjectError;

/**
 * Extension of {@link ParameterValidationResult} created for Object method
 * parameters or return values with nested errors on their properties.
 *
 * <p>The base class method {@link #getResolvableErrors()} returns
 * {@link Errors#getAllErrors()}, but this subclass provides access to the same
 * as {@link FieldError}s.
 *
 * <p>When the method parameter is a container with multiple elements such as a
 * {@link List}, {@link java.util.Set}, array, {@link java.util.Map}, or others,
 * then a separate {@link ParameterErrors} is created for each element that has
 * errors. In that case, the {@link #getContainer() container},
 * {@link #getContainerIndex() containerIndex}, and {@link #getContainerKey() containerKey}
 * provide additional context.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ParameterErrors extends ParameterValidationResult implements Errors {

  private final Errors errors;

  @Nullable
  private final Object container;

  @Nullable
  private final Integer containerIndex;

  @Nullable
  private final Object containerKey;

  /**
   * Create a {@code ParameterErrors}.
   */
  public ParameterErrors(MethodParameter parameter, @Nullable Object argument,
          Errors errors, @Nullable Object container, @Nullable Integer index, @Nullable Object key) {

    super(parameter, argument, errors.getAllErrors());
    this.errors = errors;
    this.container = container;
    this.containerIndex = index;
    this.containerKey = key;
  }

  /**
   * When {@code @Valid} is declared on a container of elements such as
   * {@link java.util.Collection}, {@link java.util.Map},
   * {@link java.util.Optional}, and others, this method returns the container
   * of the validated {@link #getArgument() argument}, while
   * {@link #getContainerIndex()} and {@link #getContainerKey()} provide
   * information about the index or key if applicable.
   */
  @Nullable
  public Object getContainer() {
    return this.container;
  }

  /**
   * When {@code @Valid} is declared on an indexed container of elements such as
   * {@link List} or array, this method returns the index of the validated
   * {@link #getArgument() argument}.
   */
  @Nullable
  public Integer getContainerIndex() {
    return this.containerIndex;
  }

  /**
   * When {@code @Valid} is declared on a container of elements referenced by
   * key such as {@link java.util.Map}, this method returns the key of the
   * validated {@link #getArgument() argument}.
   */
  @Nullable
  public Object getContainerKey() {
    return this.containerKey;
  }

  // Errors implementation

  @Override
  public String getObjectName() {
    return this.errors.getObjectName();
  }

  @Override
  public void setNestedPath(String nestedPath) {
    this.errors.setNestedPath(nestedPath);
  }

  @Override
  public String getNestedPath() {
    return this.errors.getNestedPath();
  }

  @Override
  public void pushNestedPath(String subPath) {
    this.errors.pushNestedPath(subPath);
  }

  @Override
  public void popNestedPath() throws IllegalStateException {
    this.errors.popNestedPath();
  }

  @Override
  public void reject(String errorCode) {
    this.errors.reject(errorCode);
  }

  @Override
  public void reject(String errorCode, String defaultMessage) {
    this.errors.reject(errorCode, defaultMessage);
  }

  @Override
  public void reject(String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage) {
    this.errors.reject(errorCode, errorArgs, defaultMessage);
  }

  @Override
  public void rejectValue(@Nullable String field, String errorCode) {
    this.errors.rejectValue(field, errorCode);
  }

  @Override
  public void rejectValue(@Nullable String field, String errorCode, String defaultMessage) {
    this.errors.rejectValue(field, errorCode, defaultMessage);
  }

  @Override
  public void rejectValue(@Nullable String field, String errorCode,
          @Nullable Object[] errorArgs, @Nullable String defaultMessage) {

    this.errors.rejectValue(field, errorCode, errorArgs, defaultMessage);
  }

  @Override
  public void addAllErrors(Errors errors) {
    this.errors.addAllErrors(errors);
  }

  @Override
  public boolean hasErrors() {
    return this.errors.hasErrors();
  }

  @Override
  public int getErrorCount() {
    return this.errors.getErrorCount();
  }

  @Override
  public List<ObjectError> getAllErrors() {
    return this.errors.getAllErrors();
  }

  @Override
  public boolean hasGlobalErrors() {
    return this.errors.hasGlobalErrors();
  }

  @Override
  public int getGlobalErrorCount() {
    return this.errors.getGlobalErrorCount();
  }

  @Override
  public List<ObjectError> getGlobalErrors() {
    return this.errors.getGlobalErrors();
  }

  @Override
  public ObjectError getGlobalError() {
    return this.errors.getGlobalError();
  }

  @Override
  public boolean hasFieldErrors() {
    return this.errors.hasFieldErrors();
  }

  @Override
  public int getFieldErrorCount() {
    return this.errors.getFieldErrorCount();
  }

  @Override
  public List<FieldError> getFieldErrors() {
    return this.errors.getFieldErrors();
  }

  @Override
  public FieldError getFieldError() {
    return this.errors.getFieldError();
  }

  @Override
  public boolean hasFieldErrors(String field) {
    return this.errors.hasFieldErrors(field);
  }

  @Override
  public int getFieldErrorCount(String field) {
    return this.errors.getFieldErrorCount(field);
  }

  @Override
  public List<FieldError> getFieldErrors(String field) {
    return this.errors.getFieldErrors(field);
  }

  @Override
  public FieldError getFieldError(String field) {
    return this.errors.getFieldError(field);
  }

  @Override
  public Object getFieldValue(String field) {
    return this.errors.getFieldError(field);
  }

  @Override
  public Class<?> getFieldType(String field) {
    return this.errors.getFieldType(field);
  }

}
