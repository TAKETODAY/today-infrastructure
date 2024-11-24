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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import infra.context.properties.source.ConfigurationPropertyName;
import infra.context.properties.source.PropertyMapper;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;

/**
 * Test {@link PropertyMapper} implementation.
 */
class TestPropertyMapper implements PropertyMapper {

  private MultiValueMap<ConfigurationPropertyName, String> fromConfig = new LinkedMultiValueMap<>();

  private Map<String, ConfigurationPropertyName> fromSource = new LinkedHashMap<>();

  void addFromPropertySource(String from, String to) {
    this.fromSource.put(from, ConfigurationPropertyName.of(to));
  }

  void addFromConfigurationProperty(ConfigurationPropertyName from, String... to) {
    for (String propertySourceName : to) {
      this.fromConfig.add(from, propertySourceName);
    }
  }

  @Override
  public List<String> map(ConfigurationPropertyName configurationPropertyName) {
    return this.fromConfig.getOrDefault(configurationPropertyName, Collections.emptyList());
  }

  @Override
  public ConfigurationPropertyName map(String propertySourceName) {
    return this.fromSource.getOrDefault(propertySourceName, ConfigurationPropertyName.EMPTY);
  }

}
