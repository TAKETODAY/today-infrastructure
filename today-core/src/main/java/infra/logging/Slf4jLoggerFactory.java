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

import org.slf4j.spi.LocationAwareLogger;

/**
 * LoggerFactory for slf4j
 */
final class Slf4jLoggerFactory extends LoggerFactory {

  Slf4jLoggerFactory() {
    org.slf4j.Logger.class.getName();
    SLF4JBridgeHandler.install(); // @since 4.0
  }

  @Override
  protected Logger createLogger(String name) {
    return createLog(name);
  }

  static Logger createLog(String name) {
    var target = org.slf4j.LoggerFactory.getLogger(name);
    return target instanceof LocationAwareLogger ?
            new LocationAwareSlf4jLogger((LocationAwareLogger) target) : new Slf4jLogger(target);
  }

}
