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

package cn.taketoday.validation.beanvalidation;

import java.lang.reflect.Method;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.lang.Nullable;

/**
 * Contract to apply method validation without directly using
 * {@link MethodValidationAdapter}. For use in components where Jakarta Bean
 * Validation is an optional dependency and may or may not be present on the
 * classpath. If that's not a concern, use {@code MethodValidationAdapter}
 * directly.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultMethodValidator
 * @since 4.0
 */
public interface MethodValidator {

  /**
   * Use this method determine the validation groups to pass into
   * {@link #validateArguments(Object, Method, MethodParameter[], Object[], Class[])} and
   * {@link #validateReturnValue(Object, Method, MethodParameter, Object, Class[])}.
   *
   * @param target the target Object
   * @param method the target method
   * @return the applicable validation groups as a {@code Class} array
   * @see MethodValidationAdapter#determineValidationGroups(Object, Method)
   */
  Class<?>[] determineValidationGroups(Object target, Method method);

  /**
   * Validate the given method arguments and return the result of validation.
   *
   * @param target the target Object
   * @param method the target method
   * @param parameters the parameters, if already created and available
   * @param arguments the candidate argument values to validate
   * @param groups groups for validation determined via
   * {@link #determineValidationGroups(Object, Method)}
   * @throws MethodValidationException should be raised in case of validation
   * errors unless the implementation handles those errors otherwise (e.g.
   * by injecting {@code BindingResult} into the method).
   */
  void validateArguments(Object target, Method method,
          @Nullable MethodParameter[] parameters, Object[] arguments, Class<?>[] groups);

  /**
   * Validate the given return value and return the result of validation.
   *
   * @param target the target Object
   * @param method the target method
   * @param returnType the return parameter, if already created and available
   * @param returnValue the return value to validate
   * @param groups groups for validation determined via
   * {@link #determineValidationGroups(Object, Method)}
   * @throws MethodValidationException in case of validation errors
   */
  void validateReturnValue(Object target, Method method,
          @Nullable MethodParameter returnType, @Nullable Object returnValue, Class<?>[] groups);

}
