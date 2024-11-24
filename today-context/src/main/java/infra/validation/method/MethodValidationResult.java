/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.validation.method;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import infra.context.MessageSourceResolvable;
import infra.validation.Errors;
import infra.validation.FieldError;
import jakarta.validation.ConstraintViolation;

/**
 * Container for method validation results where underlying
 * {@link ConstraintViolation violations} have been adapted to
 * {@link ParameterValidationResult} each containing a list of
 * {@link infra.context.MessageSourceResolvable} grouped by method
 * parameter.
 *
 * <p>For {@link jakarta.validation.Valid @Valid}-annotated, Object method
 * parameters or return types with cascaded violations, the {@link ParameterErrors}
 * subclass of {@link ParameterValidationResult} implements
 * {@link Errors} and exposes
 * {@link FieldError field errors}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface MethodValidationResult {

  /**
   * Return the target of the method invocation to which validation was applied.
   */
  Object getTarget();

  /**
   * Return the method to which validation was applied.
   */
  Method getMethod();

  /**
   * Whether the violations are for a return value.
   * If true the violations are from validating a return value.
   * If false the violations are from validating method arguments.
   */
  boolean isForReturnValue();

  /**
   * Whether the result contains any validation errors.
   */
  default boolean hasErrors() {
    return !getParameterValidationResults().isEmpty();
  }

  /**
   * Return a single list with all errors from all validation results.
   *
   * @see #getParameterValidationResults()
   * @see ParameterValidationResult#getResolvableErrors()
   */
  default List<? extends MessageSourceResolvable> getAllErrors() {
    return getParameterValidationResults().stream()
            .flatMap(result -> result.getResolvableErrors().stream())
            .toList();
  }

  /**
   * Return all validation results per method parameter, including both
   * {@link #getValueResults()} and {@link #getBeanResults()}.
   * <p>Use {@link #getCrossParameterValidationResults()} for access to errors
   * from cross-parameter validation.
   *
   * @see #getValueResults()
   * @see #getBeanResults()
   * @since 5.0
   */
  List<ParameterValidationResult> getParameterValidationResults();

  /**
   * Return the subset of {@link #getParameterValidationResults() allValidationResults}
   * that includes method parameters with validation errors directly on method
   * argument values. This excludes {@link #getBeanResults() beanResults} with
   * nested errors on their fields and properties.
   */
  default List<ParameterValidationResult> getValueResults() {
    return getParameterValidationResults().stream()
            .filter(result -> !(result instanceof ParameterErrors))
            .toList();
  }

  /**
   * Return the subset of {@link #getParameterValidationResults() allValidationResults}
   * that includes Object method parameters with nested errors on their fields
   * and properties. This excludes {@link #getValueResults() valueResults} with
   * validation errors directly on method arguments.
   */
  default List<ParameterErrors> getBeanResults() {
    return getParameterValidationResults().stream()
            .filter(ParameterErrors.class::isInstance)
            .map(result -> (ParameterErrors) result)
            .toList();
  }

  /**
   * Return errors from cross-parameter validation.
   *
   * @since 5.0
   */
  List<MessageSourceResolvable> getCrossParameterValidationResults();

  /**
   * Factory method to create a {@link MethodValidationResult} instance.
   *
   * @param target the target Object
   * @param method the target method
   * @param results method validation results, expected to be non-empty
   * @return the created instance
   */
  static MethodValidationResult create(Object target, Method method, List<ParameterValidationResult> results) {
    return create(target, method, results, Collections.emptyList());
  }

  /**
   * Factory method to create a {@link MethodValidationResult} instance.
   *
   * @param target the target Object
   * @param method the target method
   * @param results method validation results, expected to be non-empty
   * @param crossParameterErrors cross-parameter validation errors
   * @return the created instance
   * @since 5.0
   */
  static MethodValidationResult create(Object target, Method method,
          List<ParameterValidationResult> results, List<MessageSourceResolvable> crossParameterErrors) {

    return new DefaultMethodValidationResult(target, method, results, crossParameterErrors);
  }

  /**
   * Factory method to create a {@link MethodValidationResult} instance with
   * 0 errors, suitable to use as a constant. Getters for a target object or
   * method are not supported.
   */
  static MethodValidationResult emptyResult() {
    return new EmptyMethodValidationResult();
  }

}
