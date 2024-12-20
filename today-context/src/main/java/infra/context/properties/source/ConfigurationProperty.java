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

import infra.core.style.ToStringBuilder;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.origin.Origin;
import infra.origin.OriginProvider;
import infra.util.ObjectUtils;

/**
 * A single configuration property obtained from a {@link ConfigurationPropertySource}
 * consisting of a {@link #getName() name}, {@link #getValue() value} and optional
 * {@link #getOrigin() origin}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class ConfigurationProperty implements OriginProvider, Comparable<ConfigurationProperty> {

  private final ConfigurationPropertyName name;

  private final Object value;

  @Nullable
  private final ConfigurationPropertySource source;

  @Nullable
  private final Origin origin;

  public ConfigurationProperty(ConfigurationPropertyName name, Object value, @Nullable Origin origin) {
    this(null, name, value, origin);
  }

  private ConfigurationProperty(@Nullable ConfigurationPropertySource source,
          ConfigurationPropertyName name, Object value, @Nullable Origin origin) {
    Assert.notNull(name, "Name is required");
    Assert.notNull(value, "Value is required");
    this.source = source;
    this.name = name;
    this.value = value;
    this.origin = origin;
  }

  /**
   * Return the {@link ConfigurationPropertySource} that provided the property or
   * {@code null} if the source is unknown.
   *
   * @return the configuration property source
   */
  @Nullable
  public ConfigurationPropertySource getSource() {
    return this.source;
  }

  /**
   * Return the name of the configuration property.
   *
   * @return the configuration property name
   */
  public ConfigurationPropertyName getName() {
    return this.name;
  }

  /**
   * Return the value of the configuration property.
   *
   * @return the configuration property value
   */
  public Object getValue() {
    return this.value;
  }

  @Override
  @Nullable
  public Origin getOrigin() {
    return this.origin;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ConfigurationProperty other = (ConfigurationProperty) obj;
    boolean result = ObjectUtils.nullSafeEquals(this.name, other.name);
    result = result && ObjectUtils.nullSafeEquals(this.value, other.value);
    return result;
  }

  @Override
  public int hashCode() {
    int result = ObjectUtils.nullSafeHashCode(this.name);
    result = 31 * result + ObjectUtils.nullSafeHashCode(this.value);
    return result;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("name", this.name)
            .append("value", this.value)
            .append("origin", this.origin)
            .toString();
  }

  @Override
  public int compareTo(ConfigurationProperty other) {
    return this.name.compareTo(other.name);
  }

  @Nullable
  static ConfigurationProperty of(@Nullable ConfigurationPropertySource source,
          ConfigurationPropertyName name, @Nullable Object value, @Nullable Origin origin) {
    if (value == null) {
      return null;
    }
    return new ConfigurationProperty(source, name, value, origin);
  }

}
