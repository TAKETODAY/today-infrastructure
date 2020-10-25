/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.context.logger;

import org.slf4j.spi.LocationAwareLogger;

/**
 * @author TODAY <br>
 *         2019-11-03 13:55
 */
public class Slf4jLogger extends AbstractLogger {

  private final org.slf4j.Logger target;

  public Slf4jLogger(String className) {
    target = org.slf4j.LoggerFactory.getLogger(className);
  }

  @Override
  public boolean isTraceEnabled() {
    return target.isTraceEnabled();
  }

  @Override
  public boolean isDebugEnabled() {
    return target.isDebugEnabled();
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
  protected void logInternal(Level level, String format, Throwable t, Object[] args) {

    if (target instanceof LocationAwareLogger) { // MessageFormatter.format(format, args)
      int i = 0;
      switch (level) {
        case DEBUG:
          i = LocationAwareLogger.DEBUG_INT;
          break;
        case ERROR:
          i = LocationAwareLogger.ERROR_INT;
          break;
        case TRACE:
          i = LocationAwareLogger.TRACE_INT;
          break;
        case WARN:
          i = LocationAwareLogger.WARN_INT;
          break;
        default:
          i = LocationAwareLogger.INFO_INT;
          break;
      }
      ((LocationAwareLogger) target).log(null, FQCN, i, format, args, t);//"Today Context"
    }
    else {
      final String msg = MessageFormatter.format(format, args);
      switch (level) {
        case DEBUG:
          target.debug(msg, t);
          break;
        case ERROR:
          target.error(msg, t);
          break;
        case TRACE:
          target.trace(msg, t);
          break;
        case WARN:
          target.warn(msg, t);
          break;
        default:
          target.info(msg, t);
          break;
      }
    }
  }

}

class Slf4jLoggerFactory extends LoggerFactory {

  @Override
  protected Logger createLogger(String name) {
    return new Slf4jLogger(name);
  }
}
