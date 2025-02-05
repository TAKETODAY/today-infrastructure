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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * LoggerFactory for java logging
 */
final class JavaLoggingFactory extends LoggerFactory {
  static {
    URL resource = Thread.currentThread().getContextClassLoader().getResource("logging.properties");
    if (resource != null) {
      try (InputStream inputStream = resource.openStream()) {
        LogManager.getLogManager().readConfiguration(inputStream);
      }
      catch (SecurityException | IOException e) {
        System.err.println("Can't load config file 'logging.properties'");
        e.printStackTrace();
      }
    }
  }

  @Override
  protected JavaLoggingLogger createLogger(String name) {
    Logger logger = Logger.getLogger(name);
    return new JavaLoggingLogger(logger, logger.isLoggable(java.util.logging.Level.FINER));
  }
}
