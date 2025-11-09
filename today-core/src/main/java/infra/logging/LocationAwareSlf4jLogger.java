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

