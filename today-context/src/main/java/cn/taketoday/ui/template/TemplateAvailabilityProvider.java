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

package cn.taketoday.ui.template;

import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.TodayStrategies;

/**
 * Indicates the availability of view templates for a particular templating engine such as
 * FreeMarker.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface TemplateAvailabilityProvider {

  String DEFAULT_TEMPLATE_LOADER_PATH = TodayStrategies.getProperty(
          "template.default.loader.path", "classpath:templates/");

  /**
   * Returns {@code true} if a template is available for the given {@code view}.
   *
   * @param template the template name
   * @param environment the environment
   * @param classLoader the class loader
   * @param resourceLoader the resource loader
   * @return if the template is available
   */
  boolean isTemplateAvailable(String template, Environment environment,
          ClassLoader classLoader, ResourceLoader resourceLoader);

}
