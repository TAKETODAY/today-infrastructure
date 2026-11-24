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
 * Registry for managing and looking up {@link ParameterResolvingStrategy} instances.
 * Customized strategies are searched before default strategies, and the first
 * matching strategy in registration order is returned from each group. Both
 * groups are initially empty.
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
   *
   * <p>Strategies in this collection are consulted after customized strategies.
   *
   * @return the collection of default strategies
   */
  public ParameterResolvingStrategies getDefaultStrategies() {
    return defaultStrategies;
  }

  /**
   * Return the mutable collection of customized strategies.
   *
   * <p>Strategies in this collection are consulted before default strategies.
   *
   * @return the collection of customized strategies
   * @since 4.0
   */
  public ParameterResolvingStrategies getCustomizedStrategies() {
    return customizedStrategies;
  }

  /**
   * Find the first strategy in the specified collection that supports the given
   * parameter.
   *
   * @param resolvable the parameter to inspect
   * @param strategies the strategies to search, in priority order
   * @return the first strategy that supports the parameter, or {@code null} if
   * none of the strategies supports it
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
   * Find a strategy for the given parameter.
   *
   * <p>Customized strategies are searched first, followed by default
   * strategies. Within each collection, strategies are searched in registration
   * order.
   *
   * @param parameter the parameter to resolve
   * @return the first matching strategy, or {@code null} if no registered
   * strategy supports the parameter
   */
  public @Nullable ParameterResolvingStrategy findStrategy(HandlerParameter parameter) {
    ParameterResolvingStrategy resolvingStrategy = lookupStrategy(parameter, customizedStrategies);
    if (resolvingStrategy == null) {
      resolvingStrategy = lookupStrategy(parameter, defaultStrategies);
    }
    return resolvingStrategy;
  }

  /**
   * Find a strategy for the given parameter, failing if none is available.
   *
   * @param parameter the parameter to resolve
   * @return a strategy that supports the parameter
   * @throws ParameterResolverNotFoundException if no registered strategy supports
   * the parameter
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
   * Determine whether either strategy collection contains a strategy whose
   * concrete class exactly matches the specified class.
   *
   * @param resolverClass the concrete strategy class to search for
   * @return {@code true} if a matching strategy is registered, {@code false}
   * otherwise
   */
  public boolean contains(Class<?> resolverClass) {
    return defaultStrategies.contains(resolverClass)
            || customizedStrategies.contains(resolverClass);
  }

  //

  /**
   * Add one or more customized parameter resolving strategies.
   *
   * <p>Customized strategies are searched before default strategies. A
   * {@code null} strategy is ignored.
   *
   * @param strategies the parameter resolving strategies to add
   * @since 4.0
   */
  public void addCustomizedStrategies(ParameterResolvingStrategy @Nullable ... strategies) {
    customizedStrategies.add(strategies);
  }

  /**
   * Add one or more default parameter resolving strategies.
   *
   * <p>Default strategies are searched after customized strategies. A
   * {@code null} strategy is ignored.
   *
   * @param strategies the parameter resolving strategies to add
   * @since 4.0
   */
  public void addDefaultStrategies(ParameterResolvingStrategy @Nullable ... strategies) {
    defaultStrategies.add(strategies);
  }

  /**
   * Trim the internal storage of both strategy collections to their current
   * sizes.
   *
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
