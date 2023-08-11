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

package cn.taketoday.test.context.support;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.core.io.PropertySourceDescriptor;
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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TestPropertySource
 * @since 4.0
 */
class MergedTestPropertySources {

  private static final MergedTestPropertySources empty = new MergedTestPropertySources(List.of(), new String[0]);

  private final List<PropertySourceDescriptor> descriptors;

  private final String[] properties;

  /**
   * Factory for an <em>empty</em> {@code MergedTestPropertySources} instance.
   */
  static MergedTestPropertySources empty() {
    return empty;
  }

  /**
   * Create a {@code MergedTestPropertySources} instance with the supplied
   * {@code descriptors} and {@code properties}.
   *
   * @param descriptors the descriptors for resource locations
   * of properties files; may be empty but never {@code null}
   * @param properties the properties in the form of {@code key=value} pairs;
   * may be empty but never {@code null}
   */
  MergedTestPropertySources(List<PropertySourceDescriptor> descriptors, String[] properties) {
    Assert.notNull(descriptors, "The descriptors list must not be null");
    Assert.notNull(properties, "The properties array must not be null");
    this.descriptors = descriptors;
    this.properties = properties;
  }

  /**
   * Get the descriptors for resource locations of properties files.
   *
   * @see TestPropertySource#locations
   * @see TestPropertySource#encoding
   * @see TestPropertySource#factory
   */
  List<PropertySourceDescriptor> getPropertySourceDescriptors() {
    return this.descriptors;
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
   * instance by comparing both objects' {@linkplain #getPropertySourceDescriptors()
   * descriptors} and {@linkplain #getProperties() properties}.
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
    if (!this.descriptors.equals(that.descriptors)) {
      return false;
    }
    return Arrays.equals(this.properties, that.properties);
  }

  /**
   * Generate a unique hash code for all properties of this
   * {@code MergedTestPropertySources} instance.
   */
  @Override
  public int hashCode() {
    int result = this.descriptors.hashCode();
    result = 31 * result + Arrays.hashCode(this.properties);
    return result;
  }

  /**
   * Provide a String representation of this {@code MergedTestPropertySources}
   * instance.
   */
  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("descriptors", this.descriptors)
        .append("properties", this.properties)
        .toString();
  }

}
