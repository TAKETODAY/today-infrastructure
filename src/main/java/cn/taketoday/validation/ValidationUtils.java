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

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Utility class offering convenient methods for invoking a {@link Validator}
 * and for rejecting empty fields.
 *
 * <p>Checks for an empty field in {@code Validator} implementations can become
 * one-liners when using {@link #rejectIfEmpty} or {@link #rejectIfEmptyOrWhitespace}.
 *
 * @author Juergen Hoeller
 * @author Dmitriy Kopylenko
 * @see Validator
 * @see Errors
 * @since 4.0
 */
public abstract class ValidationUtils {

  private static final Logger logger = LoggerFactory.getLogger(ValidationUtils.class);

  /**
   * Invoke the given {@link Validator} for the supplied object and
   * {@link Errors} instance.
   *
   * @param validator the {@code Validator} to be invoked
   * @param target the object to bind the parameters to
   * @param errors the {@link Errors} instance that should store the errors
   * @throws IllegalArgumentException if either of the {@code Validator} or {@code Errors}
   * arguments is {@code null}, or if the supplied {@code Validator} does not
   * {@link Validator#supports(Class) support} the validation of the supplied object's type
   */
  public static void invokeValidator(Validator validator, Object target, Errors errors) {
    invokeValidator(validator, target, errors, (Object[]) null);
  }

  /**
   * Invoke the given {@link Validator}/{@link SmartValidator} for the supplied object and
   * {@link Errors} instance.
   *
   * @param validator the {@code Validator} to be invoked
   * @param target the object to bind the parameters to
   * @param errors the {@link Errors} instance that should store the errors
   * @param validationHints one or more hint objects to be passed to the validation engine
   * @throws IllegalArgumentException if either of the {@code Validator} or {@code Errors}
   * arguments is {@code null}, or if the supplied {@code Validator} does not
   * {@link Validator#supports(Class) support} the validation of the supplied object's type
   */
  public static void invokeValidator(
          Validator validator, Object target, Errors errors, @Nullable Object... validationHints) {

    Assert.notNull(validator, "Validator must not be null");
    Assert.notNull(target, "Target object must not be null");
    Assert.notNull(errors, "Errors object must not be null");

    if (logger.isDebugEnabled()) {
      logger.debug("Invoking validator [{}]", validator);
    }
    if (!validator.supports(target.getClass())) {
      throw new IllegalArgumentException(
              "Validator [" + validator.getClass() + "] does not support [" + target.getClass() + "]");
    }

    if (!ObjectUtils.isEmpty(validationHints) && validator instanceof SmartValidator) {
      ((SmartValidator) validator).validate(target, errors, validationHints);
    }
    else {
      validator.validate(target, errors);
    }

    if (logger.isDebugEnabled()) {
      if (errors.hasErrors()) {
        logger.debug("Validator found {} errors", errors.getErrorCount());
      }
      else {
        logger.debug("Validator found no errors");
      }
    }
  }

  /**
   * Reject the given field with the given error code if the value is empty.
   * <p>An 'empty' value in this context means either {@code null} or
   * the empty string "".
   * <p>The object whose field is being validated does not need to be passed
   * in because the {@link Errors} instance can resolve field values by itself
   * (it will usually hold an internal reference to the target object).
   *
   * @param errors the {@code Errors} instance to register errors on
   * @param field the field name to check
   * @param errorCode the error code, interpretable as message key
   */
  public static void rejectIfEmpty(Errors errors, String field, String errorCode) {
    rejectIfEmpty(errors, field, errorCode, null, null);
  }

  /**
   * Reject the given field with the given error code and default message
   * if the value is empty.
   * <p>An 'empty' value in this context means either {@code null} or
   * the empty string "".
   * <p>The object whose field is being validated does not need to be passed
   * in because the {@link Errors} instance can resolve field values by itself
   * (it will usually hold an internal reference to the target object).
   *
   * @param errors the {@code Errors} instance to register errors on
   * @param field the field name to check
   * @param errorCode error code, interpretable as message key
   * @param defaultMessage fallback default message
   */
  public static void rejectIfEmpty(Errors errors, String field, String errorCode, String defaultMessage) {
    rejectIfEmpty(errors, field, errorCode, null, defaultMessage);
  }

  /**
   * Reject the given field with the given error code and error arguments
   * if the value is empty.
   * <p>An 'empty' value in this context means either {@code null} or
   * the empty string "".
   * <p>The object whose field is being validated does not need to be passed
   * in because the {@link Errors} instance can resolve field values by itself
   * (it will usually hold an internal reference to the target object).
   *
   * @param errors the {@code Errors} instance to register errors on
   * @param field the field name to check
   * @param errorCode the error code, interpretable as message key
   * @param errorArgs the error arguments, for argument binding via MessageFormat
   * (can be {@code null})
   */
  public static void rejectIfEmpty(Errors errors, String field, String errorCode, Object[] errorArgs) {
    rejectIfEmpty(errors, field, errorCode, errorArgs, null);
  }

  /**
   * Reject the given field with the given error code, error arguments
   * and default message if the value is empty.
   * <p>An 'empty' value in this context means either {@code null} or
   * the empty string "".
   * <p>The object whose field is being validated does not need to be passed
   * in because the {@link Errors} instance can resolve field values by itself
   * (it will usually hold an internal reference to the target object).
   *
   * @param errors the {@code Errors} instance to register errors on
   * @param field the field name to check
   * @param errorCode the error code, interpretable as message key
   * @param errorArgs the error arguments, for argument binding via MessageFormat
   * (can be {@code null})
   * @param defaultMessage fallback default message
   */
  public static void rejectIfEmpty(
          Errors errors, String field, String errorCode,
          @Nullable Object[] errorArgs, @Nullable String defaultMessage) {

    Assert.notNull(errors, "Errors object must not be null");
    Object value = errors.getFieldValue(field);
    if (value == null || StringUtils.isEmpty(value.toString())) {
      errors.rejectValue(field, errorCode, errorArgs, defaultMessage);
    }
  }

  /**
   * Reject the given field with the given error code if the value is empty
   * or just contains whitespace.
   * <p>An 'empty' value in this context means either {@code null},
   * the empty string "", or consisting wholly of whitespace.
   * <p>The object whose field is being validated does not need to be passed
   * in because the {@link Errors} instance can resolve field values by itself
   * (it will usually hold an internal reference to the target object).
   *
   * @param errors the {@code Errors} instance to register errors on
   * @param field the field name to check
   * @param errorCode the error code, interpretable as message key
   */
  public static void rejectIfEmptyOrWhitespace(Errors errors, String field, String errorCode) {
    rejectIfEmptyOrWhitespace(errors, field, errorCode, null, null);
  }

  /**
   * Reject the given field with the given error code and default message
   * if the value is empty or just contains whitespace.
   * <p>An 'empty' value in this context means either {@code null},
   * the empty string "", or consisting wholly of whitespace.
   * <p>The object whose field is being validated does not need to be passed
   * in because the {@link Errors} instance can resolve field values by itself
   * (it will usually hold an internal reference to the target object).
   *
   * @param errors the {@code Errors} instance to register errors on
   * @param field the field name to check
   * @param errorCode the error code, interpretable as message key
   * @param defaultMessage fallback default message
   */
  public static void rejectIfEmptyOrWhitespace(
          Errors errors, String field, String errorCode, String defaultMessage) {

    rejectIfEmptyOrWhitespace(errors, field, errorCode, null, defaultMessage);
  }

  /**
   * Reject the given field with the given error code and error arguments
   * if the value is empty or just contains whitespace.
   * <p>An 'empty' value in this context means either {@code null},
   * the empty string "", or consisting wholly of whitespace.
   * <p>The object whose field is being validated does not need to be passed
   * in because the {@link Errors} instance can resolve field values by itself
   * (it will usually hold an internal reference to the target object).
   *
   * @param errors the {@code Errors} instance to register errors on
   * @param field the field name to check
   * @param errorCode the error code, interpretable as message key
   * @param errorArgs the error arguments, for argument binding via MessageFormat
   * (can be {@code null})
   */
  public static void rejectIfEmptyOrWhitespace(
          Errors errors, String field, String errorCode, @Nullable Object[] errorArgs) {

    rejectIfEmptyOrWhitespace(errors, field, errorCode, errorArgs, null);
  }

  /**
   * Reject the given field with the given error code, error arguments
   * and default message if the value is empty or just contains whitespace.
   * <p>An 'empty' value in this context means either {@code null},
   * the empty string "", or consisting wholly of whitespace.
   * <p>The object whose field is being validated does not need to be passed
   * in because the {@link Errors} instance can resolve field values by itself
   * (it will usually hold an internal reference to the target object).
   *
   * @param errors the {@code Errors} instance to register errors on
   * @param field the field name to check
   * @param errorCode the error code, interpretable as message key
   * @param errorArgs the error arguments, for argument binding via MessageFormat
   * (can be {@code null})
   * @param defaultMessage fallback default message
   */
  public static void rejectIfEmptyOrWhitespace(
          Errors errors, String field, String errorCode,
          @Nullable Object[] errorArgs, @Nullable String defaultMessage) {

    Assert.notNull(errors, "Errors object must not be null");
    Object value = errors.getFieldValue(field);
    if (value == null || !StringUtils.hasText(value.toString())) {
      errors.rejectValue(field, errorCode, errorArgs, defaultMessage);
    }
  }

}
