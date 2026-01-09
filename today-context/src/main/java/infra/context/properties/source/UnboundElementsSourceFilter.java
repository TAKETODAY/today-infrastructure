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

package infra.context.properties.source;

import java.util.Set;
import java.util.function.Function;

import infra.core.env.PropertySource;
import infra.core.env.StandardEnvironment;

/**
 * Function used to determine if a {@link ConfigurationPropertySource} should be included
 * when determining unbound elements. If the underlying {@link PropertySource} is a
 * systemEnvironment or systemProperties property source, it will not be considered for
 * unbound element failures.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class UnboundElementsSourceFilter implements Function<ConfigurationPropertySource, Boolean> {

  private static final Set<String> BENIGN_PROPERTY_SOURCE_NAMES = Set.of(
          StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
          StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME
  );

  @Override
  public Boolean apply(ConfigurationPropertySource configurationPropertySource) {
    Object underlyingSource = configurationPropertySource.getUnderlyingSource();
    if (underlyingSource instanceof PropertySource) {
      String name = ((PropertySource<?>) underlyingSource).getName();
      return !BENIGN_PROPERTY_SOURCE_NAMES.contains(name);
    }
    return true;
  }

}
