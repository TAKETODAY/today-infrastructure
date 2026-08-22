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

import infra.core.MethodParameter;
import infra.core.ParameterNameDiscoverer;
import infra.util.Assert;
import infra.web.bind.resolver.ParameterResolvingRegistry;

/**
 * A {@link HandlerParameterFactory} implementation that utilizes a {@link ParameterResolvingRegistry}
 * to resolve method parameters. This factory creates {@link HandlerParameter} instances
 * capable of resolving arguments based on the registered resolvers.
 *
 * @author TODAY
 * @since 3.0.1
 */
public class RegistryHandlerParameterFactory extends HandlerParameterFactory {

  private final ParameterResolvingRegistry resolvingRegistry;

  public RegistryHandlerParameterFactory(ParameterResolvingRegistry resolvingRegistry) {
    Assert.notNull(resolvingRegistry, "ParameterResolvingRegistry is required");
    this.resolvingRegistry = resolvingRegistry;
  }

  public RegistryHandlerParameterFactory(ParameterResolvingRegistry registry, ParameterNameDiscoverer discoverer) {
    super(discoverer);
    Assert.notNull(registry, "ParameterResolvingRegistry is required");
    this.resolvingRegistry = registry;
  }

  @Override
  public HandlerParameter createParameter(MethodParameter parameter) {
    return new ParameterResolverMethodParameter(parameter, resolvingRegistry);
  }

}
