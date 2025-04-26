/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.validation;

import java.beans.PropertyEditor;
import java.util.Map;

import infra.beans.PropertyEditorRegistry;
import infra.lang.Nullable;

/**
 * General interface that represents binding results. Extends the
 * {@link Errors interface} for error registration capabilities,
 * allowing for a {@link Validator} to be applied, and adds
 * binding-specific analysis and model building.
 *
 * <p>Serves as result holder for a {@link DataBinder}, obtained via
 * the {@link DataBinder#getBindingResult()} method. BindingResult
 * implementations can also be used directly, for example to invoke
 * a {@link Validator} on it (e.g. as part of a unit test).
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see DataBinder
 * @see Errors
 * @see Validator
 * @see BeanPropertyBindingResult
 * @see DirectFieldBindingResult
 * @see MapBindingResult
 * @since 4.0
 */
public interface BindingResult extends Errors {

  /**
   * Prefix for the name of the BindingResult instance in a model,
   * followed by the object name.
   */
  String MODEL_KEY_PREFIX = BindingResult.class.getName() + ".";

  /**
   * Return the wrapped target object, which may be a bean, an object with
   * public fields, a Map - depending on the concrete binding strategy.
   */
  @Nullable
  Object getTarget();

  /**
   * Return a model Map for the obtained state, exposing a BindingResult
   * instance as '{@link #MODEL_KEY_PREFIX MODEL_KEY_PREFIX} + objectName'
   * and the object itself as 'objectName'.
   * <p>Note that the Map is constructed every time you're calling this method.
   * Adding things to the map and then re-calling this method will not work.
   * <p>The attributes in the model Map returned by this method are usually
   * included in the {@link infra.web.view.ModelAndView}
   * for a form view that uses Framework's {@code bind} tag in a JSP,
   * which needs access to the BindingResult instance. Framework's pre-built
   * form controllers will do this for you when rendering a form view.
   * When building the ModelAndView instance yourself, you need to include
   * the attributes from the model Map returned by this method.
   *
   * @see #getObjectName()
   * @see #MODEL_KEY_PREFIX
   * @see infra.web.view.ModelAndView
   */
  Map<String, Object> getModel();

  /**
   * Extract the raw field value for the given field.
   * Typically used for comparison purposes.
   *
   * @param field the field to check
   * @return the current value of the field in its raw form, or {@code null} if not known
   */
  @Nullable
  Object getRawFieldValue(String field);

  /**
   * Find a custom property editor for the given type and property.
   *
   * @param field the path of the property (name or nested path), or
   * {@code null} if looking for an editor for all properties of the given type
   * @param valueType the type of the property (can be {@code null} if a property
   * is given but should be specified in any case for consistency checking)
   * @return the registered editor, or {@code null} if none
   */
  @Nullable
  PropertyEditor findEditor(@Nullable String field, @Nullable Class<?> valueType);

  /**
   * Return the underlying PropertyEditorRegistry.
   *
   * @return the PropertyEditorRegistry, or {@code null} if none
   * available for this BindingResult
   */
  @Nullable
  PropertyEditorRegistry getPropertyEditorRegistry();

  /**
   * Resolve the given error code into message codes.
   * <p>Calls the configured {@link MessageCodesResolver} with appropriate parameters.
   *
   * @param errorCode the error code to resolve into message codes
   * @return the resolved message codes
   */
  String[] resolveMessageCodes(String errorCode);

  /**
   * Resolve the given error code into message codes for the given field.
   * <p>Calls the configured {@link MessageCodesResolver} with appropriate parameters.
   *
   * @param errorCode the error code to resolve into message codes
   * @param field the field to resolve message codes for
   * @return the resolved message codes
   */
  String[] resolveMessageCodes(String errorCode, String field);

  /**
   * Add a custom {@link ObjectError} or {@link FieldError} to the errors list.
   * <p>Intended to be used by cooperating strategies such as {@link BindingErrorProcessor}.
   *
   * @see ObjectError
   * @see FieldError
   * @see BindingErrorProcessor
   */
  void addError(ObjectError error);

  /**
   * Record the given value for the specified field.
   * <p>To be used when a target object cannot be constructed, making
   * the original field values available through {@link #getFieldValue}.
   * In case of a registered error, the rejected value will be exposed
   * for each affected field.
   *
   * @param field the field to record the value for
   * @param type the type of the field
   * @param value the original value
   * @since 4.0
   */
  default void recordFieldValue(String field, Class<?> type, @Nullable Object value) {
  }

  /**
   * Mark the specified disallowed field as suppressed.
   * <p>The data binder invokes this for each field value that was
   * detected to target a disallowed field.
   *
   * @see DataBinder#setAllowedFields
   */
  default void recordSuppressedField(String field) {
  }

  /**
   * Return the list of fields that were suppressed during the bind process.
   * <p>Can be used to determine whether any field values were targeting
   * disallowed fields.
   *
   * @see DataBinder#setAllowedFields
   */
  default String[] getSuppressedFields() {
    return new String[0];
  }

}
