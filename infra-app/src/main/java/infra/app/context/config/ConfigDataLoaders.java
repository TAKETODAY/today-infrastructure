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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import infra.app.BootstrapContext;
import infra.app.BootstrapRegistry;
import infra.app.ConfigurableBootstrapContext;
import infra.core.ResolvableType;
import infra.lang.TodayStrategies;
import infra.lang.TodayStrategies.ArgumentResolver;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * A collection of {@link ConfigDataLoader} instances loaded via {@code today.strategies}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
class ConfigDataLoaders {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @SuppressWarnings("rawtypes")
  private final List<ConfigDataLoader> loaders;

  private final List<Class<?>> resourceTypes;

  /**
   * Create a new {@link ConfigDataLoaders} instance.
   *
   * @param bootstrapContext the bootstrap context
   * @param strategies to load {@link ConfigDataLoader} instances
   */
  ConfigDataLoaders(ConfigurableBootstrapContext bootstrapContext, TodayStrategies strategies) {
    this.loaders = strategies.load(ConfigDataLoader.class, ArgumentResolver.of(BootstrapContext.class, bootstrapContext)
            .and(BootstrapRegistry.class, bootstrapContext)
            .and(ConfigurableBootstrapContext.class, bootstrapContext));
    this.resourceTypes = getResourceTypes(this.loaders);
  }

  @SuppressWarnings("rawtypes")
  private List<Class<?>> getResourceTypes(List<ConfigDataLoader> loaders) {
    var resourceTypes = new ArrayList<Class<?>>(loaders.size());
    for (ConfigDataLoader<?> loader : loaders) {
      resourceTypes.add(getResourceType(loader));
    }
    return Collections.unmodifiableList(resourceTypes);
  }

  private Class<?> getResourceType(ConfigDataLoader<?> loader) {
    ResolvableType resolvableType = ResolvableType.forClass(loader.getClass());
    Class<?> resourceType = resolvableType.as(ConfigDataLoader.class).resolveGeneric();
    if (resourceType == null) {
      throw new IllegalStateException("Cannot determine resource type for " + loader.getClass());
    }
    return resourceType;
  }

  /**
   * Load {@link ConfigData} using the first appropriate {@link ConfigDataLoader}.
   *
   * @param <R> the resource type
   * @param context the loader context
   * @param resource the resource to load
   * @return the loaded {@link ConfigData}
   * @throws IOException on IO error
   */
  <R extends ConfigDataResource> @Nullable ConfigData load(ConfigDataLoaderContext context, R resource) throws IOException {
    ConfigDataLoader<R> loader = getLoader(context, resource);
    if (logger.isTraceEnabled()) {
      logger.trace("Loading {} using loader {}", resource, loader.getClass().getName());
    }
    return loader.load(context, resource);
  }

  @SuppressWarnings("unchecked")
  private <R extends ConfigDataResource> ConfigDataLoader<R> getLoader(ConfigDataLoaderContext context, R resource) {
    ConfigDataLoader<R> result = null;
    for (int i = 0; i < this.loaders.size(); i++) {
      ConfigDataLoader<R> candidate = this.loaders.get(i);
      if (this.resourceTypes.get(i).isInstance(resource)) {
        if (candidate.isLoadable(context, resource)) {
          if (result != null) {
            throw new IllegalStateException("Multiple loaders found for resource '%s' [%s,%s]"
                    .formatted(resource, candidate.getClass().getName(), result.getClass().getName()));
          }
          result = candidate;
        }
      }
    }
    if (result == null) {
      throw new IllegalStateException("No loader found for resource '" + resource + "'");
    }
    return result;
  }

}
