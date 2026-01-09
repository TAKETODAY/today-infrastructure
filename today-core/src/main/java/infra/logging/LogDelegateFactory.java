/*
 * Copyright 2002-present the original author or authors.
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

package infra.logging;

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

  private LogDelegateFactory() {
  }

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
