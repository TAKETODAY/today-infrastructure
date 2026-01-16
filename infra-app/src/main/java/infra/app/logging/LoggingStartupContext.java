/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.app.logging;

import org.jspecify.annotations.Nullable;

import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;

/**
 * Context passed to the {@link LoggingSystem} during initialization.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class LoggingStartupContext {

  private final @Nullable ConfigurableEnvironment environment;

  /**
   * Create a new {@link LoggingStartupContext} instance.
   *
   * @param environment the Infra environment.
   */
  public LoggingStartupContext(@Nullable ConfigurableEnvironment environment) {
    this.environment = environment;
  }

  /**
   * Return the Infra environment if available.
   *
   * @return the {@link Environment} or {@code null}
   */
  public @Nullable Environment getEnvironment() {
    return this.environment;
  }

}
