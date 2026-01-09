/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.validation.method;

import java.lang.reflect.Method;
import java.util.List;

import infra.context.MessageSourceResolvable;
import infra.lang.Assert;

/**
 * Exception that is a {@link MethodValidationResult}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
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
  public List<ParameterValidationResult> getParameterValidationResults() {
    return this.validationResult.getParameterValidationResults();
  }

  @Override
  public List<MessageSourceResolvable> getCrossParameterValidationResults() {
    return this.validationResult.getCrossParameterValidationResults();
  }

}
