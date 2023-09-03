/*
 * Copyright 2017 - 2023 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.context.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.framework.BootstrapContext;
import cn.taketoday.framework.BootstrapRegistry;
import cn.taketoday.framework.ConfigurableBootstrapContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.Instantiator;

/**
 * A collection of {@link ConfigDataLoader} instances loaded via {@code today.strategies}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
class ConfigDataLoaders {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final List<ConfigDataLoader<?>> loaders;

  private final List<Class<?>> resourceTypes;

  /**
   * Create a new {@link ConfigDataLoaders} instance.
   *
   * @param bootstrapContext the bootstrap context
   * @param classLoader the class loader used when loading
   */
  ConfigDataLoaders(ConfigurableBootstrapContext bootstrapContext, @Nullable ClassLoader classLoader) {
    this(bootstrapContext, classLoader,
            TodayStrategies.findNames(ConfigDataLoader.class, classLoader));
  }

  /**
   * Create a new {@link ConfigDataLoaders} instance.
   *
   * @param bootstrapContext the bootstrap context
   * @param classLoader the class loader used when loading
   * @param names the {@link ConfigDataLoader} class names instantiate
   */
  ConfigDataLoaders(ConfigurableBootstrapContext bootstrapContext,
          @Nullable ClassLoader classLoader, List<String> names) {
    var instantiator = new Instantiator<ConfigDataLoader<?>>(ConfigDataLoader.class,
            parameters -> {
              parameters.add(BootstrapContext.class, bootstrapContext);
              parameters.add(BootstrapRegistry.class, bootstrapContext);
              parameters.add(ConfigurableBootstrapContext.class, bootstrapContext);
            });
    this.loaders = instantiator.instantiate(classLoader, names);
    this.resourceTypes = getResourceTypes(this.loaders);
  }

  private List<Class<?>> getResourceTypes(List<ConfigDataLoader<?>> loaders) {
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
  @Nullable
  <R extends ConfigDataResource> ConfigData load(ConfigDataLoaderContext context, R resource) throws IOException {
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
      if (this.resourceTypes.get(i).isInstance(resource)) {
        ConfigDataLoader<?> candidate = this.loaders.get(i);
        ConfigDataLoader<R> loader = (ConfigDataLoader<R>) candidate;
        if (loader.isLoadable(context, resource)) {
          if (result != null) {
            throw new IllegalStateException(
                    "Multiple loaders found for resource '" + resource + "' ["
                            + candidate.getClass().getName() + "," + result.getClass().getName() + "]");
          }
          result = loader;
        }
      }
    }
    if (result == null) {
      throw new IllegalStateException("No loader found for resource '" + resource + "'");
    }
    return result;
  }

}
