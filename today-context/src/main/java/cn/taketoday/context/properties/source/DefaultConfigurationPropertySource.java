/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.properties.source;

import java.util.Map;
import java.util.Random;

import cn.taketoday.context.properties.source.ConfigurationPropertyName.Form;
import cn.taketoday.core.env.EnumerablePropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.env.SystemEnvironmentPropertySource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.origin.Origin;
import cn.taketoday.origin.PropertySourceOrigin;

/**
 * {@link ConfigurationPropertySource} backed by a non-enumerable Framework
 * {@link PropertySource} or a restricted {@link EnumerablePropertySource} implementation
 * (such as a security restricted {@code systemEnvironment} source). A
 * {@link PropertySource} is adapted with the help of a {@link PropertyMapper} which
 * provides the mapping rules for individual properties.
 * <p>
 * Each {@link ConfigurationPropertySource#getConfigurationProperty
 * getConfigurationProperty} call attempts to
 * {@link PropertyMapper#map(ConfigurationPropertyName) map} the
 * {@link ConfigurationPropertyName} to one or more {@code String} based names. This
 * allows fast property resolution for well formed property sources.
 * <p>
 * When possible the {@link DefaultIterableConfigurationPropertySource} will be used in
 * preference to this implementation since it supports full "relaxed" style resolution.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #from(PropertySource)
 * @see PropertyMapper
 * @see DefaultIterableConfigurationPropertySource
 * @since 4.0
 */
class DefaultConfigurationPropertySource implements ConfigurationPropertySource {

  private static final PropertyMapper[] DEFAULT_MAPPERS = {
          DefaultPropertyMapper.INSTANCE
  };

  private static final PropertyMapper[] SYSTEM_ENVIRONMENT_MAPPERS = {
          SystemEnvironmentPropertyMapper.INSTANCE,
          DefaultPropertyMapper.INSTANCE
  };

  private final PropertySource<?> propertySource;

  private final PropertyMapper[] mappers;

  /**
   * Create a new {@link DefaultConfigurationPropertySource} implementation.
   *
   * @param propertySource the source property source
   * @param mappers the property mappers
   */
  DefaultConfigurationPropertySource(PropertySource<?> propertySource, PropertyMapper... mappers) {
    Assert.notNull(propertySource, "PropertySource is required");
    Assert.isTrue(mappers.length > 0, "Mappers must contain at least one item");
    this.propertySource = propertySource;
    this.mappers = mappers;
  }

  @Nullable
  @Override
  public ConfigurationProperty getConfigurationProperty(@Nullable ConfigurationPropertyName name) {
    if (name == null) {
      return null;
    }
    for (PropertyMapper mapper : this.mappers) {
      try {
        for (String candidate : mapper.map(name)) {
          Object value = getPropertySource().getProperty(candidate);
          if (value != null) {
            Origin origin = PropertySourceOrigin.get(this.propertySource, candidate);
            return ConfigurationProperty.of(this, name, value, origin);
          }
        }
      }
      catch (Exception ignored) { }
    }
    return null;
  }

  @Override
  public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
    PropertySource<?> source = getPropertySource();
    if (source.getSource() instanceof Random) {
      return containsDescendantOfForRandom("random", name);
    }
    if (source.getSource() instanceof PropertySource<?>
            && ((PropertySource<?>) source.getSource()).getSource() instanceof Random) {
      // Assume wrapped random sources use the source name as the prefix
      return containsDescendantOfForRandom(source.getName(), name);
    }
    return ConfigurationPropertyState.UNKNOWN;
  }

  private static ConfigurationPropertyState containsDescendantOfForRandom(
          String prefix, ConfigurationPropertyName name) {
    if (name.getNumberOfElements() > 1 && name.getElement(0, Form.DASHED).equals(prefix)) {
      return ConfigurationPropertyState.PRESENT;
    }
    return ConfigurationPropertyState.ABSENT;
  }

  @Override
  public Object getUnderlyingSource() {
    return this.propertySource;
  }

  protected PropertySource<?> getPropertySource() {
    return this.propertySource;
  }

  protected final PropertyMapper[] getMappers() {
    return this.mappers;
  }

  @Override
  public String toString() {
    return this.propertySource.toString();
  }

  /**
   * Create a new {@link DefaultConfigurationPropertySource} for the specified
   * {@link PropertySource}.
   *
   * @param source the source Framework {@link PropertySource}
   * @return a {@link DefaultConfigurationPropertySource} or
   * {@link DefaultIterableConfigurationPropertySource} instance
   */
  static DefaultConfigurationPropertySource from(PropertySource<?> source) {
    Assert.notNull(source, "Source is required");
    PropertyMapper[] mappers = getPropertyMappers(source);
    if (isFullEnumerable(source)) {
      return new DefaultIterableConfigurationPropertySource((EnumerablePropertySource<?>) source, mappers);
    }
    return new DefaultConfigurationPropertySource(source, mappers);
  }

  private static PropertyMapper[] getPropertyMappers(PropertySource<?> source) {
    if (source instanceof SystemEnvironmentPropertySource && hasSystemEnvironmentName(source)) {
      return SYSTEM_ENVIRONMENT_MAPPERS;
    }
    return DEFAULT_MAPPERS;
  }

  private static boolean hasSystemEnvironmentName(PropertySource<?> source) {
    String name = source.getName();
    return StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME.equals(name)
            || name.endsWith("-" + StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
  }

  private static boolean isFullEnumerable(PropertySource<?> source) {
    PropertySource<?> rootSource = getRootSource(source);
    if (rootSource.getSource() instanceof Map) {
      // Check we're not security restricted
      try {
        ((Map<?, ?>) rootSource.getSource()).size();
      }
      catch (UnsupportedOperationException ex) {
        return false;
      }
    }
    return (source instanceof EnumerablePropertySource);
  }

  private static PropertySource<?> getRootSource(PropertySource<?> source) {
    while (source.getSource() != null && source.getSource() instanceof PropertySource) {
      source = (PropertySource<?>) source.getSource();
    }
    return source;
  }

}
