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

package infra.web.handler.method;

import org.jspecify.annotations.Nullable;

import infra.core.MethodParameter;
import infra.web.RequestContext;
import infra.web.bind.resolver.ParameterResolvingRegistry;
import infra.web.bind.resolver.ParameterResolvingStrategy;

/**
 * @author TODAY 2020/9/26 20:06
 * @since 3.0
 */
final class ParameterResolverMethodParameter extends ResolvableMethodParameter {

  private final ParameterResolvingRegistry resolvers;

  @Nullable
  private ParameterResolvingStrategy strategy;

  ParameterResolverMethodParameter(MethodParameter parameter, ParameterResolvingRegistry resolvers) {
    super(parameter);
    this.resolvers = resolvers;
  }

  @Override
  @Nullable
  public Object resolveParameter(final RequestContext request) throws Throwable {
    ParameterResolvingStrategy strategy = this.strategy;
    if (strategy == null) {
      strategy = resolvers.obtainStrategy(this);
      this.strategy = strategy;
    }
    return strategy.resolveArgument(request, this);
  }

}
