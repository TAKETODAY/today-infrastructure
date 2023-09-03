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

import cn.taketoday.framework.BootstrapContext;
import cn.taketoday.framework.BootstrapRegistry;
import cn.taketoday.framework.ConfigurableBootstrapContext;
import cn.taketoday.lang.Nullable;

/**
 * Strategy class that can be used to load {@link ConfigData} for a given
 * {@link ConfigDataResource}. Implementations should be added as {@code today.strategies}
 * entries. The following constructor parameter types are supported:
 * <ul>
 * <li>{@link ConfigurableBootstrapContext} - A bootstrap context that can be used to
 * store objects that may be expensive to create, or need to be shared
 * ({@link BootstrapContext} or {@link BootstrapRegistry} may also be used).</li>
 * </ul>
 * <p>
 * Multiple loaders cannot claim the same resource.
 *
 * @param <R> the resource type
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
public interface ConfigDataLoader<R extends ConfigDataResource> {

  /**
   * Returns if the specified resource can be loaded by this instance.
   *
   * @param context the loader context
   * @param resource the resource to check.
   * @return if the resource is supported by this loader
   */
  default boolean isLoadable(ConfigDataLoaderContext context, R resource) {
    return true;
  }

  /**
   * Load {@link ConfigData} for the given resource.
   *
   * @param context the loader context
   * @param resource the resource to load
   * @return the loaded config data or {@code null} if the location should be skipped
   * @throws IOException on IO error
   * @throws ConfigDataResourceNotFoundException if the resource cannot be found
   */
  @Nullable
  ConfigData load(ConfigDataLoaderContext context, R resource)
          throws IOException, ConfigDataResourceNotFoundException;

}
