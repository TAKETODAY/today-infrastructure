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
 * Default implementation of {@link MethodValidator} that delegates to a
 * {@link MethodValidationAdapter}. Also, convenient as a base class that allows
 * handling of the validation result.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DefaultMethodValidator implements MethodValidator {

  private final MethodValidationAdapter adapter;

  public DefaultMethodValidator(MethodValidationAdapter adapter) {
    this.adapter = adapter;
  }

  @Override
  public Class<?>[] determineValidationGroups(Object bean, Method method) {
    return MethodValidationAdapter.determineValidationGroups(bean, method);
  }

  @Override
  public void validateArguments(Object target, Method method,
          @Nullable MethodParameter[] parameters, Object[] arguments, Class<?>[] groups) {

    handleArgumentsValidationResult(target, method, arguments, groups,
            this.adapter.validateMethodArguments(target, method, parameters, arguments, groups));
  }

  public void validateReturnValue(Object target, Method method,
          @Nullable MethodParameter returnType, @Nullable Object returnValue, Class<?>[] groups) {

    handleReturnValueValidationResult(target, method, returnValue, groups,
            this.adapter.validateMethodReturnValue(target, method, returnType, returnValue, groups));
  }

  /**
   * Subclasses can override this to handle the result of argument validation.
   * By default, {@link MethodValidationResult#throwIfViolationsPresent()} is called.
   *
   * @param bean the target Object for method invocation
   * @param method the target method
   * @param arguments the candidate argument values to validate
   * @param groups groups for validation determined via
   */
  protected void handleArgumentsValidationResult(Object bean, Method method,
          Object[] arguments, Class<?>[] groups, MethodValidationResult result) {

    result.throwIfViolationsPresent();
  }

  /**
   * Subclasses can override this to handle the result of return value validation.
   * By default, {@link MethodValidationResult#throwIfViolationsPresent()} is called.
   *
   * @param bean the target Object for method invocation
   * @param method the target method
   * @param returnValue the return value to validate
   * @param groups groups for validation determined via
   */
  protected void handleReturnValueValidationResult(Object bean, Method method,
          @Nullable Object returnValue, Class<?>[] groups, MethodValidationResult result) {

    result.throwIfViolationsPresent();
  }

}
