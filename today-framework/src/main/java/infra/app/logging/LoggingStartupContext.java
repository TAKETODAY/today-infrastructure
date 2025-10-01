/*
 * Copyright 2017 - 2025 the original author or authors.
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

  @Nullable
  private final ConfigurableEnvironment environment;

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
  @Nullable
  public Environment getEnvironment() {
    return this.environment;
  }

}
