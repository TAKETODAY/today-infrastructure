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

import java.util.Collection;
import java.util.List;

import cn.taketoday.context.MessageSourceResolvable;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import jakarta.validation.ConstraintViolation;

/**
 * Store and expose the results of method validation via
 * {@link jakarta.validation.Validator} for a specific method parameter.
 * <ul>
 * <li>For a constraints directly on a method parameter, each
 * {@link ConstraintViolation} is adapted to {@link MessageSourceResolvable}.
 * <li>For cascaded constraints via {@link jakarta.validation.Validator @Valid}
 * on a bean method parameter, {@link InfraValidatorAdapter} is used to initialize
 * an {@link cn.taketoday.validation.Errors} with field errors, and create
 * the {@link ParameterErrors} sub-class.
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ParameterValidationResult {

  private final MethodParameter methodParameter;

  @Nullable
  private final Object argument;

  private final List<MessageSourceResolvable> resolvableErrors;

  private final List<ConstraintViolation<Object>> violations;

  /**
   * Create a {@code ParameterValidationResult}.
   */
  public ParameterValidationResult(MethodParameter methodParameter, @Nullable Object argument,
          Collection<? extends MessageSourceResolvable> resolvableErrors,
          Collection<ConstraintViolation<Object>> violations) {

    Assert.notNull(methodParameter, "MethodParameter is required");
    Assert.notEmpty(resolvableErrors, "`resolvableErrors` must not be empty");
    Assert.notEmpty(violations, "'violations' must not be empty");
    this.methodParameter = methodParameter;
    this.argument = argument;
    this.resolvableErrors = List.copyOf(resolvableErrors);
    this.violations = List.copyOf(violations);
  }

  /**
   * The method parameter the validation results are for.
   */
  public MethodParameter getMethodParameter() {
    return this.methodParameter;
  }

  /**
   * The method argument value that was validated.
   */
  @Nullable
  public Object getArgument() {
    return this.argument;
  }

  /**
   * List of {@link MessageSourceResolvable} representations adapted from the
   * underlying {@link #getViolations() violations}.
   * <ul>
   * <li>For a constraints directly on a method parameter, error codes are
   * based on the names of the constraint annotation, the object, the method,
   * the parameter, and parameter type, e.g.
   * {@code ["Max.myObject#myMethod.myParameter", "Max.myParameter", "Max.int", "Max"]}.
   * Arguments include the parameter itself as a {@link MessageSourceResolvable}, e.g.
   * {@code ["myObject#myMethod.myParameter", "myParameter"]}, followed by actual
   * constraint annotation attributes (i.e. excluding "message", "groups" and
   * "payload") in alphabetical order of attribute names.
   * <li>For cascaded constraints via {@link jakarta.validation.Validator @Valid}
   * on a bean method parameter, this method returns
   * {@link cn.taketoday.validation.FieldError field errors} that you
   * can also access more conveniently through methods of the
   * {@link ParameterErrors} sub-class.
   * </ul>
   */
  public List<MessageSourceResolvable> getResolvableErrors() {
    return this.resolvableErrors;
  }

  /**
   * The violations associated with the method parameter, in the same order
   * as {@link #getResolvableErrors()}.
   */
  public List<ConstraintViolation<Object>> getViolations() {
    return this.violations;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!super.equals(other)) {
      return false;
    }
    ParameterValidationResult otherResult = (ParameterValidationResult) other;
    return getMethodParameter().equals(otherResult.getMethodParameter())
            && ObjectUtils.nullSafeEquals(getArgument(), otherResult.getArgument())
            && getViolations().equals(otherResult.getViolations());
  }

  @Override
  public int hashCode() {
    int hashCode = super.hashCode();
    hashCode = 29 * hashCode + getMethodParameter().hashCode();
    hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getArgument());
    hashCode = 29 * hashCode + (getViolations().hashCode());
    return hashCode;
  }

  @Override
  public String toString() {
    return "Validation results for method parameter '" + this.methodParameter +
            "': argument [" + ObjectUtils.nullSafeConciseToString(this.argument) + "]; " +
            getResolvableErrors();
  }

}
