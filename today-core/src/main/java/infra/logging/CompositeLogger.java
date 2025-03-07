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

import java.io.Serial;
import java.util.List;

import infra.lang.Nullable;

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

  private static final Logger NO_OP_LOG = new NoOpLogger();

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
  protected void logInternal(Level level, String msg, @Nullable Throwable t, @Nullable Object[] args) {
    logger(level).logInternal(level, msg, t, args);
  }

}
