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

package infra.ui.template;

import infra.core.env.Environment;
import infra.core.io.ResourceLoader;
import infra.lang.TodayStrategies;

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
