/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.properties.bind;

import org.jspecify.annotations.Nullable;

import infra.context.properties.source.ConfigurationProperty;
import infra.context.properties.source.ConfigurationPropertySource;

/**
 * Context information for use by {@link BindHandler BindHandlers}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface BindContext {

  /**
   * Return the source binder that is performing the bind operation.
   *
   * @return the source binder
   */
  Binder getBinder();

  /**
   * Return the current depth of the binding. Root binding starts with a depth of
   * {@code 0}. Each subsequent property binding increases the depth by {@code 1}.
   *
   * @return the depth of the current binding
   */
  int getDepth();

  /**
   * Return an {@link Iterable} of the {@link ConfigurationPropertySource sources} being
   * used by the {@link Binder}.
   *
   * @return the sources
   */
  Iterable<ConfigurationPropertySource> getSources();

  /**
   * Return the {@link ConfigurationProperty} actually being bound or {@code null} if
   * the property has not yet been determined.
   *
   * @return the configuration property (may be {@code null}).
   */
  @Nullable
  ConfigurationProperty getConfigurationProperty();

}
