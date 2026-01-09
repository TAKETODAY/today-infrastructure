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

import java.time.Duration;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * {@link ConfigurationPropertyCaching} for an {@link Iterable iterable} set of
 * {@link ConfigurationPropertySource} instances.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ConfigurationPropertySourcesCaching implements ConfigurationPropertyCaching {

  @Nullable
  private final Iterable<ConfigurationPropertySource> sources;

  ConfigurationPropertySourcesCaching(@Nullable Iterable<ConfigurationPropertySource> sources) {
    this.sources = sources;
  }

  @Override
  public void enable() {
    forEach(ConfigurationPropertyCaching::enable);
  }

  @Override
  public void disable() {
    forEach(ConfigurationPropertyCaching::disable);
  }

  @Override
  public void setTimeToLive(Duration timeToLive) {
    forEach((caching) -> caching.setTimeToLive(timeToLive));
  }

  @Override
  public void clear() {
    forEach(ConfigurationPropertyCaching::clear);
  }

  @Override
  public CacheOverride override() {
    CacheOverrides override = new CacheOverrides();
    forEach(override::add);
    return override;
  }

  private void forEach(Consumer<ConfigurationPropertyCaching> action) {
    if (this.sources != null) {
      for (ConfigurationPropertySource source : this.sources) {
        ConfigurationPropertyCaching caching = CachingConfigurationPropertySource.find(source);
        if (caching != null) {
          action.accept(caching);
        }
      }
    }
  }

  /**
   * Composite {@link CacheOverride}.
   */
  private static final class CacheOverrides implements CacheOverride {

    private final ArrayList<CacheOverride> overrides = new ArrayList<>();

    void add(ConfigurationPropertyCaching caching) {
      this.overrides.add(caching.override());
    }

    @Override
    public void close() {
      this.overrides.forEach(CacheOverride::close);
    }

  }

}
