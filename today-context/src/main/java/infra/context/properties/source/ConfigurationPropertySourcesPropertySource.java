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

import infra.core.env.Environment;
import infra.core.env.PropertyResolver;
import infra.core.env.PropertySource;
import infra.origin.Origin;
import infra.origin.OriginLookup;

/**
 * {@link PropertySource} that exposes {@link ConfigurationPropertySource} instances so
 * that they can be used with a {@link PropertyResolver} or added to the
 * {@link Environment}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ConfigurationPropertySourcesPropertySource extends PropertySource<Iterable<ConfigurationPropertySource>>
        implements OriginLookup<String> {

  ConfigurationPropertySourcesPropertySource(String name, Iterable<ConfigurationPropertySource> source) {
    super(name, source);
  }

  @Override
  public boolean containsProperty(String name) {
    return findConfigurationProperty(name) != null;
  }

  @Override
  @Nullable
  public Object getProperty(String name) {
    ConfigurationProperty configurationProperty = findConfigurationProperty(name);
    return configurationProperty != null ? configurationProperty.getValue() : null;
  }

  @Nullable
  @Override
  public Origin getOrigin(String name) {
    return Origin.from(findConfigurationProperty(name));
  }

  @Nullable
  private ConfigurationProperty findConfigurationProperty(String name) {
    try {
      return findConfigurationProperty(ConfigurationPropertyName.of(name, true));
    }
    catch (Exception ex) {
      return null;
    }
  }

  @Nullable
  ConfigurationProperty findConfigurationProperty(@Nullable ConfigurationPropertyName name) {
    if (name == null) {
      return null;
    }
    for (ConfigurationPropertySource configurationPropertySource : getSource()) {
      ConfigurationProperty configurationProperty = configurationPropertySource.getConfigurationProperty(name);
      if (configurationProperty != null) {
        return configurationProperty;
      }
    }
    return null;
  }

}
