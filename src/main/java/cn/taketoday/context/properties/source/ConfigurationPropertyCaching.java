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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties.source;

import java.time.Duration;

import cn.taketoday.core.env.Environment;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Interface that can be used to control configuration property source caches.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ConfigurationPropertyCaching {

  /**
   * Enable caching with an unlimited time-to-live.
   */
  void enable();

  /**
   * Disable caching.
   */
  void disable();

  /**
   * Set amount of time that an item can live in the cache. Calling this method will
   * also enable the cache.
   *
   * @param timeToLive the time to live value.
   */
  void setTimeToLive(Duration timeToLive);

  /**
   * Clear the cache and force it to be reloaded on next access.
   */
  void clear();

  /**
   * Get for all configuration property sources in the environment.
   *
   * @param environment the spring environment
   * @return a caching instance that controls all sources in the environment
   */
  static ConfigurationPropertyCaching get(Environment environment) {
    return get(environment, null);
  }

  /**
   * Get for a specific configuration property source in the environment.
   *
   * @param environment the spring environment
   * @param underlyingSource the
   * {@link ConfigurationPropertySource#getUnderlyingSource() underlying source} that
   * must match
   * @return a caching instance that controls the matching source
   */
  static ConfigurationPropertyCaching get(Environment environment, @Nullable Object underlyingSource) {
    Iterable<ConfigurationPropertySource> sources = ConfigurationPropertySources.get(environment);
    return get(sources, underlyingSource);
  }

  /**
   * Get for all specified configuration property sources.
   *
   * @param sources the configuration property sources
   * @return a caching instance that controls the sources
   */
  static ConfigurationPropertyCaching get(Iterable<ConfigurationPropertySource> sources) {
    return get(sources, null);
  }

  /**
   * Get for a specific configuration property source in the specified configuration
   * property sources.
   *
   * @param sources the configuration property sources
   * @param underlyingSource the
   * {@link ConfigurationPropertySource#getUnderlyingSource() underlying source} that
   * must match
   * @return a caching instance that controls the matching source
   */
  static ConfigurationPropertyCaching get(
          Iterable<ConfigurationPropertySource> sources, @Nullable Object underlyingSource) {
    Assert.notNull(sources, "Sources must not be null");
    if (underlyingSource == null) {
      return new ConfigurationPropertySourcesCaching(sources);
    }
    for (ConfigurationPropertySource source : sources) {
      if (source.getUnderlyingSource() == underlyingSource) {
        ConfigurationPropertyCaching caching = CachingConfigurationPropertySource.find(source);
        if (caching != null) {
          return caching;
        }
      }
    }
    throw new IllegalStateException("Unable to find cache from configuration property sources");
  }

}
