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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.PropertyEditorRegistry;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Abstract implementation of the {@link BindingResult} interface and
 * its super-interface {@link Errors}. Encapsulates common management of
 * {@link ObjectError ObjectErrors} and {@link FieldError FieldErrors}.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Errors
 * @since 4.0
 */
public abstract class AbstractBindingResult extends AbstractErrors implements BindingResult, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final String objectName;

  private MessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();

  private final ArrayList<ObjectError> errors = new ArrayList<>();
  private final HashSet<String> suppressedFields = new HashSet<>();
  private final HashMap<String, Object> fieldValues = new HashMap<>();
  private final HashMap<String, Class<?>> fieldTypes = new HashMap<>();

  /**
   * Create a new AbstractBindingResult instance.
   *
   * @param objectName the name of the target object
   * @see DefaultMessageCodesResolver
   */
  protected AbstractBindingResult(String objectName) {
    this.objectName = objectName;
  }

  /**
   * Set the strategy to use for resolving errors into message codes.
   * Default is DefaultMessageCodesResolver.
   *
   * @see DefaultMessageCodesResolver
   */
  public void setMessageCodesResolver(MessageCodesResolver messageCodesResolver) {
    Assert.notNull(messageCodesResolver, "MessageCodesResolver is required");
    this.messageCodesResolver = messageCodesResolver;
  }

  /**
   * Return the strategy to use for resolving errors into message codes.
   */
  public MessageCodesResolver getMessageCodesResolver() {
    return this.messageCodesResolver;
  }

  //---------------------------------------------------------------------
  // Implementation of the Errors interface
  //---------------------------------------------------------------------

  @Override
  public String getObjectName() {
    return this.objectName;
  }

  @Override
  public void reject(String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage) {
    addError(new ObjectError(getObjectName(), resolveMessageCodes(errorCode), errorArgs, defaultMessage));
  }

  @Override
  public void rejectValue(@Nullable String field, String errorCode,
          @Nullable Object[] errorArgs, @Nullable String defaultMessage) {

    if (StringUtils.isEmpty(getNestedPath()) && StringUtils.isEmpty(field)) {
      // We're at the top of the nested object hierarchy,
      // so the present level is not a field but rather the top object.
      // The best we can do is register a global error here...
      reject(errorCode, errorArgs, defaultMessage);
      return;
    }

    String fixedField = fixedField(field);
    Object newVal = getActualFieldValue(fixedField);
    FieldError fe = new FieldError(getObjectName(), fixedField, newVal, false,
            resolveMessageCodes(errorCode, field), errorArgs, defaultMessage);
    addError(fe);
  }

  @Override
  public void addAllErrors(Errors errors) {
    if (!errors.getObjectName().equals(getObjectName())) {
      throw new IllegalArgumentException("Errors object needs to have same object name");
    }
    this.errors.addAll(errors.getAllErrors());
  }

  @Override
  public boolean hasErrors() {
    return !this.errors.isEmpty();
  }

  @Override
  public int getErrorCount() {
    return this.errors.size();
  }

  @Override
  public List<ObjectError> getAllErrors() {
    return Collections.unmodifiableList(this.errors);
  }

  @Override
  public List<ObjectError> getGlobalErrors() {
    ArrayList<ObjectError> result = new ArrayList<>();
    for (ObjectError objectError : this.errors) {
      if (!(objectError instanceof FieldError)) {
        result.add(objectError);
      }
    }
    return Collections.unmodifiableList(result);
  }

  @Override
  @Nullable
  public ObjectError getGlobalError() {
    for (ObjectError objectError : this.errors) {
      if (!(objectError instanceof FieldError)) {
        return objectError;
      }
    }
    return null;
  }

  @Override
  public List<FieldError> getFieldErrors() {
    ArrayList<FieldError> result = new ArrayList<>();
    for (ObjectError objectError : this.errors) {
      if (objectError instanceof FieldError) {
        result.add((FieldError) objectError);
      }
    }
    return Collections.unmodifiableList(result);
  }

  @Override
  @Nullable
  public FieldError getFieldError() {
    for (ObjectError objectError : this.errors) {
      if (objectError instanceof FieldError) {
        return (FieldError) objectError;
      }
    }
    return null;
  }

  @Override
  public List<FieldError> getFieldErrors(String field) {
    ArrayList<FieldError> result = new ArrayList<>();
    String fixedField = fixedField(field);
    for (ObjectError objectError : this.errors) {
      if (objectError instanceof FieldError && isMatchingFieldError(fixedField, (FieldError) objectError)) {
        result.add((FieldError) objectError);
      }
    }
    return Collections.unmodifiableList(result);
  }

  @Override
  @Nullable
  public FieldError getFieldError(String field) {
    String fixedField = fixedField(field);
    for (ObjectError objectError : this.errors) {
      if (objectError instanceof FieldError fieldError) {
        if (isMatchingFieldError(fixedField, fieldError)) {
          return fieldError;
        }
      }
    }
    return null;
  }

  @Override
  @Nullable
  public Object getFieldValue(String field) {
    FieldError fieldError = getFieldError(field);
    // Use rejected value in case of error, current field value otherwise.
    if (fieldError != null) {
      Object value = fieldError.getRejectedValue();
      // Do not apply formatting on binding failures like type mismatches.
      return fieldError.isBindingFailure() || getTarget() == null ? value : formatFieldValue(field, value);
    }
    else if (getTarget() != null) {
      Object value = getActualFieldValue(fixedField(field));
      return formatFieldValue(field, value);
    }
    else {
      return this.fieldValues.get(field);
    }
  }

  /**
   * This default implementation determines the type based on the actual
   * field value, if any. Subclasses should override this to determine
   * the type from a descriptor, even for {@code null} values.
   *
   * @see #getActualFieldValue
   */
  @Override
  @Nullable
  public Class<?> getFieldType(@Nullable String field) {
    if (getTarget() != null) {
      Object value = getActualFieldValue(fixedField(field));
      if (value != null) {
        return value.getClass();
      }
    }
    return this.fieldTypes.get(field);
  }

  //---------------------------------------------------------------------
  // Implementation of BindingResult interface
  //---------------------------------------------------------------------

  /**
   * Return a model Map for the obtained state, exposing an Errors
   * instance as '{@link #MODEL_KEY_PREFIX MODEL_KEY_PREFIX} + objectName'
   * and the object itself.
   * <p>Note that the Map is constructed every time you're calling this method.
   * Adding things to the map and then re-calling this method will not work.
   * <p>The attributes in the model Map returned by this method are usually
   * included in the ModelAndView for a form view that uses Framework's bind tag,
   * which needs access to the Errors instance.
   *
   * @see #getObjectName
   * @see #MODEL_KEY_PREFIX
   */
  @Override
  public Map<String, Object> getModel() {
    LinkedHashMap<String, Object> model = new LinkedHashMap<>(2);
    // Mapping from name to target object.
    model.put(getObjectName(), getTarget());
    // Errors instance, even if no errors.
    model.put(MODEL_KEY_PREFIX + getObjectName(), this);
    return model;
  }

  @Override
  @Nullable
  public Object getRawFieldValue(String field) {
    return getTarget() != null ? getActualFieldValue(fixedField(field)) : null;
  }

  /**
   * This implementation delegates to the
   * {@link #getPropertyEditorRegistry() PropertyEditorRegistry}'s
   * editor lookup facility, if available.
   */
  @Override
  @Nullable
  public PropertyEditor findEditor(@Nullable String field, @Nullable Class<?> valueType) {
    PropertyEditorRegistry editorRegistry = getPropertyEditorRegistry();
    if (editorRegistry != null) {
      Class<?> valueTypeToUse = valueType;
      if (valueTypeToUse == null) {
        valueTypeToUse = getFieldType(field);
      }
      return editorRegistry.findCustomEditor(valueTypeToUse, fixedField(field));
    }
    else {
      return null;
    }
  }

  /**
   * This implementation returns {@code null}.
   */
  @Override
  @Nullable
  public PropertyEditorRegistry getPropertyEditorRegistry() {
    return null;
  }

  @Override
  public String[] resolveMessageCodes(String errorCode) {
    return getMessageCodesResolver().resolveMessageCodes(errorCode, getObjectName());
  }

  @Override
  public String[] resolveMessageCodes(String errorCode, @Nullable String field) {
    return getMessageCodesResolver().resolveMessageCodes(
            errorCode, getObjectName(), fixedField(field), getFieldType(field));
  }

  @Override
  public void addError(ObjectError error) {
    this.errors.add(error);
  }

  @Override
  public void recordFieldValue(String field, Class<?> type, @Nullable Object value) {
    this.fieldTypes.put(field, type);
    this.fieldValues.put(field, value);
  }

  /**
   * Mark the specified disallowed field as suppressed.
   * <p>The data binder invokes this for each field value that was
   * detected to target a disallowed field.
   *
   * @see DataBinder#setAllowedFields
   */
  @Override
  public void recordSuppressedField(String field) {
    this.suppressedFields.add(field);
  }

  /**
   * Return the list of fields that were suppressed during the bind process.
   * <p>Can be used to determine whether any field values were targeting
   * disallowed fields.
   *
   * @see DataBinder#setAllowedFields
   */
  @Override
  public String[] getSuppressedFields() {
    return StringUtils.toStringArray(this.suppressedFields);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BindingResult otherResult)) {
      return false;
    }
    return (getObjectName().equals(otherResult.getObjectName())
            && ObjectUtils.nullSafeEquals(getTarget(), otherResult.getTarget())
            && getAllErrors().equals(otherResult.getAllErrors()));
  }

  @Override
  public int hashCode() {
    return getObjectName().hashCode();
  }

  //---------------------------------------------------------------------
  // Template methods to be implemented/overridden by subclasses
  //---------------------------------------------------------------------

  /**
   * Return the wrapped target object.
   */
  @Override
  @Nullable
  public abstract Object getTarget();

  /**
   * Extract the actual field value for the given field.
   *
   * @param field the field to check
   * @return the current value of the field
   */
  @Nullable
  protected abstract Object getActualFieldValue(String field);

  /**
   * Format the given value for the specified field.
   * <p>The default implementation simply returns the field value as-is.
   *
   * @param field the field to check
   * @param value the value of the field (either a rejected value
   * other than from a binding error, or an actual field value)
   * @return the formatted value
   */
  @Nullable
  protected Object formatFieldValue(String field, @Nullable Object value) {
    return value;
  }

}
