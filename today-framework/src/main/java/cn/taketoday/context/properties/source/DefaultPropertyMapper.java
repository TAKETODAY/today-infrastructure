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

import java.util.Collections;
import java.util.List;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Default {@link PropertyMapper} implementation. Names are mapped by removing invalid
 * characters and converting to lower case. For example "{@code my.server_name.PORT}" is
 * mapped to "{@code my.servername.port}".
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PropertyMapper
 * @see FrameworkConfigurationPropertySource
 * @since 4.0
 */
final class DefaultPropertyMapper implements PropertyMapper {

  public static final PropertyMapper INSTANCE = new DefaultPropertyMapper();

  @Nullable
  private LastMapping<ConfigurationPropertyName, List<String>> lastMappedConfigurationPropertyName;

  @Nullable
  private LastMapping<String, ConfigurationPropertyName> lastMappedPropertyName;

  private DefaultPropertyMapper() { }

  @Override
  public List<String> map(ConfigurationPropertyName configurationPropertyName) {
    // Use a local copy in case another thread changes things
    LastMapping<ConfigurationPropertyName, List<String>> last = this.lastMappedConfigurationPropertyName;
    if (last != null && last.isFrom(configurationPropertyName)) {
      return last.getMapping();
    }
    String convertedName = configurationPropertyName.toString();
    List<String> mapping = Collections.singletonList(convertedName);
    this.lastMappedConfigurationPropertyName = new LastMapping<>(configurationPropertyName, mapping);
    return mapping;
  }

  @Override
  public ConfigurationPropertyName map(String propertySourceName) {
    // Use a local copy in case another thread changes things
    LastMapping<String, ConfigurationPropertyName> last = this.lastMappedPropertyName;
    if (last != null && last.isFrom(propertySourceName)) {
      return last.getMapping();
    }
    ConfigurationPropertyName mapping = tryMap(propertySourceName);
    this.lastMappedPropertyName = new LastMapping<>(propertySourceName, mapping);
    return mapping;
  }

  private ConfigurationPropertyName tryMap(String propertySourceName) {
    try {
      ConfigurationPropertyName convertedName = ConfigurationPropertyName.adapt(propertySourceName, '.');
      if (!convertedName.isEmpty()) {
        return convertedName;
      }
    }
    catch (Exception ignored) { }
    return ConfigurationPropertyName.EMPTY;
  }

  private record LastMapping<T, M>(T from, M mapping) {

    boolean isFrom(T from) {
      return ObjectUtils.nullSafeEquals(from, this.from);
    }

    M getMapping() {
      return this.mapping;
    }

  }

}
