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

import java.util.List;
import java.util.function.BiPredicate;

import cn.taketoday.core.env.EnumerablePropertySource;
import cn.taketoday.core.env.PropertySource;

/**
 * Strategy used to provide a mapping between a {@link PropertySource} and a
 * {@link ConfigurationPropertySource}.
 * <p>
 * Mappings should be provided for both {@link ConfigurationPropertyName
 * ConfigurationPropertyName} types and {@code String} based names. This allows the
 * {@link DefaultConfigurationPropertySource} to first attempt any direct mappings (i.e.
 * map the {@link ConfigurationPropertyName} directly to the {@link PropertySource} name)
 * before falling back to {@link EnumerablePropertySource enumerating} property names,
 * mapping them to a {@link ConfigurationPropertyName} and checking for applicability. See
 * {@link DefaultConfigurationPropertySource} for more details.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultConfigurationPropertySource
 * @since 4.0
 */
interface PropertyMapper {

  /**
   * The default ancestor of check.
   */
  BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> DEFAULT_ANCESTOR_OF_CHECK = ConfigurationPropertyName::isAncestorOf;

  /**
   * Provide mappings from a {@link ConfigurationPropertySource}
   * {@link ConfigurationPropertyName}.
   *
   * @param configurationPropertyName the name to map
   * @return the mapped names or an empty list
   */
  List<String> map(ConfigurationPropertyName configurationPropertyName);

  /**
   * Provide mappings from a {@link PropertySource} property name.
   *
   * @param propertySourceName the name to map
   * @return the mapped configuration property name or
   * {@link ConfigurationPropertyName#EMPTY}
   */
  ConfigurationPropertyName map(String propertySourceName);

  /**
   * Returns a {@link BiPredicate} that can be used to check if one name is an ancestor
   * of another when considering the mapping rules.
   *
   * @return a predicate that can be used to check if one name is an ancestor of another
   */
  default BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> getAncestorOfCheck() {
    return DEFAULT_ANCESTOR_OF_CHECK;
  }

}
