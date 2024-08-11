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

package cn.taketoday.validation.method;

import java.lang.reflect.Method;
import java.util.List;

import cn.taketoday.context.MessageSourceResolvable;
import cn.taketoday.lang.Assert;

/**
 * Default {@link MethodValidationResult} implementation as a simple container.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class DefaultMethodValidationResult implements MethodValidationResult {

  private final Object target;

  private final Method method;

  private final List<ParameterValidationResult> parameterValidationResults;

  private final List<MessageSourceResolvable> crossParamResults;

  private final boolean forReturnValue;

  DefaultMethodValidationResult(Object target, Method method,
          List<ParameterValidationResult> results, List<MessageSourceResolvable> crossParamResults) {

    Assert.isTrue(!results.isEmpty() || !crossParamResults.isEmpty(), "Expected validation results");
    Assert.notNull(target, "'target' is required");
    Assert.notNull(method, "Method is required");

    this.target = target;
    this.method = method;
    this.parameterValidationResults = results;
    this.crossParamResults = crossParamResults;
    this.forReturnValue = (!results.isEmpty() && results.get(0).getMethodParameter().getParameterIndex() == -1);
  }

  @Override
  public Object getTarget() {
    return this.target;
  }

  @Override
  public Method getMethod() {
    return this.method;
  }

  @Override
  public boolean isForReturnValue() {
    return this.forReturnValue;
  }

  @Override
  public List<ParameterValidationResult> getParameterValidationResults() {
    return this.parameterValidationResults;
  }

  @Override
  public List<MessageSourceResolvable> getCrossParameterValidationResults() {
    return this.crossParamResults;
  }

  @Override
  public String toString() {
    return getAllErrors().size() + " validation errors " +
            "for " + (isForReturnValue() ? "return value" : "arguments") + " of " +
            this.method.toGenericString();
  }

}
