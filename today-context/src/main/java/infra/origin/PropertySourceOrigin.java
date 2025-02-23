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

package infra.origin;

import infra.core.env.PropertySource;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * {@link Origin} from a {@link PropertySource}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class PropertySourceOrigin implements Origin, OriginProvider {

  private final PropertySource<?> propertySource;

  private final String propertyName;

  @Nullable
  private final Origin origin;

  /**
   * Create a new {@link PropertySourceOrigin} instance.
   *
   * @param propertySource the property source
   * @param propertyName the name from the property source
   */
  public PropertySourceOrigin(PropertySource<?> propertySource, String propertyName) {
    this(propertySource, propertyName, null);
  }

  /**
   * Create a new {@link PropertySourceOrigin} instance.
   *
   * @param propertySource the property source
   * @param propertyName the name from the property source
   * @param origin the actual origin for the source if known
   * @since 5.0
   */
  public PropertySourceOrigin(PropertySource<?> propertySource, String propertyName, @Nullable Origin origin) {
    Assert.notNull(propertySource, "PropertySource is required");
    Assert.hasLength(propertyName, "PropertyName must not be empty");
    this.propertySource = propertySource;
    this.propertyName = propertyName;
    this.origin = origin;
  }

  /**
   * Return the origin {@link PropertySource}.
   *
   * @return the origin property source
   */
  public PropertySource<?> getPropertySource() {
    return this.propertySource;
  }

  /**
   * Return the property name that was used when obtaining the original value from the
   * {@link #getPropertySource() property source}.
   *
   * @return the origin property name
   */
  public String getPropertyName() {
    return this.propertyName;
  }

  /**
   * Return the actual origin for the source if known.
   *
   * @return the actual source origin
   * @since 5.0
   */
  @Nullable
  @Override
  public Origin getOrigin() {
    return this.origin;
  }

  @Override
  public String toString() {
    return "\"" + this.propertyName + "\" from property source \"" + this.propertySource.getName() + "\"";
  }

  /**
   * Get an {@link Origin} for the given {@link PropertySource} and
   * {@code propertyName}. Will either return an {@link OriginLookup} result or a
   * {@link PropertySourceOrigin}.
   *
   * @param propertySource the origin property source
   * @param name the property name
   * @return the property origin
   */
  public static Origin get(PropertySource<?> propertySource, String name) {
    Origin origin = OriginLookup.getOrigin(propertySource, name);
    return origin != null ? origin : new PropertySourceOrigin(propertySource, name);
  }

}
