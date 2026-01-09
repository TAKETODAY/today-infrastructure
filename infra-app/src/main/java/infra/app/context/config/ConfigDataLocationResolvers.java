/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.context.config;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import infra.app.BootstrapContext;
import infra.app.BootstrapRegistry;
import infra.app.ConfigurableBootstrapContext;
import infra.context.properties.bind.Binder;
import infra.core.env.Environment;
import infra.core.io.ResourceLoader;
import infra.lang.TodayStrategies;
import infra.util.Instantiator;

/**
 * A collection of {@link ConfigDataLocationResolver} instances loaded via
 * {@code today.strategies}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ConfigDataLocationResolvers {

  private final List<ConfigDataLocationResolver<?>> resolvers;

  /**
   * Create a new {@link ConfigDataLocationResolvers} instance.
   *
   * @param bootstrapContext the bootstrap context
   * @param binder a binder providing values from the initial {@link Environment}
   * @param resourceLoader {@link ResourceLoader} to load resource locations
   */
  ConfigDataLocationResolvers(ConfigurableBootstrapContext bootstrapContext, Binder binder, ResourceLoader resourceLoader) {
    this(bootstrapContext, binder, resourceLoader, TodayStrategies.findNames(
            ConfigDataLocationResolver.class, resourceLoader.getClassLoader()));
  }

  /**
   * Create a new {@link ConfigDataLocationResolvers} instance.
   *
   * @param bootstrapContext the bootstrap context
   * @param binder {@link Binder} providing values from the initial {@link Environment}
   * @param resourceLoader {@link ResourceLoader} to load resource locations
   * @param names the {@link ConfigDataLocationResolver} class names
   */
  ConfigDataLocationResolvers(ConfigurableBootstrapContext bootstrapContext,
          Binder binder, ResourceLoader resourceLoader, List<String> names) {
    var instantiator = new Instantiator<ConfigDataLocationResolver<?>>(ConfigDataLocationResolver.class,
            parameters -> {
              parameters.add(Binder.class, binder);
              parameters.add(ResourceLoader.class, resourceLoader);
              parameters.add(ConfigurableBootstrapContext.class, bootstrapContext);
              parameters.add(BootstrapContext.class, bootstrapContext);
              parameters.add(BootstrapRegistry.class, bootstrapContext);
            });
    this.resolvers = reorder(instantiator.instantiate(resourceLoader.getClassLoader(), names));
  }

  private List<ConfigDataLocationResolver<?>> reorder(List<ConfigDataLocationResolver<?>> resolvers) {
    var reordered = new ArrayList<ConfigDataLocationResolver<?>>(resolvers.size());
    ConfigDataLocationResolver<?> resourceResolver = null;
    for (ConfigDataLocationResolver<?> resolver : resolvers) {
      if (resolver instanceof StandardConfigDataLocationResolver) {
        resourceResolver = resolver;
      }
      else {
        reordered.add(resolver);
      }
    }
    if (resourceResolver != null) {
      reordered.add(resourceResolver);
    }
    return Collections.unmodifiableList(reordered);
  }

  List<ConfigDataResolutionResult> resolve(ConfigDataLocationResolverContext context, @Nullable ConfigDataLocation location, @Nullable Profiles profiles) {
    if (location == null) {
      return Collections.emptyList();
    }
    for (ConfigDataLocationResolver<?> resolver : getResolvers()) {
      if (resolver.isResolvable(context, location)) {
        return resolve(resolver, context, location, profiles);
      }
    }
    throw new UnsupportedConfigDataLocationException(location);
  }

  private List<ConfigDataResolutionResult> resolve(ConfigDataLocationResolver<?> resolver,
          ConfigDataLocationResolverContext context, ConfigDataLocation location, @Nullable Profiles profiles) {
    List<ConfigDataResolutionResult> resolved = resolve(location, false, () -> resolver.resolve(context, location));
    if (profiles == null) {
      return resolved;
    }
    List<ConfigDataResolutionResult> profileSpecific = resolve(
            location, true, () -> resolver.resolveProfileSpecific(context, location, profiles));
    return merge(resolved, profileSpecific);
  }

  // TODO: replace resolveAction to list
  private List<ConfigDataResolutionResult> resolve(ConfigDataLocation location, boolean profileSpecific,
          Supplier<List<? extends ConfigDataResource>> resolveAction) {
    List<ConfigDataResource> resources = nonNullList(resolveAction.get());
    var resolved = new ArrayList<ConfigDataResolutionResult>(resources.size());
    for (ConfigDataResource resource : resources) {
      resolved.add(new ConfigDataResolutionResult(location, resource, profileSpecific));
    }
    return resolved;
  }

  @SuppressWarnings("unchecked")
  private <T> List<T> nonNullList(@Nullable List<? extends T> list) {
    return (list != null) ? (List<T>) list : Collections.emptyList();
  }

  private <T> List<T> merge(List<T> list1, List<T> list2) {
    var merged = new ArrayList<T>(list1.size() + list2.size());
    merged.addAll(list1);
    merged.addAll(list2);
    return merged;
  }

  /**
   * Return the resolvers managed by this object.
   *
   * @return the resolvers
   */
  List<ConfigDataLocationResolver<?>> getResolvers() {
    return this.resolvers;
  }

}
