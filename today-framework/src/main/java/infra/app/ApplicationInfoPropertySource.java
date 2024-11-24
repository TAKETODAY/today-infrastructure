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

package infra.app;

import java.util.HashMap;
import java.util.Map;

import infra.core.ApplicationPid;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertySource;
import infra.core.env.PropertySources;
import infra.lang.Nullable;
import infra.lang.VersionExtractor;
import infra.util.StringUtils;

/**
 * {@link PropertySource} which provides information about the application, like the
 * process ID (PID) or the version.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class ApplicationInfoPropertySource extends MapPropertySource {

  static final String NAME = "applicationInfo";

  ApplicationInfoPropertySource(@Nullable Class<?> mainClass) {
    super(NAME, getProperties(VersionExtractor.forClass(mainClass)));
  }

  ApplicationInfoPropertySource(@Nullable String applicationVersion) {
    super(NAME, getProperties(applicationVersion));
  }

  private static Map<String, Object> getProperties(@Nullable String applicationVersion) {
    Map<String, Object> result = new HashMap<>();
    if (StringUtils.hasText(applicationVersion)) {
      result.put("app.version", applicationVersion);
    }
    ApplicationPid applicationPid = new ApplicationPid();
    if (applicationPid.isAvailable()) {
      result.put("app.pid", applicationPid.toLong());
    }
    return result;
  }

  /**
   * Moves the {@link ApplicationInfoPropertySource} to the end of the environment's
   * property sources.
   *
   * @param environment the environment
   */
  static void moveToEnd(ConfigurableEnvironment environment) {
    PropertySources propertySources = environment.getPropertySources();
    PropertySource<?> propertySource = propertySources.remove(NAME);
    if (propertySource != null) {
      propertySources.addLast(propertySource);
    }
  }

}
