/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import cn.taketoday.beans.PropertyAccessException;
import cn.taketoday.context.support.DefaultMessageSourceResolvable;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Default {@link BindingErrorProcessor} implementation.
 *
 * <p>Uses the "required" error code and the field name to resolve message codes
 * for a missing field error.
 *
 * <p>Creates a {@code FieldError} for each {@code PropertyAccessException}
 * given, using the {@code PropertyAccessException}'s error code ("typeMismatch",
 * "methodInvocation") for resolving message codes.
 *
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @see #MISSING_FIELD_ERROR_CODE
 * @see DataBinder#setBindingErrorProcessor
 * @see BeanPropertyBindingResult#addError
 * @see BeanPropertyBindingResult#resolveMessageCodes
 * @see cn.taketoday.beans.PropertyAccessException#getErrorCode
 * @see cn.taketoday.beans.TypeMismatchException#ERROR_CODE
 * @see cn.taketoday.beans.MethodInvocationException#ERROR_CODE
 * @since 4.0
 */
public class DefaultBindingErrorProcessor implements BindingErrorProcessor {

  /**
   * Error code that a missing field error (i.e. a required field not
   * found in the list of property values) will be registered with:
   * "required".
   */
  public static final String MISSING_FIELD_ERROR_CODE = "required";

  @Override
  public void processMissingFieldError(String missingField, BindingResult bindingResult) {
    // Create field error with code "required".
    String fixedField = bindingResult.getNestedPath() + missingField;
    String[] codes = bindingResult.resolveMessageCodes(MISSING_FIELD_ERROR_CODE, missingField);
    Object[] arguments = getArgumentsForBindError(bindingResult.getObjectName(), fixedField);
    FieldError error = new FieldError(bindingResult.getObjectName(), fixedField, "", true,
            codes, arguments, "Field '" + fixedField + "' is required");
    bindingResult.addError(error);
  }

  @Override
  public void processPropertyAccessException(PropertyAccessException ex, BindingResult bindingResult) {
    // Create field error with the exceptions's code, e.g. "typeMismatch".
    String field = ex.getPropertyName();
    Assert.state(field != null, "No field in exception");
    String[] codes = bindingResult.resolveMessageCodes(ex.getErrorCode(), field);
    Object[] arguments = getArgumentsForBindError(bindingResult.getObjectName(), field);
    Object rejectedValue = ex.getValue();
    if (ObjectUtils.isArray(rejectedValue)) {
      rejectedValue = StringUtils.arrayToString(ObjectUtils.toObjectArray(rejectedValue));
    }
    FieldError error = new FieldError(bindingResult.getObjectName(), field, rejectedValue, true,
            codes, arguments, ex.getLocalizedMessage());
    error.wrap(ex);
    bindingResult.addError(error);
  }

  /**
   * Return FieldError arguments for a binding error on the given field.
   * Invoked for each missing required field and each type mismatch.
   * <p>The default implementation returns a single argument indicating the field name
   * (of type DefaultMessageSourceResolvable, with "objectName.field" and "field" as codes).
   *
   * @param objectName the name of the target object
   * @param field the field that caused the binding error
   * @return the Object array that represents the FieldError arguments
   * @see cn.taketoday.validation.FieldError#getArguments
   * @see cn.taketoday.context.support.DefaultMessageSourceResolvable
   */
  protected Object[] getArgumentsForBindError(String objectName, String field) {
    String[] codes = new String[] { objectName + Errors.NESTED_PATH_SEPARATOR + field, field };
    return new Object[] { new DefaultMessageSourceResolvable(codes, field) };
  }

}
