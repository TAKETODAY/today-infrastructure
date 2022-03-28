/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.LogMessage;
import cn.taketoday.logging.Logger;
import cn.taketoday.util.Instantiator;

/**
 * A collection of {@link ConfigDataLoader} instances loaded via {@code spring.factories}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataLoaders {

  private final Logger logger;

  private final List<ConfigDataLoader<?>> loaders;

  private final List<Class<?>> resourceTypes;

  /**
   * Create a new {@link ConfigDataLoaders} instance.
   *
   * @param logFactory the deferred log factory
   * @param bootstrapContext the bootstrap context
   * @param classLoader the class loader used when loading
   */
  ConfigDataLoaders(DeferredLogFactory logFactory, ConfigurableBootstrapContext bootstrapContext,
          ClassLoader classLoader) {
    this(logFactory, bootstrapContext, classLoader,
            SpringFactoriesLoader.loadFactoryNames(ConfigDataLoader.class, classLoader));
  }

  /**
   * Create a new {@link ConfigDataLoaders} instance.
   *
   * @param logFactory the deferred log factory
   * @param bootstrapContext the bootstrap context
   * @param classLoader the class loader used when loading
   * @param names the {@link ConfigDataLoader} class names instantiate
   */
  ConfigDataLoaders(DeferredLogFactory logFactory, ConfigurableBootstrapContext bootstrapContext,
          ClassLoader classLoader, List<String> names) {
    this.logger = logFactory.getLog(getClass());
    Instantiator<ConfigDataLoader<?>> instantiator = new Instantiator<>(ConfigDataLoader.class,
            (availableParameters) -> {
              availableParameters.add(Log.class, logFactory::getLog);
              availableParameters.add(DeferredLogFactory.class, logFactory);
              availableParameters.add(ConfigurableBootstrapContext.class, bootstrapContext);
              availableParameters.add(BootstrapContext.class, bootstrapContext);
              availableParameters.add(BootstrapRegistry.class, bootstrapContext);
            });
    this.loaders = instantiator.instantiate(classLoader, names);
    this.resourceTypes = getResourceTypes(this.loaders);
  }

  private List<Class<?>> getResourceTypes(List<ConfigDataLoader<?>> loaders) {
    List<Class<?>> resourceTypes = new ArrayList<>(loaders.size());
    for (ConfigDataLoader<?> loader : loaders) {
      resourceTypes.add(getResourceType(loader));
    }
    return Collections.unmodifiableList(resourceTypes);
  }

  private Class<?> getResourceType(ConfigDataLoader<?> loader) {
    return ResolvableType.fromClass(loader.getClass()).as(ConfigDataLoader.class).resolveGeneric();
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
  <R extends ConfigDataResource> ConfigData load(ConfigDataLoaderContext context, R resource) throws IOException {
    ConfigDataLoader<R> loader = getLoader(context, resource);
    this.logger.trace(LogMessage.from(() -> "Loading " + resource + " using loader " + loader.getClass().getName()));
    return loader.load(context, resource);
  }

  @SuppressWarnings("unchecked")
  private <R extends ConfigDataResource> ConfigDataLoader<R> getLoader(ConfigDataLoaderContext context, R resource) {
    ConfigDataLoader<R> result = null;
    for (int i = 0; i < this.loaders.size(); i++) {
      ConfigDataLoader<?> candidate = this.loaders.get(i);
      if (this.resourceTypes.get(i).isInstance(resource)) {
        ConfigDataLoader<R> loader = (ConfigDataLoader<R>) candidate;
        if (loader.isLoadable(context, resource)) {
          if (result != null) {
            throw new IllegalStateException("Multiple loaders found for resource '" + resource + "' ["
                    + candidate.getClass().getName() + "," + result.getClass().getName() + "]");
          }
          result = loader;
        }
      }
    }
    Assert.state(result != null, () -> "No loader found for resource '" + resource + "'");
    return result;
  }

}
