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

import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;

/**
 * Container for method validation results where underlying
 * {@link ConstraintViolation violations} have been adapted to
 * {@link ParameterValidationResult} each containing a list of
 * {@link cn.taketoday.context.MessageSourceResolvable} grouped by method
 * parameter.
 *
 * <p>For {@link jakarta.validation.Valid @Valid}-annotated, Object method
 * parameters or return types with cascaded violations, the {@link ParameterErrors}
 * subclass of {@link ParameterValidationResult} implements
 * {@link cn.taketoday.validation.Errors} and exposes
 * {@link cn.taketoday.validation.FieldError field errors}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface MethodValidationResult {

  /**
   * Returns the set of constraint violations reported during a validation.
   *
   * @return the {@code Set} of {@link ConstraintViolation}s, or an empty Set
   */
  Set<ConstraintViolation<?>> getConstraintViolations();

  /**
   * Return all validation results. This includes method parameters with
   * constraints declared on them, as well as
   * {@link jakarta.validation.Valid @Valid} method parameters with
   * cascaded constraints.
   *
   * @see #getValueResults()
   * @see #getBeanResults()
   */
  List<ParameterValidationResult> getAllValidationResults();

  /**
   * Return only validation results for method parameters with constraints
   * declared directly on them. This excludes
   * {@link jakarta.validation.Valid @Valid} method parameters with cascaded
   * constraints.
   *
   * @see #getAllValidationResults()
   */
  List<ParameterValidationResult> getValueResults();

  /**
   * Return only validation results for {@link jakarta.validation.Valid @Valid}
   * method parameters with cascaded constraints. This excludes method
   * parameters with constraints declared directly on them.
   *
   * @see #getAllValidationResults()
   */
  List<ParameterErrors> getBeanResults();

  /**
   * Check if {@link #getConstraintViolations()} is empty, and if not, raise
   * {@link MethodValidationException}.
   *
   * @throws MethodValidationException if the result contains any violations
   */
  void throwIfViolationsPresent();

}
