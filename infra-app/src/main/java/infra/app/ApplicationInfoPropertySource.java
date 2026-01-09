/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import infra.core.ApplicationPid;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertySource;
import infra.core.env.PropertySources;
import infra.lang.VersionExtractor;
import infra.origin.Origin;
import infra.origin.OriginLookup;
import infra.util.StringUtils;

/**
 * {@link PropertySource} which provides information about the application, like the
 * process ID (PID) or the version.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class ApplicationInfoPropertySource extends MapPropertySource implements OriginLookup<String> {

  static final String NAME = "applicationInfo";

  ApplicationInfoPropertySource(@Nullable Class<?> mainClass) {
    super(NAME, getProperties(VersionExtractor.forClass(mainClass)));
  }

  ApplicationInfoPropertySource(@Nullable String applicationVersion) {
    super(NAME, getProperties(applicationVersion));
  }

  @Nullable
  @Override
  public Origin getOrigin(String key) {
    return null;
  }

  @Override
  public boolean isImmutable() {
    return true;
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
