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

import java.io.Serial;
import java.util.List;

/**
 * Implementation of {@link Logger} that wraps a list of loggers and delegates
 * to the first one for which logging is enabled at the given level.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see LogDelegateFactory#getCompositeLog
 * @since 4.0
 */
final class CompositeLogger extends Logger {

  @Serial
  private static final long serialVersionUID = 1L;

  static final Logger NO_OP_LOG = new NoOpLogger();

  private final Logger errorLogger;

  private final Logger warnLogger;

  private final Logger infoLogger;

  private final Logger debugLogger;

  private final Logger traceLogger;

  private final String name;

  /**
   * Constructor with list of loggers. For optimal performance, the constructor
   * checks and remembers which logger is on for each log category.
   *
   * @param loggers the loggers to use
   * @param name logger name
   */
  public CompositeLogger(List<Logger> loggers, String name) {
    super(initLogger(loggers, Level.DEBUG) != NO_OP_LOG);
    this.errorLogger = initLogger(loggers, Level.ERROR);
    this.warnLogger = initLogger(loggers, Level.WARN);
    this.infoLogger = initLogger(loggers, Level.INFO);
    this.debugLogger = initLogger(loggers, Level.DEBUG);
    this.traceLogger = initLogger(loggers, Level.TRACE);
    this.name = name;
  }

  private static Logger initLogger(List<Logger> loggers, Level level) {
    for (Logger logger : loggers) {
      if (logger.isEnabled(level)) {
        return logger;
      }
    }
    return NO_OP_LOG;
  }

  @Override
  public boolean isErrorEnabled() {
    return this.errorLogger != NO_OP_LOG;
  }

  @Override
  public boolean isWarnEnabled() {
    return this.warnLogger != NO_OP_LOG;
  }

  @Override
  public boolean isInfoEnabled() {
    return this.infoLogger != NO_OP_LOG;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isTraceEnabled() {
    return this.traceLogger != NO_OP_LOG;
  }

  private Logger logger(Level level) {
    return switch (level) {
      case INFO -> infoLogger;
      case WARN -> warnLogger;
      case ERROR -> errorLogger;
      case DEBUG -> debugLogger;
      case TRACE -> traceLogger;
    };
  }

  @Override
  protected void logInternal(Level level, @Nullable String msg, @Nullable Throwable t, @Nullable Object @Nullable [] args) {
    logger(level).logInternal(level, msg, t, args);
  }

}
