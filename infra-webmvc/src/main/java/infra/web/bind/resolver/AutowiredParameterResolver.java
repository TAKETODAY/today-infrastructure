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

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.config.DependencyDescriptor;
import infra.beans.factory.support.DependencyInjector;
import infra.beans.factory.support.DependencyInjectorProvider;
import infra.web.RequestContext;
import infra.web.handler.method.ResolvableMethodParameter;

/**
 * Resolves method parameters annotated with {@link Autowired} by injecting dependencies
 * from the application context.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 3.0 2021/4/2 23:21
 */
public class AutowiredParameterResolver implements ParameterResolvingStrategy {

  private final DependencyInjector injector;

  public AutowiredParameterResolver(DependencyInjectorProvider provider) {
    this.injector = provider.getInjector();
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    return resolvable.hasParameterAnnotation(Autowired.class);
  }

  @Override
  public @Nullable Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    return injector.resolveValue(new DependencyDescriptor(resolvable.getParameter(), resolvable.isRequired()));
  }

}
