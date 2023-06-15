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

import java.io.Serial;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import cn.taketoday.lang.Assert;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * Extension of {@link ConstraintViolationException} that implements
 * {@link MethodValidationResult} exposing an additional list of
 * {@link ParameterValidationResult} that represents violations adapted to
 * {@link cn.taketoday.context.MessageSourceResolvable} and grouped by
 * method parameter.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ParameterValidationResult
 * @see ParameterErrors
 * @see MethodValidationAdapter
 * @since 4.0
 */
public class MethodValidationException extends ConstraintViolationException implements MethodValidationResult {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Object target;

  private final Method method;

  private final List<ParameterValidationResult> allValidationResults;

  private final boolean forReturnValue;

  public MethodValidationException(
          Object target, Method method, Set<? extends ConstraintViolation<?>> violations,
          List<ParameterValidationResult> validationResults, boolean forReturnValue) {

    super(violations);
    Assert.notEmpty(violations, "'violations' must not be empty");
    this.target = target;
    this.method = method;
    this.allValidationResults = validationResults;
    this.forReturnValue = forReturnValue;
  }

  /**
   * Return the target of the method invocation to which validation was applied.
   */
  public Object getTarget() {
    return this.target;
  }

  /**
   * Return the method to which validation was applied.
   */
  public Method getMethod() {
    return this.method;
  }

  /**
   * Whether the violations are for a return value.
   * If true the violations are from validating a return value.
   * If false the violations are from validating method arguments.
   */
  public boolean isForReturnValue() {
    return this.forReturnValue;
  }

  // re-declare parent class method for NonNull treatment of interface

  @Override
  public Set<ConstraintViolation<?>> getConstraintViolations() {
    return super.getConstraintViolations();
  }

  @Override
  public List<ParameterValidationResult> getAllValidationResults() {
    return this.allValidationResults;
  }

  @Override
  public List<ParameterValidationResult> getValueResults() {
    return this.allValidationResults.stream()
            .filter(result -> !(result instanceof ParameterErrors))
            .toList();
  }

  @Override
  public List<ParameterErrors> getBeanResults() {
    return this.allValidationResults.stream()
            .filter(result -> result instanceof ParameterErrors)
            .map(result -> (ParameterErrors) result)
            .toList();
  }

  @Override
  public void throwIfViolationsPresent() {
    throw this;
  }

  @Override
  public String toString() {
    return "MethodValidationResult (" + getConstraintViolations().size() + " violations) " +
            "for " + this.method.toGenericString();
  }

}
