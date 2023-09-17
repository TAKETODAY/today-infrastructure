/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.app.loader.tools;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import cn.taketoday.util.ClassUtils;

/**
 * Utility to initialize logback (when present) to use INFO level logging.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class LogbackInitializer {

  public static void initialize() {
    if (ClassUtils.isPresent("org.slf4j.LoggerFactory", null)
            && ClassUtils.isPresent("ch.qos.logback.classic.Logger", null)) {
      new Initializer().setRootLogLevel();
    }
  }

  private static class Initializer {

    void setRootLogLevel() {
      ILoggerFactory factory = LoggerFactory.getILoggerFactory();
      Logger logger = factory.getLogger(Logger.ROOT_LOGGER_NAME);
      ((ch.qos.logback.classic.Logger) logger).setLevel(Level.INFO);
    }

  }

}
