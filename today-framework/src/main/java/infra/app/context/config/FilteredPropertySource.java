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

package infra.app.context.config;

import java.util.Set;
import java.util.function.Consumer;

import infra.core.env.ConfigurableEnvironment;
import infra.core.env.PropertySource;
import infra.core.env.PropertySources;

/**
 * Internal {@link PropertySource} implementation used by
 * {@link ConfigFileApplicationListener} to filter out properties for specific operations.
 *
 * @author Phillip Webb
 */
class FilteredPropertySource extends PropertySource<PropertySource<?>> {

  private final Set<String> filteredProperties;

  FilteredPropertySource(PropertySource<?> original, Set<String> filteredProperties) {
    super(original.getName(), original);
    this.filteredProperties = filteredProperties;
  }

  @Override
  public Object getProperty(String name) {
    if (this.filteredProperties.contains(name)) {
      return null;
    }
    return getSource().getProperty(name);
  }

  static void apply(ConfigurableEnvironment environment, String propertySourceName, Set<String> filteredProperties,
          Consumer<PropertySource<?>> operation) {
    PropertySources propertySources = environment.getPropertySources();
    PropertySource<?> original = propertySources.get(propertySourceName);
    if (original == null) {
      operation.accept(null);
      return;
    }
    propertySources.replace(propertySourceName, new FilteredPropertySource(original, filteredProperties));
    try {
      operation.accept(original);
    }
    finally {
      propertySources.replace(propertySourceName, original);
    }
  }

}
