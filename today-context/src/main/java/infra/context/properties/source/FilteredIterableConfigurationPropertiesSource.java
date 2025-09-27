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

package infra.context.properties.source;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
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

  @Nullable
  private ConfigurationPropertyName @Nullable [] filteredNames;

  private int numerOfFilteredNames;

  @SuppressWarnings("NullAway")
  FilteredIterableConfigurationPropertiesSource(IterableConfigurationPropertySource source, Predicate<ConfigurationPropertyName> filter) {
    super(source, filter);
    @Nullable ConfigurationPropertyName[] filterableNames = getFilterableNames(source);
    if (filterableNames != null) {
      this.filteredNames = new ConfigurationPropertyName[filterableNames.length];
      this.numerOfFilteredNames = 0;
      for (ConfigurationPropertyName name : filterableNames) {
        if (name == null) {
          break;
        }
        if (filter.test(name)) {
          this.filteredNames[this.numerOfFilteredNames++] = name;
        }
      }
    }
  }

  @Nullable
  private ConfigurationPropertyName @Nullable [] getFilterableNames(IterableConfigurationPropertySource source) {
    if (source instanceof DefaultIterableConfigurationPropertySource dicps && dicps.isImmutablePropertySource()) {
      return dicps.getConfigurationPropertyNames();
    }
    if (source instanceof FilteredIterableConfigurationPropertiesSource filteredSource) {
      return filteredSource.filteredNames;
    }
    return null;
  }

  @Override
  @SuppressWarnings("NullAway")
  public Stream<ConfigurationPropertyName> stream() {
    if (this.filteredNames != null) {
      return Arrays.stream(this.filteredNames, 0, this.numerOfFilteredNames);
    }
    return getSource().stream().filter(getFilter());
  }

  @Override
  protected IterableConfigurationPropertySource getSource() {
    return (IterableConfigurationPropertySource) super.getSource();
  }

  @Override
  @SuppressWarnings("NullAway")
  public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
    if (this.filteredNames != null) {
      return ConfigurationPropertyState.search(this.filteredNames,
              0, this.numerOfFilteredNames, name::isAncestorOf);
    }
    return ConfigurationPropertyState.search(this, name::isAncestorOf);
  }

}
