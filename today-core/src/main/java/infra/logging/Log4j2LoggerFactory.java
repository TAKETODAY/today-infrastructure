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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;

/**
 * LoggerFactory for log4j2
 */
final class Log4j2LoggerFactory extends LoggerFactory {

  Log4j2LoggerFactory() {
    LogManager.class.getName();
  }

  @Override
  protected Logger createLogger(String name) {
    LoggerContext context = Log4j2Logger.loggerContext;
    if (context == null) {
      // Circular call in early-init scenario -> static field not initialized yet
      context = LogManager.getContext(Log4j2Logger.class.getClassLoader(), false);
    }
    ExtendedLogger logger = context.getLogger(name);
    return new Log4j2Logger(logger);
  }
}
