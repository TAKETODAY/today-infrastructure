/*
 * Copyright 2002-present the original author or authors.
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

package infra.web.mock;

import org.jspecify.annotations.Nullable;

import infra.core.env.ConfigurableEnvironment;
import infra.core.env.PropertySource;
import infra.mock.api.MockConfig;
import infra.mock.api.MockContext;

/**
 * Specialization of {@link ConfigurableEnvironment} allowing initialization of
 * servlet-related {@link PropertySource} objects at the
 * earliest moment that the {@link MockContext} and (optionally) {@link MockConfig}
 * become available.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 17:07
 */
public interface ConfigurableWebEnvironment extends ConfigurableEnvironment {

  /**
   * Replace any {@linkplain
   * PropertySource.StubPropertySource stub property source}
   * instances acting as placeholders with real servlet context/config property sources
   * using the given parameters.
   *
   * @param mockContext the {@link MockContext} (may not be {@code null})
   * @param mockConfig the {@link MockConfig} ({@code null} if not available)
   */
  void initPropertySources(@Nullable MockContext mockContext, @Nullable MockConfig mockConfig);

}
