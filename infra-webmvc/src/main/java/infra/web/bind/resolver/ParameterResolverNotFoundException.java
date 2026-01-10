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

package infra.web.bind.resolver;

import org.jspecify.annotations.Nullable;

import infra.web.InfraConfigurationException;
import infra.web.handler.method.ResolvableMethodParameter;

/**
 * For {@link ParameterResolvingStrategy} NotFound Exception
 *
 * @author TODAY 2021/9/9 22:36
 * @see ParameterResolvingStrategy
 * @since 4.0
 */
public class ParameterResolverNotFoundException extends InfraConfigurationException {

  private final ResolvableMethodParameter parameter;

  public ParameterResolverNotFoundException(ResolvableMethodParameter parameter, @Nullable String message) {
    this(parameter, message, null);
  }

  public ParameterResolverNotFoundException(ResolvableMethodParameter parameter, @Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
    this.parameter = parameter;
  }

  public ResolvableMethodParameter getParameter() {
    return parameter;
  }

  public String getParameterName() {
    return parameter.getName();
  }

}
