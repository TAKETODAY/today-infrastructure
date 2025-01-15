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

import java.io.Serial;

import infra.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 2019-11-03 13:55
 */
class Slf4jLogger extends Logger {

  @Serial
  private static final long serialVersionUID = 1L;

  private final String name;

  private final transient org.slf4j.Logger target;

  Slf4jLogger(org.slf4j.Logger target) {
    super(target.isDebugEnabled());
    this.target = target;
    this.name = target.getName();
  }

  @Override
  public boolean isTraceEnabled() {
    return debugEnabled && target.isTraceEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return target.isInfoEnabled();
  }

  @Override
  public boolean isWarnEnabled() {
    return target.isWarnEnabled();
  }

  @Override
  public boolean isErrorEnabled() {
    return target.isErrorEnabled();
  }

  @Override
  public String getName() {
    return target.getName();
  }

  @Override
  protected void logInternal(Level level, String format, @Nullable Throwable t, @Nullable Object[] args) {
    final String msg = MessageFormatter.format(format, args);
    switch (level) {
      case DEBUG -> target.debug(msg, t);
      case ERROR -> target.error(msg, t);
      case TRACE -> target.trace(msg, t);
      case WARN -> target.warn(msg, t);
      default -> target.info(msg, t);
    }
  }

  @Serial
  protected Object readResolve() {
    return Slf4jLoggerFactory.createLog(this.name);
  }
}

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
  protected void logInternal(Level level, String format, @Nullable Throwable t, @Nullable Object[] args) {
    log.log(null, FQCN, getLevel(level), format, args, t);
  }
}

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
    org.slf4j.Logger target = org.slf4j.LoggerFactory.getLogger(name);
    return target instanceof LocationAwareLogger ?
            new LocationAwareSlf4jLogger((LocationAwareLogger) target) : new Slf4jLogger(target);
  }

}
