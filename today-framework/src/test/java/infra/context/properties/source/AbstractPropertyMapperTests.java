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

import java.util.List;

import infra.context.properties.source.ConfigurationPropertyName;
import infra.context.properties.source.PropertyMapper;

/**
 * Abstract base class for {@link PropertyMapper} tests.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Eddú Meléndez
 */
public abstract class AbstractPropertyMapperTests {

  protected abstract PropertyMapper getMapper();

  protected final List<String> mapConfigurationPropertyName(String configurationPropertyName) {
    return getMapper().map(ConfigurationPropertyName.of(configurationPropertyName));
  }

  protected final String mapPropertySourceName(String propertySourceName) {
    return getMapper().map(propertySourceName).toString();
  }

}