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

package infra.app.context.config;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import infra.context.properties.source.ConfigurationProperty;
import infra.context.properties.source.ConfigurationPropertyName;
import infra.context.properties.source.ConfigurationPropertySource;
import infra.core.env.AbstractEnvironment;

/**
 * Exception thrown if an invalid property is found when processing config data.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
public class InvalidConfigDataPropertyException extends ConfigDataException {

  private static final Set<ConfigurationPropertyName> PROFILE_SPECIFIC_ERRORS;
  private static final Map<ConfigurationPropertyName, ConfigurationPropertyName> ERRORS;

  static {
    var errors = new LinkedHashMap<ConfigurationPropertyName, ConfigurationPropertyName>();
    errors.put(ConfigurationPropertyName.of("infra.profiles"), ConfigurationPropertyName.of("app.config.activate.on-profile"));
    errors.put(ConfigurationPropertyName.of("infra.profiles[0]"), ConfigurationPropertyName.of("app.config.activate.on-profile"));
    ERRORS = Collections.unmodifiableMap(errors);
  }

  static {
    var errors = new LinkedHashSet<ConfigurationPropertyName>();
    errors.add(Profiles.INCLUDE_PROFILES);
    errors.add(Profiles.INCLUDE_PROFILES.append("[0]"));
    errors.add(ConfigurationPropertyName.of(AbstractEnvironment.KEY_ACTIVE_PROFILES));
    errors.add(ConfigurationPropertyName.of(AbstractEnvironment.KEY_ACTIVE_PROFILES + "[0]"));
    errors.add(ConfigurationPropertyName.of(AbstractEnvironment.KEY_DEFAULT_PROFILES));
    errors.add(ConfigurationPropertyName.of(AbstractEnvironment.KEY_DEFAULT_PROFILES + "[0]"));
    PROFILE_SPECIFIC_ERRORS = Collections.unmodifiableSet(errors);
  }

  private final ConfigurationProperty property;

  @Nullable
  private final ConfigurationPropertyName replacement;

  @Nullable
  private final ConfigDataResource location;

  InvalidConfigDataPropertyException(ConfigurationProperty property, boolean profileSpecific,
          @Nullable ConfigurationPropertyName replacement, @Nullable ConfigDataResource location) {
    super(getMessage(property, profileSpecific, replacement, location), null);
    this.property = property;
    this.replacement = replacement;
    this.location = location;
  }

  /**
   * Return source property that caused the exception.
   *
   * @return the invalid property
   */
  public ConfigurationProperty getProperty() {
    return this.property;
  }

  /**
   * Return the {@link ConfigDataResource} of the invalid property or {@code null} if
   * the source was not loaded from {@link ConfigData}.
   *
   * @return the config data location or {@code null}
   */
  @Nullable
  public ConfigDataResource getLocation() {
    return this.location;
  }

  /**
   * Return the replacement property that should be used instead or {@code null} if not
   * replacement is available.
   *
   * @return the replacement property name
   */
  @Nullable
  public ConfigurationPropertyName getReplacement() {
    return this.replacement;
  }

  /**
   * Throw an {@link InvalidConfigDataPropertyException} if the given
   * {@link ConfigDataEnvironmentContributor} contains any invalid property.
   *
   * @param contributor the contributor to check
   */
  static void throwIfPropertyFound(ConfigDataEnvironmentContributor contributor) {
    ConfigurationPropertySource propertySource = contributor.configurationPropertySource;
    if (propertySource != null) {
      ERRORS.forEach((name, replacement) -> {
        ConfigurationProperty property = propertySource.getConfigurationProperty(name);
        if (property != null) {
          throw new InvalidConfigDataPropertyException(property, false, replacement, contributor.resource);
        }
      });
      if (contributor.fromProfileSpecificImport
              && !contributor.hasConfigDataOption(ConfigData.Option.IGNORE_PROFILES)) {
        PROFILE_SPECIFIC_ERRORS.forEach(name -> {
          ConfigurationProperty property = propertySource.getConfigurationProperty(name);
          if (property != null) {
            throw new InvalidConfigDataPropertyException(property, true, null, contributor.resource);
          }
        });
      }
    }
  }

  private static String getMessage(ConfigurationProperty property, boolean profileSpecific,
          @Nullable ConfigurationPropertyName replacement, @Nullable ConfigDataResource location) {
    StringBuilder message = new StringBuilder("Property '");
    message.append(property.getName());
    if (location != null) {
      message.append("' imported from location '");
      message.append(location);
    }
    message.append("' is invalid");
    if (profileSpecific) {
      message.append(" in a profile specific resource");
    }
    if (replacement != null) {
      message.append(" and should be replaced with '");
      message.append(replacement);
      message.append("'");
    }
    if (property.getOrigin() != null) {
      message.append(" [origin: ");
      message.append(property.getOrigin());
      message.append("]");
    }
    return message.toString();
  }

}
