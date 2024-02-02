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

package cn.taketoday.framework.context.config;

import cn.taketoday.lang.Nullable;
import cn.taketoday.origin.Origin;
import cn.taketoday.origin.OriginProvider;
import cn.taketoday.util.StringUtils;

/**
 * A user specified location that can be {@link ConfigDataLocationResolver resolved} to
 * one or more {@link ConfigDataResource config data resources}. A
 * {@link ConfigDataLocation} is a simple wrapper around a {@link String} value. The exact
 * format of the value will depend on the underlying technology, but is usually a URL like
 * syntax consisting of a prefix and path. For example, {@code crypt:somehost/somepath}.
 * <p>
 * Locations can be mandatory or {@link #isOptional() optional}. Optional locations are
 * prefixed with {@code optional:}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Phillip Webb
 * @since 4.0
 */
public final class ConfigDataLocation implements OriginProvider {

  /**
   * Prefix used to indicate that a {@link ConfigDataResource} is optional.
   */
  public static final String OPTIONAL_PREFIX = "optional:";

  private final boolean optional;

  private final String value;

  @Nullable
  private final Origin origin;

  private ConfigDataLocation(boolean optional, String value, @Nullable Origin origin) {
    this.value = value;
    this.origin = origin;
    this.optional = optional;
  }

  /**
   * Return if the location is optional and should ignore
   * {@link ConfigDataNotFoundException}.
   *
   * @return if the location is optional
   */
  public boolean isOptional() {
    return this.optional;
  }

  /**
   * Return the value of the location (always excluding any user specified
   * {@code optional:} prefix).
   *
   * @return the location value
   */
  public String getValue() {
    return this.value;
  }

  /**
   * Return if {@link #getValue()} has the specified prefix.
   *
   * @param prefix the prefix to check
   * @return if the value has the prefix
   */
  public boolean hasPrefix(String prefix) {
    return this.value.startsWith(prefix);
  }

  /**
   * Return {@link #getValue()} with the specified prefix removed. If the location does
   * not have the given prefix then the {@link #getValue()} is returned unchanged.
   *
   * @param prefix the prefix to check
   * @return the value with the prefix removed
   */
  public String getNonPrefixedValue(String prefix) {
    if (hasPrefix(prefix)) {
      return this.value.substring(prefix.length());
    }
    return this.value;
  }

  @Nullable
  @Override
  public Origin getOrigin() {
    return this.origin;
  }

  /**
   * Return an array of {@link ConfigDataLocation} elements built by splitting this
   * {@link ConfigDataLocation} around a delimiter of {@code ";"}.
   *
   * @return the split locations
   */
  public ConfigDataLocation[] split() {
    return split(";");
  }

  /**
   * Return an array of {@link ConfigDataLocation} elements built by splitting this
   * {@link ConfigDataLocation} around the specified delimiter.
   *
   * @param delimiter the delimiter to split on
   * @return the split locations
   */
  public ConfigDataLocation[] split(String delimiter) {
    String[] values = StringUtils.delimitedListToStringArray(toString(), delimiter);
    ConfigDataLocation[] result = new ConfigDataLocation[values.length];

    int i = 0;
    Origin origin = getOrigin();
    if (origin != null) {
      for (String value : values) {
        ConfigDataLocation dataLocation = valueOf(value);
        if (dataLocation != null) {
          result[i++] = dataLocation.withOrigin(origin);
        }
      }
    }
    else {
      for (String value : values) {
        ConfigDataLocation dataLocation = valueOf(value);
        if (dataLocation != null) {
          result[i++] = dataLocation;
        }
      }
    }
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ConfigDataLocation other)) {
      return false;
    }
    return this.value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return this.value.hashCode();
  }

  @Override
  public String toString() {
    return (!this.optional) ? this.value : OPTIONAL_PREFIX + this.value;
  }

  /**
   * Create a new {@link ConfigDataLocation} with a specific {@link Origin}.
   *
   * @param origin the origin to set
   * @return a new {@link ConfigDataLocation} instance.
   */
  ConfigDataLocation withOrigin(@Nullable Origin origin) {
    return new ConfigDataLocation(this.optional, this.value, origin);
  }

  /**
   * Factory method to create a new {@link ConfigDataLocation} from a string.
   *
   * @param location the location string
   * @return a {@link ConfigDataLocation} instance or {@code null} if no location was
   * provided
   */
  @Nullable
  public static ConfigDataLocation valueOf(@Nullable String location) {
    boolean optional = location != null && location.startsWith(OPTIONAL_PREFIX);
    if (optional) {
      location = location.substring(OPTIONAL_PREFIX.length());
    }
    if (StringUtils.isBlank(location)) {
      return null;
    }
    return new ConfigDataLocation(optional, location, null);
  }

}
