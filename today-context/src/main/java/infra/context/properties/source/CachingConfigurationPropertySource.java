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

package infra.context.properties.source;

import infra.lang.Nullable;

/**
 * Interface used to indicate that a {@link ConfigurationPropertySource} supports
 * {@link ConfigurationPropertyCaching}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
interface CachingConfigurationPropertySource {

  /**
   * Return {@link ConfigurationPropertyCaching} for this source.
   *
   * @return source caching
   */
  ConfigurationPropertyCaching getCaching();

  /**
   * Find {@link ConfigurationPropertyCaching} for the given source.
   *
   * @param source the configuration property source
   * @return a {@link ConfigurationPropertyCaching} instance or {@code null} if the
   * source does not support caching.
   */
  @Nullable
  static ConfigurationPropertyCaching find(ConfigurationPropertySource source) {
    if (source instanceof CachingConfigurationPropertySource) {
      return ((CachingConfigurationPropertySource) source).getCaching();
    }
    return null;
  }

}
