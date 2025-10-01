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

package infra.core.annotation;

import org.jspecify.annotations.Nullable;

import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Log facade used to handle annotation introspection failures (in particular
 * {@code TypeNotPresentExceptions}). Allows annotation processing to continue,
 * assuming that when Class attribute values are not resolvable the annotation
 * should effectively disappear.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/10/19 14:45
 */
enum IntrospectionFailureLogger {

  DEBUG {
    @Override
    public boolean isEnabled() {
      return getLogger().isDebugEnabled();
    }

    @Override
    public void log(String message) {
      getLogger().debug(message);
    }
  },

  INFO {
    @Override
    public boolean isEnabled() {
      return getLogger().isInfoEnabled();
    }

    @Override
    public void log(String message) {
      getLogger().info(message);
    }
  };

  @Nullable
  private static Logger logger;

  void log(String message, @Nullable Object source, Exception ex) {
    String on = (source != null ? " on " + source : "");
    log(message + on + ": " + ex);
  }

  abstract boolean isEnabled();

  abstract void log(String message);

  private static Logger getLogger() {
    Logger logger = IntrospectionFailureLogger.logger;
    if (logger == null) {
      logger = LoggerFactory.getLogger(IntrospectionFailureLogger.class);
      IntrospectionFailureLogger.logger = logger;
    }
    return logger;
  }

}
