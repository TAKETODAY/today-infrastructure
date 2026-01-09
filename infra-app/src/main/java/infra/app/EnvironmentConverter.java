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

import java.util.HashSet;

import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.env.PropertySource;
import infra.core.env.PropertySources;
import infra.core.env.StandardEnvironment;

/**
 * Utility class for converting one type of {@link Environment} to another.
 *
 * @author Ethan Rubinson
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/3 14:30
 */
final class EnvironmentConverter {

  private EnvironmentConverter() {
  }

  /**
   * Converts the given {@code environment} to the given {@link StandardEnvironment}
   * type. If the environment is already of the same type, no conversion is performed
   * and it is returned unchanged.
   *
   * @param environment the Environment to convert
   * @param type the type to convert the Environment to
   * @return the converted Environment
   */
  static StandardEnvironment convertIfNecessary(ConfigurableEnvironment environment, Class<? extends StandardEnvironment> type) {
    if (type.equals(environment.getClass())) {
      return (StandardEnvironment) environment;
    }
    return convertEnvironment(environment, type);
  }

  static StandardEnvironment convertEnvironment(ConfigurableEnvironment environment, Class<? extends StandardEnvironment> type) {
    StandardEnvironment result = createEnvironment(type);
    result.setActiveProfiles(environment.getActiveProfiles());
    result.setConversionService(environment.getConversionService());
    copyPropertySources(environment, result);
    return result;
  }

  static StandardEnvironment createEnvironment(Class<? extends StandardEnvironment> type) {
    try {
      return type.getDeclaredConstructor().newInstance();
    }
    catch (Exception ex) {
      return new StandardEnvironment();
    }
  }

  static void copyPropertySources(ConfigurableEnvironment source, StandardEnvironment target) {
    removePropertySources(target.getPropertySources());
    for (PropertySource<?> propertySource : source.getPropertySources()) {
      target.getPropertySources().addLast(propertySource);
    }
  }

  static void removePropertySources(PropertySources propertySources) {
    HashSet<String> names = new HashSet<>();
    for (PropertySource<?> propertySource : propertySources) {
      names.add(propertySource.getName());
    }
    for (String name : names) {
      propertySources.remove(name);
    }
  }

}
