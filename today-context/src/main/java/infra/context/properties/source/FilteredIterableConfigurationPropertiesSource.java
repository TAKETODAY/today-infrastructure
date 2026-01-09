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
