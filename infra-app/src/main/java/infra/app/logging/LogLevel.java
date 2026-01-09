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

package infra.app.logging;

import org.jspecify.annotations.Nullable;

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
