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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;

import infra.context.properties.source.ConfigurationPropertyName.ToStringFormat;

/**
 * {@link PropertyMapper} for system environment variables. Names are mapped by removing
 * invalid characters, converting to lower case and replacing "{@code _}" with
 * "{@code .}". For example, "{@code SERVER_PORT}" is mapped to "{@code server.port}". In
 * addition, numeric elements are mapped to indexes (e.g. "{@code HOST_0}" is mapped to
 * "{@code host[0]}").
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PropertyMapper
 * @see DefaultConfigurationPropertySource
 * @since 4.0
 */
final class SystemEnvironmentPropertyMapper implements PropertyMapper {

  public static final PropertyMapper INSTANCE = new SystemEnvironmentPropertyMapper();

  @Override
  public List<String> map(ConfigurationPropertyName configurationPropertyName) {
    String name = configurationPropertyName.toString(ToStringFormat.SYSTEM_ENVIRONMENT);
    String legacyName = configurationPropertyName.toString(ToStringFormat.LEGACY_SYSTEM_ENVIRONMENT);
    if (name.equals(legacyName)) {
      return Collections.singletonList(name);
    }
    return Arrays.asList(name, legacyName);
  }

  @Override
  public ConfigurationPropertyName map(String propertySourceName) {
    return convertName(propertySourceName);
  }

  private ConfigurationPropertyName convertName(String propertySourceName) {
    try {
      return ConfigurationPropertyName.adapt(propertySourceName, '_', this::processElementValue);
    }
    catch (Exception ex) {
      return ConfigurationPropertyName.EMPTY;
    }
  }

  private CharSequence processElementValue(CharSequence value) {
    String result = value.toString().toLowerCase(Locale.ENGLISH);
    return isNumber(result) ? "[" + result + "]" : result;
  }

  private static boolean isNumber(String string) {
    return string.chars().allMatch(Character::isDigit);
  }

  @Override
  public BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> getAncestorOfCheck() {
    return this::isAncestorOf;
  }

  private boolean isAncestorOf(ConfigurationPropertyName name, ConfigurationPropertyName candidate) {
    return name.isAncestorOf(candidate) || isLegacyAncestorOf(name, candidate);
  }

  private boolean isLegacyAncestorOf(ConfigurationPropertyName name, ConfigurationPropertyName candidate) {
    if (!name.hasDashedElement()) {
      return false;
    }
    ConfigurationPropertyName legacyCompatibleName = name.asSystemEnvironmentLegacyName();
    return legacyCompatibleName != null && legacyCompatibleName.isAncestorOf(candidate);
  }

}
