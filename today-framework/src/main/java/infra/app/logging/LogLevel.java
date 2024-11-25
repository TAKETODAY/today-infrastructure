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

package infra.app.logging;

import infra.lang.Nullable;
import infra.logging.Logger;

/**
 * Logging levels supported by a {@link LoggingSystem}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public enum LogLevel {

  TRACE(Logger::trace),

  DEBUG(Logger::debug),

  INFO(Logger::info),

  WARN(Logger::warn),

  ERROR(Logger::error),

  FATAL(Logger::error),

  OFF(null);

  @Nullable
  private final LogMethod logMethod;

  LogLevel(@Nullable LogMethod logMethod) {
    this.logMethod = logMethod;
  }

  /**
   * Log a message to the given logger at this level.
   *
   * @param logger the logger
   * @param message the message to log
   */
  public void log(@Nullable Logger logger, Object message) {
    log(logger, message, null);
  }

  /**
   * Log a message to the given logger at this level.
   *
   * @param logger the logger
   * @param message the message to log
   * @param cause the cause to log
   */
  public void log(@Nullable Logger logger, Object message, @Nullable Throwable cause) {
    if (logger != null && logMethod != null) {
      logMethod.log(logger, message, cause);
    }
  }

  @FunctionalInterface
  private interface LogMethod {

    void log(Logger logger, Object message, @Nullable Throwable cause);

  }

}
