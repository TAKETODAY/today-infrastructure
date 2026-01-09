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

package infra.web.bind;

import org.jspecify.annotations.Nullable;

import infra.core.MethodParameter;

/**
 * {@link RequestBindingException} subclass that indicates a missing parameter.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 23:20
 */
public class MissingRequestParameterException extends MissingRequestValueException {

  private final String parameterName;

  private final String parameterType;

  @Nullable
  private final MethodParameter parameter;

  /**
   * Constructor for MissingRequestParameterException.
   *
   * @param parameterName the name of the missing parameter
   * @param parameterType the expected type of the missing parameter
   */
  public MissingRequestParameterException(String parameterName, String parameterType) {
    super("", false, null, new Object[] { parameterName });
    this.parameterName = parameterName;
    this.parameterType = parameterType;
    this.parameter = null;
    getBody().setDetail(initBodyDetail(this.parameterName));
  }

  /**
   * Constructor with a {@link MethodParameter} instead of a String parameterType.
   *
   * @param parameterName the name of the missing parameter
   * @param parameter the target method parameter for the missing value
   * @param missingAfterConversion whether the value became null after conversion
   * @since 5.0
   */
  public MissingRequestParameterException(String parameterName, MethodParameter parameter, boolean missingAfterConversion) {
    super("", missingAfterConversion, null, new Object[] { parameterName });
    this.parameterName = parameterName;
    this.parameterType = parameter.getNestedParameterType().getSimpleName();
    this.parameter = parameter;
    getBody().setDetail(initBodyDetail(this.parameterName));
  }

  private static String initBodyDetail(String name) {
    return "Required parameter '" + name + "' is not present.";
  }

  @Override
  public String getMessage() {
    return "Required request parameter '%s' for method parameter type %s is %s"
            .formatted(this.parameterName, this.parameterType, isMissingAfterConversion() ? "present but converted to null" : "not present");
  }

  /**
   * Return the name of the offending parameter.
   */
  public final String getParameterName() {
    return this.parameterName;
  }

  /**
   * Return the expected type of the offending parameter.
   */
  public final String getParameterType() {
    return this.parameterType;
  }

  /**
   * Return the target {@link MethodParameter} if the exception was raised for
   * a controller method argument.
   *
   * @since 5.0
   */
  @Nullable
  public MethodParameter getMethodParameter() {
    return this.parameter;
  }

}
