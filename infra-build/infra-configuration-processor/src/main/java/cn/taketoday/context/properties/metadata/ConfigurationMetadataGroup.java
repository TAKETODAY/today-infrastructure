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

package cn.taketoday.context.properties.metadata;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Gather a collection of {@link ConfigurationMetadataProperty properties} that are
 * sharing a {@link #getId() common prefix}. Provide access to all the
 * {@link ConfigurationMetadataSource sources} that have contributed properties to the
 * group.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ConfigurationMetadataGroup implements Serializable {

  private final String id;

  private final Map<String, ConfigurationMetadataSource> sources = new HashMap<>();

  private final Map<String, ConfigurationMetadataProperty> properties = new HashMap<>();

  public ConfigurationMetadataGroup(String id) {
    this.id = id;
  }

  /**
   * Return the id of the group, used as a common prefix for all properties associated
   * to it.
   *
   * @return the id of the group
   */
  public String getId() {
    return this.id;
  }

  /**
   * Return the {@link ConfigurationMetadataSource sources} defining the properties of
   * this group.
   *
   * @return the sources of the group
   */
  public Map<String, ConfigurationMetadataSource> getSources() {
    return this.sources;
  }

  /**
   * Return the {@link ConfigurationMetadataProperty properties} defined in this group.
   * <p>
   * A property may appear more than once for a given source, potentially with
   * conflicting type or documentation. This is a "merged" view of the properties of
   * this group.
   *
   * @return the properties of the group
   * @see ConfigurationMetadataSource#getProperties()
   */
  public Map<String, ConfigurationMetadataProperty> getProperties() {
    return this.properties;
  }

}
