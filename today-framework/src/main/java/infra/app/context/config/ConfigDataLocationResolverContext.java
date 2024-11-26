/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.app.context.config;

import infra.app.ConfigurableBootstrapContext;
import infra.app.env.EnvironmentPostProcessor;
import infra.context.properties.bind.Binder;
import infra.lang.Nullable;

/**
 * Context provided to {@link ConfigDataLocationResolver} methods.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public interface ConfigDataLocationResolverContext {

  /**
   * Provides access to a binder that can be used to obtain previously contributed
   * values.
   *
   * @return a binder instance
   */
  Binder getBinder();

  /**
   * Provides access to the parent {@link ConfigDataResource} that triggered the resolve
   * or {@code null} if there is no available parent.
   *
   * @return the parent location
   */
  @Nullable
  ConfigDataResource getParent();

  /**
   * Provides access to the {@link ConfigurableBootstrapContext} shared across all
   * {@link EnvironmentPostProcessor EnvironmentPostProcessors}.
   *
   * @return the bootstrap context
   */
  ConfigurableBootstrapContext getBootstrapContext();

}
