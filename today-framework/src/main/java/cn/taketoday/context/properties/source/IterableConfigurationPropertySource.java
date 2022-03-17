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

import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import cn.taketoday.lang.Nullable;
import cn.taketoday.origin.OriginTrackedValue;
import cn.taketoday.util.StringUtils;

/**
 * A {@link ConfigurationPropertySource} with a fully {@link Iterable} set of entries.
 * Implementations of this interface <strong>must</strong> be able to iterate over all
 * contained configuration properties. Any {@code non-null} result from
 * {@link #getConfigurationProperty(ConfigurationPropertyName)} must also have an
 * equivalent entry in the {@link #iterator() iterator}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConfigurationPropertyName
 * @see OriginTrackedValue
 * @see #getConfigurationProperty(ConfigurationPropertyName)
 * @see #iterator()
 * @see #stream()
 * @since 4.0
 */
public interface IterableConfigurationPropertySource
        extends ConfigurationPropertySource, Iterable<ConfigurationPropertyName> {

  /**
   * Return an iterator for the {@link ConfigurationPropertyName names} managed by this
   * source.
   *
   * @return an iterator (never {@code null})
   */
  @Override
  default Iterator<ConfigurationPropertyName> iterator() {
    return stream().iterator();
  }

  /**
   * Returns a sequential {@code Stream} for the {@link ConfigurationPropertyName names}
   * managed by this source.
   *
   * @return a stream of names (never {@code null})
   */
  Stream<ConfigurationPropertyName> stream();

  @Override
  default ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
    return ConfigurationPropertyState.search(this, name::isAncestorOf);
  }

  @Override
  default IterableConfigurationPropertySource filter(Predicate<ConfigurationPropertyName> filter) {
    return new FilteredIterableConfigurationPropertiesSource(this, filter);
  }

  @Override
  default IterableConfigurationPropertySource withAliases(ConfigurationPropertyNameAliases aliases) {
    return new AliasedIterableConfigurationPropertySource(this, aliases);
  }

  @Override
  default IterableConfigurationPropertySource withPrefix(@Nullable String prefix) {
    return StringUtils.hasText(prefix) ? new PrefixedIterableConfigurationPropertySource(this, prefix) : this;
  }

}
