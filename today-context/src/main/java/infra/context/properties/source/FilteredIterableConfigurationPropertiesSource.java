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

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A filtered {@link IterableConfigurationPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class FilteredIterableConfigurationPropertiesSource extends FilteredConfigurationPropertiesSource
        implements IterableConfigurationPropertySource {

  FilteredIterableConfigurationPropertiesSource(
          IterableConfigurationPropertySource source, Predicate<ConfigurationPropertyName> filter) {
    super(source, filter);
  }

  @Override
  public Stream<ConfigurationPropertyName> stream() {
    return getSource().stream().filter(getFilter());
  }

  @Override
  protected IterableConfigurationPropertySource getSource() {
    return (IterableConfigurationPropertySource) super.getSource();
  }

  @Override
  public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
    return ConfigurationPropertyState.search(this, name::isAncestorOf);
  }

}
