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

package cn.taketoday.web.view.freemarker;

import freemarker.template.Configuration;

/**
 * Interface to be implemented by objects that configure and manage a
 * FreeMarker Configuration object in a web environment. Detected and
 * used by {@link FreeMarkerView}.
 *
 * @author Darren Davison
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see FreeMarkerConfigurer
 * @see FreeMarkerView
 * @since 4.0
 */
public interface FreeMarkerConfig {

  /**
   * Return the FreeMarker {@link Configuration} object for the current
   * web application context.
   * <p>A FreeMarker Configuration object may be used to set FreeMarker
   * properties and shared objects, and allows to retrieve templates.
   *
   * @return the FreeMarker Configuration
   */
  Configuration getConfiguration();

}
