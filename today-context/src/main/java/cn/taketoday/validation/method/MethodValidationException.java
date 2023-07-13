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
import java.util.List;

import cn.taketoday.lang.Assert;

/**
 * Exception that is a {@link MethodValidationResult}.
 *
 * @author Rossen Stoyanchev
 * @see MethodValidator
 * @since 4.0
 */
public class MethodValidationException extends RuntimeException implements MethodValidationResult {

  private final MethodValidationResult validationResult;

  public MethodValidationException(MethodValidationResult validationResult) {
    super(validationResult.toString());
    Assert.notNull(validationResult, "MethodValidationResult is required");
    this.validationResult = validationResult;
  }

  @Override
  public Object getTarget() {
    return this.validationResult.getTarget();
  }

  @Override
  public Method getMethod() {
    return this.validationResult.getMethod();
  }

  @Override
  public boolean isForReturnValue() {
    return this.validationResult.isForReturnValue();
  }

  @Override
  public List<ParameterValidationResult> getAllValidationResults() {
    return this.validationResult.getAllValidationResults();
  }

}
