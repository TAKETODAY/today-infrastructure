/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.logging;

import org.slf4j.spi.LocationAwareLogger;

import java.io.Serial;

/**
 * @author TODAY <br>
 * 2019-11-03 13:55
 */
class Slf4jLogger extends Logger {
  protected final String name;
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
  protected void logInternal(Level level, Object msg, Throwable t) {
    String message = String.valueOf(msg);
    switch (level) {
      case DEBUG -> target.debug(message, t);
      case ERROR -> target.error(message, t);
      case TRACE -> target.trace(message, t);
      case WARN -> target.warn(message, t);
      default -> target.info(message, t);
    }
  }

  @Override
  protected void logInternal(Level level, String format, Throwable t, Object[] args) {
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
  private final LocationAwareLogger log;

  public LocationAwareSlf4jLogger(LocationAwareLogger log) {
    super(log);
    this.log = log;
  }

  @Override
  protected void logInternal(Level level, Object msg, Throwable t) {
    String message = String.valueOf(msg);
    log.log(null, FQCN, getLevel(level), message, null, t);
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
  protected void logInternal(Level level, String format, Throwable t, Object[] args) {
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
