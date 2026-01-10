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

package infra.context.properties.source;

import org.jspecify.annotations.Nullable;

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
