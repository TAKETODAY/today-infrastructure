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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.context.support;

import java.util.Arrays;

import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.context.TestPropertySource;

/**
 * {@code MergedTestPropertySources} encapsulates the <em>merged</em>
 * property sources declared on a test class and all of its superclasses
 * via {@link TestPropertySource @TestPropertySource}.
 *
 * @author Sam Brannen
 * @see TestPropertySource
 * @since 4.0
 */
class MergedTestPropertySources {

  private static final MergedTestPropertySources empty = new MergedTestPropertySources(new String[0], new String[0]);

  private final String[] locations;

  private final String[] properties;

  /**
   * Factory for an <em>empty</em> {@code MergedTestPropertySources} instance.
   */
  static MergedTestPropertySources empty() {
    return empty;
  }

  /**
   * Create a {@code MergedTestPropertySources} instance with the supplied
   * {@code locations} and {@code properties}.
   *
   * @param locations the resource locations of properties files; may be
   * empty but never {@code null}
   * @param properties the properties in the form of {@code key=value} pairs;
   * may be empty but never {@code null}
   */
  MergedTestPropertySources(String[] locations, String[] properties) {
    Assert.notNull(locations, "The locations array must not be null");
    Assert.notNull(properties, "The properties array must not be null");
    this.locations = locations;
    this.properties = properties;
  }

  /**
   * Get the resource locations of properties files.
   *
   * @see TestPropertySource#locations()
   */
  String[] getLocations() {
    return this.locations;
  }

  /**
   * Get the properties in the form of <em>key-value</em> pairs.
   *
   * @see TestPropertySource#properties()
   */
  String[] getProperties() {
    return this.properties;
  }

  /**
   * Determine if the supplied object is equal to this {@code MergedTestPropertySources}
   * instance by comparing both object's {@linkplain #getLocations() locations}
   * and {@linkplain #getProperties() properties}.
   *
   * @since 4.0
   */
  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || other.getClass() != getClass()) {
      return false;
    }

    MergedTestPropertySources that = (MergedTestPropertySources) other;
    if (!Arrays.equals(this.locations, that.locations)) {
      return false;
    }
    return Arrays.equals(this.properties, that.properties);
  }

  /**
   * Generate a unique hash code for all properties of this
   * {@code MergedTestPropertySources} instance.
   *
   * @since 4.0
   */
  @Override
  public int hashCode() {
    int result = Arrays.hashCode(this.locations);
    result = 31 * result + Arrays.hashCode(this.properties);
    return result;
  }

  /**
   * Provide a String representation of this {@code MergedTestPropertySources}
   * instance.
   *
   * @since 4.0
   */
  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("locations", Arrays.toString(this.locations))
            .append("properties", Arrays.toString(this.properties))
            .toString();
  }

}
