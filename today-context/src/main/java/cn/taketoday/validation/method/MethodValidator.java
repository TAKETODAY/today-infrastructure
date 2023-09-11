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

import java.lang.reflect.Method;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.lang.Nullable;

/**
 * Contract to apply method validation and handle the results.
 * Exposes methods that return {@link MethodValidationResult}, and methods that
 * handle the results, by default raising {@link MethodValidationException}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface MethodValidator {

  /**
   * Determine the applicable validation groups. By default, obtained from an
   * {@link cn.taketoday.validation.annotation.Validated @Validated}
   * annotation on the method, or on the class level.
   *
   * @param target the target Object
   * @param method the target method
   * @return the applicable validation groups as a {@code Class} array
   */
  Class<?>[] determineValidationGroups(Object target, Method method);

  /**
   * Validate the given method arguments and return validation results.
   *
   * @param target the target Object
   * @param method the target method
   * @param parameters the parameters, if already created and available
   * @param arguments the candidate argument values to validate
   * @param groups validation groups from {@link #determineValidationGroups}
   * @return the result of validation
   */
  MethodValidationResult validateArguments(Object target, Method method,
          @Nullable MethodParameter[] parameters, Object[] arguments, Class<?>[] groups);

  /**
   * Delegate to {@link #validateArguments} and handle the validation result,
   * by default raising {@link MethodValidationException} in case of errors.
   * Implementations may provide alternative handling, e.g. injecting
   * {@link cn.taketoday.validation.Errors} into the method.
   *
   * @throws MethodValidationException in case of unhandled errors.
   */
  default void applyArgumentValidation(Object target, Method method,
          @Nullable MethodParameter[] parameters, Object[] arguments, Class<?>[] groups) {

    MethodValidationResult result = validateArguments(target, method, parameters, arguments, groups);
    if (result.hasErrors()) {
      throw new MethodValidationException(result);
    }
  }

  /**
   * Validate the given return value and return validation results.
   *
   * @param target the target Object
   * @param method the target method
   * @param returnType the return parameter, if already created and available
   * @param returnValue the return value to validate
   * @param groups validation groups from {@link #determineValidationGroups}
   * @return the result of validation
   */
  MethodValidationResult validateReturnValue(Object target, Method method,
          @Nullable MethodParameter returnType, @Nullable Object returnValue, Class<?>[] groups);

  /**
   * Delegate to {@link #validateReturnValue} and handle the validation result,
   * by default raising {@link MethodValidationException} in case of errors.
   * Implementations may provide alternative handling.
   *
   * @throws MethodValidationException in case of unhandled errors.
   */
  default void applyReturnValueValidation(Object target, Method method,
          @Nullable MethodParameter returnType, @Nullable Object returnValue, Class<?>[] groups) {

    MethodValidationResult result = validateReturnValue(target, method, returnType, returnValue, groups);
    if (result.hasErrors()) {
      throw new MethodValidationException(result);
    }
  }

}
