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

import infra.context.properties.source.ConfigurationProperty;
import infra.context.properties.source.ConfigurationPropertyName;
import infra.context.properties.source.ConfigurationPropertySource;
import infra.core.env.PropertySource;
import infra.lang.Assert;
import infra.origin.Origin;

/**
 * Exception thrown when an attempt is made to resolve a property against an inactive
 * {@link ConfigData} property source. Used to ensure that a user doesn't accidentally
 * attempt to specify a properties that can never be resolved.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
public class InactiveConfigDataAccessException extends ConfigDataException {

  private final PropertySource<?> propertySource;

  @Nullable
  private final ConfigDataResource location;

  private final String propertyName;

  @Nullable
  private final Origin origin;

  /**
   * Create a new {@link InactiveConfigDataAccessException} instance.
   *
   * @param propertySource the inactive property source
   * @param location the {@link ConfigDataResource} of the property source or
   * {@code null} if the source was not loaded from {@link ConfigData}.
   * @param propertyName the name of the property
   * @param origin the origin or the property or {@code null}
   */
  InactiveConfigDataAccessException(PropertySource<?> propertySource, @Nullable ConfigDataResource location,
          String propertyName, @Nullable Origin origin) {
    super(getMessage(propertySource, location, propertyName, origin), null);
    this.propertySource = propertySource;
    this.location = location;
    this.propertyName = propertyName;
    this.origin = origin;
  }

  private static String getMessage(
          PropertySource<?> propertySource, @Nullable ConfigDataResource location, String propertyName, @Nullable Origin origin) {
    StringBuilder message = new StringBuilder("Inactive property source '");
    message.append(propertySource.getName());
    if (location != null) {
      message.append("' imported from location '");
      message.append(location);
    }
    message.append("' cannot contain property '");
    message.append(propertyName);
    message.append("'");
    if (origin != null) {
      message.append(" [origin: ");
      message.append(origin);
      message.append("]");
    }
    return message.toString();
  }

  /**
   * Return the inactive property source that contained the property.
   *
   * @return the property source
   */
  public PropertySource<?> getPropertySource() {
    return this.propertySource;
  }

  /**
   * Return the {@link ConfigDataResource} of the property source or {@code null} if the
   * source was not loaded from {@link ConfigData}.
   *
   * @return the config data location or {@code null}
   */
  @Nullable
  public ConfigDataResource getLocation() {
    return this.location;
  }

  /**
   * Return the name of the property.
   *
   * @return the property name
   */
  public String getPropertyName() {
    return this.propertyName;
  }

  /**
   * Return the origin or the property or {@code null}.
   *
   * @return the property origin
   */
  @Nullable
  public Origin getOrigin() {
    return this.origin;
  }

  /**
   * Throw an {@link InactiveConfigDataAccessException} if the given
   * {@link ConfigDataEnvironmentContributor} contains the property.
   *
   * @param contributor the contributor to check
   * @param name the name to check
   */
  static void throwIfPropertyFound(ConfigDataEnvironmentContributor contributor, ConfigurationPropertyName name) {
    ConfigurationPropertySource source = contributor.configurationPropertySource;
    ConfigurationProperty property = (source != null) ? source.getConfigurationProperty(name) : null;
    if (property != null) {
      PropertySource<?> propertySource = contributor.propertySource;
      Assert.state(propertySource != null, "'propertySource' is required");
      throw new InactiveConfigDataAccessException(propertySource, contributor.resource, name.toString(),
              property.getOrigin());
    }
  }

}
