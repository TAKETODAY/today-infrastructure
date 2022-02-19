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

import cn.taketoday.boot.origin.Origin;
import cn.taketoday.boot.origin.OriginProvider;
import cn.taketoday.boot.origin.OriginTrackedValue;
import cn.taketoday.core.style.ToStringCreator;
import cn.taketoday.util.Assert;
import cn.taketoday.util.ObjectUtils;

/**
 * A single configuration property obtained from a {@link ConfigurationPropertySource}
 * consisting of a {@link #getName() name}, {@link #getValue() value} and optional
 * {@link #getOrigin() origin}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
public final class ConfigurationProperty implements OriginProvider, Comparable<ConfigurationProperty> {

  private final ConfigurationPropertyName name;

  private final Object value;

  private final ConfigurationPropertySource source;

  private final Origin origin;

  public ConfigurationProperty(ConfigurationPropertyName name, Object value, Origin origin) {
    this(null, name, value, origin);
  }

  private ConfigurationProperty(ConfigurationPropertySource source, ConfigurationPropertyName name, Object value,
                                Origin origin) {
    Assert.notNull(name, "Name must not be null");
    Assert.notNull(value, "Value must not be null");
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
   * @since 4.0
   */
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
    boolean result = true;
    result = result && ObjectUtils.nullSafeEquals(this.name, other.name);
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
    return new ToStringCreator(this).append("name", this.name).append("value", this.value)
            .append("origin", this.origin).toString();
  }

  @Override
  public int compareTo(ConfigurationProperty other) {
    return this.name.compareTo(other.name);
  }

  static ConfigurationProperty of(ConfigurationPropertyName name, OriginTrackedValue value) {
    if (value == null) {
      return null;
    }
    return new ConfigurationProperty(name, value.getValue(), value.getOrigin());
  }

  static ConfigurationProperty of(ConfigurationPropertySource source, ConfigurationPropertyName name, Object value,
                                  Origin origin) {
    if (value == null) {
      return null;
    }
    return new ConfigurationProperty(source, name, value, origin);
  }

}
