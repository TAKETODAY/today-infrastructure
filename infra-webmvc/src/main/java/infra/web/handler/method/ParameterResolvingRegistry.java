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

import infra.core.ArraySizeTrimmer;
import infra.core.style.ToStringBuilder;

/**
 * Registry for managing and providing access to {@link ParameterResolvingStrategy} instances.
 * Searches customized strategies before default strategies, returning the first match
 * in registration order within each group. Both groups are initially empty.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ParameterResolvingStrategy
 * @since 3.0 2019-07-07 23:24
 */
public class ParameterResolvingRegistry implements ArraySizeTrimmer {

  private final ParameterResolvingStrategies defaultStrategies = new ParameterResolvingStrategies(36);

  private final ParameterResolvingStrategies customizedStrategies = new ParameterResolvingStrategies();

  /**
   * Return the mutable collection of default strategies.
   */
  public ParameterResolvingStrategies getDefaultStrategies() {
    return defaultStrategies;
  }

  /**
   * get customized resolving-strategies
   *
   * @since 4.0
   */
  public ParameterResolvingStrategies getCustomizedStrategies() {
    return customizedStrategies;
  }

  /**
   * Find a suitable {@link ParameterResolvingStrategy} for given {@link HandlerParameter}
   *
   * @param resolvable resolvable MethodParameter
   * @return a suitable {@link ParameterResolvingStrategy},
   * if returns {@code null} no suitable  {@link ParameterResolvingStrategy}
   */
  protected @Nullable ParameterResolvingStrategy lookupStrategy(HandlerParameter resolvable, Iterable<ParameterResolvingStrategy> strategies) {
    for (ParameterResolvingStrategy resolver : strategies) {
      if (resolver.supportsParameter(resolvable)) {
        return resolver;
      }
    }
    return null;
  }

  /**
   * find parameter resolving strategies
   * <p>
   * find in {@code customizedResolvers} and when find in {@code defaultResolvers}
   *
   * @param parameter parameter value to be resolve
   * @return A suitable {@link ParameterResolvingStrategy}
   */
  public @Nullable ParameterResolvingStrategy findStrategy(HandlerParameter parameter) {
    ParameterResolvingStrategy resolvingStrategy = lookupStrategy(parameter, customizedStrategies);
    if (resolvingStrategy == null) {
      resolvingStrategy = lookupStrategy(parameter, defaultStrategies);
    }
    return resolvingStrategy;
  }

  /**
   * Get correspond parameter resolver, If there isn't a suitable resolver will be
   * throws {@link ParameterResolverNotFoundException}
   *
   * @return A suitable {@link ParameterResolvingStrategy}
   * @throws ParameterResolverNotFoundException If there isn't a suitable resolver
   */
  public ParameterResolvingStrategy obtainStrategy(HandlerParameter parameter) {
    ParameterResolvingStrategy resolver = findStrategy(parameter);
    if (resolver == null) {
      throw new ParameterResolverNotFoundException(
              parameter, "There isn't have a parameter resolver to resolve parameter: [%s] called: [%s] on %s"
              .formatted(parameter.getParameterType(), parameter.getName(), parameter.getMethod()));
    }
    return resolver;
  }

  /**
   * Returns <tt>true</tt> if resolvers list contains the specified {@code resolverClass}.
   * More formally, returns <tt>true</tt> if and only if all resolvers contains
   * at least one element <tt>e</tt> such that
   * <tt>(resolverClass == resolver.getClass())</tt>.
   *
   * @param resolverClass element whose presence in this defaultResolvers or customizedResolvers is to be tested
   * @return <tt>true</tt> if resolvers contains the specified {@code resolverClass}
   */
  public boolean contains(Class<?> resolverClass) {
    return defaultStrategies.contains(resolverClass)
            || customizedStrategies.contains(resolverClass);
  }

  //

  /**
   * Add one or more customized parameter resolving strategies to the registry.
   *
   * @param strategies the parameter resolving strategies to add
   * @since 4.0
   */
  public void addCustomizedStrategies(ParameterResolvingStrategy @Nullable ... strategies) {
    customizedStrategies.add(strategies);
  }

  /**
   * Add one or more default parameter resolving strategies to the registry.
   *
   * @param strategies the parameter resolving strategies to add
   * @since 4.0
   */
  public void addDefaultStrategies(ParameterResolvingStrategy @Nullable ... strategies) {
    defaultStrategies.add(strategies);
  }

  /**
   * @since 4.0
   */
  @Override
  public void trimToSize() {
    defaultStrategies.trimToSize();
    customizedStrategies.trimToSize();
  }

  @Override
  public String toString() {
    return ToStringBuilder.forInstance(this)
            .append("defaultStrategies", defaultStrategies.size())
            .append("customizedStrategies", customizedStrategies.size())
            .toString();
  }

}
