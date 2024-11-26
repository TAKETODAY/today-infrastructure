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

package infra.app.env;

import java.io.IOException;
import java.util.List;

import infra.core.env.PropertySource;
import infra.core.io.Resource;
import infra.lang.TodayStrategies;

/**
 * Strategy interface located via {@link TodayStrategies} and used to load a
 * {@link PropertySource}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 4.0
 */
public interface PropertySourceLoader {

  /**
   * Returns the file extensions that the loader supports (excluding the '.').
   *
   * @return the file extensions
   */
  String[] getFileExtensions();

  /**
   * Load the resource into one or more property sources. Implementations may either
   * return a list containing a single source, or in the case of a multi-document format
   * such as yaml a source for each document in the resource.
   *
   * @param name the root name of the property source. If multiple documents are loaded
   * an additional suffix should be added to the name for each source loaded.
   * @param resource the resource to load
   * @return a list property sources
   * @throws IOException if the source cannot be loaded
   */
  List<PropertySource<?>> load(String name, Resource resource) throws IOException;

}
