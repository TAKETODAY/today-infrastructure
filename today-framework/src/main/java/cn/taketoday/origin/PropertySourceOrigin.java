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

package cn.taketoday.origin;

import cn.taketoday.core.env.PropertySource;
import cn.taketoday.lang.Assert;

/**
 * {@link Origin} from a {@link PropertySource}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class PropertySourceOrigin implements Origin {

  private final PropertySource<?> propertySource;

  private final String propertyName;

  /**
   * Create a new {@link PropertySourceOrigin} instance.
   *
   * @param propertySource the property source
   * @param propertyName the name from the property source
   */
  public PropertySourceOrigin(PropertySource<?> propertySource, String propertyName) {
    Assert.notNull(propertySource, "PropertySource must not be null");
    Assert.hasLength(propertyName, "PropertyName must not be empty");
    this.propertySource = propertySource;
    this.propertyName = propertyName;
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
