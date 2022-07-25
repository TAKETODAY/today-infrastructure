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

import java.util.function.Predicate;

import cn.taketoday.core.env.PropertySource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.origin.OriginTrackedValue;
import cn.taketoday.util.StringUtils;

/**
 * A source of {@link ConfigurationProperty ConfigurationProperties}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConfigurationPropertyName
 * @see OriginTrackedValue
 * @see #getConfigurationProperty(ConfigurationPropertyName)
 * @since 4.0
 */
@FunctionalInterface
public interface ConfigurationPropertySource {

  /**
   * Return a single {@link ConfigurationProperty} from the source or {@code null} if no
   * property can be found.
   *
   * @param name the name of the property (must not be {@code null})
   * @return the associated object or {@code null}.
   */
  @Nullable
  ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name);

  /**
   * Returns if the source contains any descendants of the specified name. May return
   * {@link ConfigurationPropertyState#PRESENT} or
   * {@link ConfigurationPropertyState#ABSENT} if an answer can be determined or
   * {@link ConfigurationPropertyState#UNKNOWN} if it's not possible to determine a
   * definitive answer.
   *
   * @param name the name to check
   * @return if the source contains any descendants
   */
  default ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
    return ConfigurationPropertyState.UNKNOWN;
  }

  /**
   * Return a filtered variant of this source, containing only names that match the
   * given {@link Predicate}.
   *
   * @param filter the filter to match
   * @return a filtered {@link ConfigurationPropertySource} instance
   */
  default ConfigurationPropertySource filter(Predicate<ConfigurationPropertyName> filter) {
    return new FilteredConfigurationPropertiesSource(this, filter);
  }

  /**
   * Return a variant of this source that supports name aliases.
   *
   * @param aliases a function that returns a stream of aliases for any given name
   * @return a {@link ConfigurationPropertySource} instance supporting name aliases
   */
  default ConfigurationPropertySource withAliases(ConfigurationPropertyNameAliases aliases) {
    return new AliasedConfigurationPropertySource(this, aliases);
  }

  /**
   * Return a variant of this source that supports a prefix.
   *
   * @param prefix the prefix for properties in the source
   * @return a {@link ConfigurationPropertySource} instance supporting a prefix
   */
  default ConfigurationPropertySource withPrefix(@Nullable String prefix) {
    return StringUtils.hasText(prefix) ? new PrefixedConfigurationPropertySource(this, prefix) : this;
  }

  /**
   * Return the underlying source that is actually providing the properties.
   *
   * @return the underlying property source or {@code null}.
   */
  @Nullable
  default Object getUnderlyingSource() {
    return null;
  }

  /**
   * Return a single new {@link ConfigurationPropertySource} adapted from the given
   * Framework {@link PropertySource} or {@code null} if the source cannot be adapted.
   *
   * @param source the Framework property source to adapt
   * @return an adapted source or {@code null} {@link DefaultConfigurationPropertySource}
   */
  @Nullable
  static ConfigurationPropertySource from(PropertySource<?> source) {
    if (source instanceof ConfigurationPropertySourcesPropertySource) {
      return null;
    }
    return DefaultConfigurationPropertySource.from(source);
  }

}
