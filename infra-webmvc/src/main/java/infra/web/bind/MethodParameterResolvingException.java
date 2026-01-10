/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.bind;

import org.jspecify.annotations.Nullable;

import infra.core.MethodParameter;

/**
 * MethodParameter can't be resolved
 *
 * @author TODAY 2021/1/17 10:05
 * @since 3.0
 */
public class MethodParameterResolvingException extends RequestBindingException {

  private final MethodParameter parameter;

  public MethodParameterResolvingException(MethodParameter parameter) {
    this(parameter, null, null);
  }

  public MethodParameterResolvingException(MethodParameter parameter, @Nullable String message) {
    this(parameter, message, null);
  }

  public MethodParameterResolvingException(MethodParameter parameter, @Nullable Throwable cause) {
    this(parameter, null, cause);
  }

  public MethodParameterResolvingException(MethodParameter parameter, @Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
    this.parameter = parameter;
  }

  public MethodParameter getParameter() {
    return parameter;
  }

  @Nullable
  public String getParameterName() {
    return parameter.getParameterName();
  }

  public Class<?> getParameterType() {
    return parameter.getParameterType();
  }
}
