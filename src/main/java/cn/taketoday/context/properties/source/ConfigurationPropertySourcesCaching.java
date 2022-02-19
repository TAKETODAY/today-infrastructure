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
import java.util.function.Consumer;

/**
 * {@link ConfigurationPropertyCaching} for an {@link Iterable iterable} set of
 * {@link ConfigurationPropertySource} instances.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertySourcesCaching implements ConfigurationPropertyCaching {

  private final Iterable<ConfigurationPropertySource> sources;

  ConfigurationPropertySourcesCaching(Iterable<ConfigurationPropertySource> sources) {
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

}
