/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.validation;

import java.beans.PropertyEditor;
import java.io.Serial;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.PropertyEditorRegistry;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Thrown when binding errors are considered fatal. Implements the
 * {@link BindingResult} interface (and its super-interface {@link Errors})
 * to allow for the direct analysis of binding errors.
 *
 * <p>this is a special-purpose class. Normally,
 * application code will work with the {@link BindingResult} interface,
 * or with a {@link DataBinder} that in turn exposes a BindingResult via
 * {@link cn.taketoday.validation.DataBinder#getBindingResult()}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BindingResult
 * @see DataBinder#getBindingResult()
 * @see DataBinder#close()
 * @since 4.0
 */
public class BindException extends Exception implements BindingResult {

  @Serial
  private static final long serialVersionUID = 1L;

  private final BindingResult bindingResult;

  /**
   * Create a new BindException instance for a BindingResult.
   *
   * @param bindingResult the BindingResult instance to wrap
   */
  public BindException(BindingResult bindingResult) {
    Assert.notNull(bindingResult, "BindingResult must not be null");
    this.bindingResult = bindingResult;
  }

  /**
   * Create a new BindException instance for a target bean.
   *
   * @param target the target bean to bind onto
   * @param objectName the name of the target object
   * @see BeanPropertyBindingResult
   */
  public BindException(Object target, String objectName) {
    Assert.notNull(target, "Target object must not be null");
    this.bindingResult = new BeanPropertyBindingResult(target, objectName);
  }

  /**
   * Return the BindingResult that this BindException wraps.
   */
  public final BindingResult getBindingResult() {
    return this.bindingResult;
  }

  @Override
  public String getObjectName() {
    return this.bindingResult.getObjectName();
  }

  @Override
  public void setNestedPath(String nestedPath) {
    this.bindingResult.setNestedPath(nestedPath);
  }

  @Override
  public String getNestedPath() {
    return this.bindingResult.getNestedPath();
  }

  @Override
  public void pushNestedPath(String subPath) {
    this.bindingResult.pushNestedPath(subPath);
  }

  @Override
  public void popNestedPath() throws IllegalStateException {
    this.bindingResult.popNestedPath();
  }

  @Override
  public void reject(String errorCode) {
    this.bindingResult.reject(errorCode);
  }

  @Override
  public void reject(String errorCode, String defaultMessage) {
    this.bindingResult.reject(errorCode, defaultMessage);
  }

  @Override
  public void reject(String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage) {
    this.bindingResult.reject(errorCode, errorArgs, defaultMessage);
  }

  @Override
  public void rejectValue(@Nullable String field, String errorCode) {
    this.bindingResult.rejectValue(field, errorCode);
  }

  @Override
  public void rejectValue(@Nullable String field, String errorCode, String defaultMessage) {
    this.bindingResult.rejectValue(field, errorCode, defaultMessage);
  }

  @Override
  public void rejectValue(@Nullable String field, String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage) {
    this.bindingResult.rejectValue(field, errorCode, errorArgs, defaultMessage);
  }

  @Override
  public void addAllErrors(Errors errors) {
    this.bindingResult.addAllErrors(errors);
  }

  @Override
  public boolean hasErrors() {
    return this.bindingResult.hasErrors();
  }

  @Override
  public int getErrorCount() {
    return this.bindingResult.getErrorCount();
  }

  @Override
  public List<ObjectError> getAllErrors() {
    return this.bindingResult.getAllErrors();
  }

  @Override
  public boolean hasGlobalErrors() {
    return this.bindingResult.hasGlobalErrors();
  }

  @Override
  public int getGlobalErrorCount() {
    return this.bindingResult.getGlobalErrorCount();
  }

  @Override
  public List<ObjectError> getGlobalErrors() {
    return this.bindingResult.getGlobalErrors();
  }

  @Override
  @Nullable
  public ObjectError getGlobalError() {
    return this.bindingResult.getGlobalError();
  }

  @Override
  public boolean hasFieldErrors() {
    return this.bindingResult.hasFieldErrors();
  }

  @Override
  public int getFieldErrorCount() {
    return this.bindingResult.getFieldErrorCount();
  }

  @Override
  public List<FieldError> getFieldErrors() {
    return this.bindingResult.getFieldErrors();
  }

  @Override
  @Nullable
  public FieldError getFieldError() {
    return this.bindingResult.getFieldError();
  }

  @Override
  public boolean hasFieldErrors(String field) {
    return this.bindingResult.hasFieldErrors(field);
  }

  @Override
  public int getFieldErrorCount(String field) {
    return this.bindingResult.getFieldErrorCount(field);
  }

  @Override
  public List<FieldError> getFieldErrors(String field) {
    return this.bindingResult.getFieldErrors(field);
  }

  @Override
  @Nullable
  public FieldError getFieldError(String field) {
    return this.bindingResult.getFieldError(field);
  }

  @Override
  @Nullable
  public Object getFieldValue(String field) {
    return this.bindingResult.getFieldValue(field);
  }

  @Override
  @Nullable
  public Class<?> getFieldType(String field) {
    return this.bindingResult.getFieldType(field);
  }

  @Override
  @Nullable
  public Object getTarget() {
    return this.bindingResult.getTarget();
  }

  @Override
  public Map<String, Object> getModel() {
    return this.bindingResult.getModel();
  }

  @Override
  @Nullable
  public Object getRawFieldValue(String field) {
    return this.bindingResult.getRawFieldValue(field);
  }

  @Override
  @SuppressWarnings("rawtypes")
  @Nullable
  public PropertyEditor findEditor(@Nullable String field, @Nullable Class valueType) {
    return this.bindingResult.findEditor(field, valueType);
  }

  @Override
  @Nullable
  public PropertyEditorRegistry getPropertyEditorRegistry() {
    return this.bindingResult.getPropertyEditorRegistry();
  }

  @Override
  public String[] resolveMessageCodes(String errorCode) {
    return this.bindingResult.resolveMessageCodes(errorCode);
  }

  @Override
  public String[] resolveMessageCodes(String errorCode, String field) {
    return this.bindingResult.resolveMessageCodes(errorCode, field);
  }

  @Override
  public void addError(ObjectError error) {
    this.bindingResult.addError(error);
  }

  @Override
  public void recordFieldValue(String field, Class<?> type, @Nullable Object value) {
    this.bindingResult.recordFieldValue(field, type, value);
  }

  @Override
  public void recordSuppressedField(String field) {
    this.bindingResult.recordSuppressedField(field);
  }

  @Override
  public String[] getSuppressedFields() {
    return this.bindingResult.getSuppressedFields();
  }

  /**
   * Returns diagnostic information about the errors held in this object.
   */
  @Override
  public String getMessage() {
    return this.bindingResult.toString();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || this.bindingResult.equals(other));
  }

  @Override
  public int hashCode() {
    return this.bindingResult.hashCode();
  }

}
