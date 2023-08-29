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

import java.util.Map;

/**
 * A repository of configuration metadata.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ConfigurationMetadataRepository {

  /**
   * Defines the name of the "root" group, that is the group that gathers all the
   * properties that aren't attached to a specific group.
   */
  String ROOT_GROUP = "_ROOT_GROUP_";

  /**
   * Return the groups, indexed by id.
   *
   * @return all configuration meta-data groups
   */
  Map<String, ConfigurationMetadataGroup> getAllGroups();

  /**
   * Return the properties, indexed by id.
   *
   * @return all configuration meta-data properties
   */
  Map<String, ConfigurationMetadataProperty> getAllProperties();

}
