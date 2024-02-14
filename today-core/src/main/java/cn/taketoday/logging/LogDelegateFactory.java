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

package cn.taketoday.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Factory for common {@link Logger} delegates with logging conventions.
 *
 * <p>Mainly for internal use within the framework
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class LogDelegateFactory {

  private LogDelegateFactory() { }

  /**
   * Create a composite logger that delegates to a primary or falls back on a
   * secondary logger if logging for the primary logger is not enabled.
   * <p>This may be used for fallback logging from lower-level packages that
   * logically should log together with some higher-level package but the two
   * don't happen to share a suitable parent package (e.g. logging for the web
   * and lower-level http and codec packages). For such cases the primary
   * (class-based) logger can be wrapped with a shared fallback logger.
   *
   * @param primaryLogger primary logger to try first
   * @param secondaryLogger secondary logger
   * @param tertiaryLoggers optional vararg of further fallback loggers
   * @return the resulting composite logger for the related categories
   */
  public static Logger getCompositeLog(Logger primaryLogger, Logger secondaryLogger, Logger... tertiaryLoggers) {
    List<Logger> loggers = new ArrayList<>(2 + tertiaryLoggers.length);
    loggers.add(primaryLogger);
    loggers.add(secondaryLogger);
    Collections.addAll(loggers, tertiaryLoggers);
    return new CompositeLogger(loggers, primaryLogger.getName());
  }

  /**
   * Create a "hidden" logger with a category name prefixed with "_", thus
   * precluding it from being enabled together with other log categories from
   * the same package. This is useful for specialized output that is either
   * too verbose or otherwise optional or unnecessary to see all the time.
   *
   * @param clazz the class for which to create a logger
   * @return a Logger with the category {@code "_" + fully-qualified class name}
   */
  public static Logger getHiddenLog(Class<?> clazz) {
    return getHiddenLog(clazz.getName());
  }

  /**
   * Create a "hidden" logger with a category name prefixed with "_", thus
   * precluding it from being enabled together with other log categories from
   * the same package. This is useful for specialized output that is either
   * too verbose or otherwise optional or unnecessary to see all the time.
   *
   * @param category the log category to use
   * @return a Logger with the category {@code "_" + category}
   */
  public static Logger getHiddenLog(String category) {
    return LoggerFactory.getLogger("_" + category);
  }

}
