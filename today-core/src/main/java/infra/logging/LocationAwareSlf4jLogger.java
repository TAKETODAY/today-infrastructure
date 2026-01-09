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

package infra.logging;

import org.jspecify.annotations.Nullable;
import org.slf4j.spi.LocationAwareLogger;

import java.io.Serial;

/**
 * LocationAwareLogger
 */
final class LocationAwareSlf4jLogger extends Slf4jLogger {

  @Serial
  private static final long serialVersionUID = 1L;

  private final LocationAwareLogger log;

  public LocationAwareSlf4jLogger(LocationAwareLogger log) {
    super(log);
    this.log = log;
  }

  private static int getLevel(Level level) {
    return switch (level) {
      case DEBUG -> LocationAwareLogger.DEBUG_INT;
      case ERROR -> LocationAwareLogger.ERROR_INT;
      case TRACE -> LocationAwareLogger.TRACE_INT;
      case WARN -> LocationAwareLogger.WARN_INT;
      default -> LocationAwareLogger.INFO_INT;
    };
  }

  @Override
  protected void logInternal(Level level, @Nullable String format, @Nullable Throwable t, @Nullable Object @Nullable [] args) {
    log.log(null, FQCN, getLevel(level), format, args, t);
  }
}

