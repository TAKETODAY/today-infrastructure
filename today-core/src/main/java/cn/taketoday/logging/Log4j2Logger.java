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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;

import java.io.Serial;

/**
 * @author TODAY <br>
 * 2019-11-03 16:09
 */
final class Log4j2Logger extends Logger {
  static final LoggerContext loggerContext =
          LogManager.getContext(Log4j2Logger.class.getClassLoader(), false);

  private final ExtendedLogger logger;

  Log4j2Logger(ExtendedLogger logger, boolean debugEnabled) {
    super(debugEnabled);
    this.logger = logger;
  }

  @Override
  public String getName() {
    return logger.getName();
  }

  @Override
  public boolean isTraceEnabled() {
    return debugEnabled && logger.isTraceEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  @Override
  public boolean isWarnEnabled() {
    return logger.isWarnEnabled();
  }

  @Override
  public boolean isErrorEnabled() {
    return logger.isErrorEnabled();
  }

  private org.apache.logging.log4j.Level getLevel(Level level) {
    return switch (level) {
      case INFO -> org.apache.logging.log4j.Level.INFO;
      case WARN -> org.apache.logging.log4j.Level.WARN;
      case DEBUG -> org.apache.logging.log4j.Level.DEBUG;
      case TRACE -> org.apache.logging.log4j.Level.TRACE;
      case ERROR -> org.apache.logging.log4j.Level.ERROR;
    };
  }

  @Override
  protected void logInternal(Level level, Object message, Throwable t) {
    if (message instanceof String) {
      // Explicitly pass a String argument, avoiding Log4j's argument expansion
      // for message objects in case of "{}" sequences (SPR-16226)
      if (t != null) {
        this.logger.logIfEnabled(FQCN, getLevel(level), null, (String) message, t);
      }
      else {
        this.logger.logIfEnabled(FQCN, getLevel(level), null, (String) message);
      }
    }
    else {
      this.logger.logIfEnabled(FQCN, getLevel(level), null, message, t);
    }
  }

  @Override
  protected void logInternal(Level level, String format, Throwable t, Object[] args) {
    final Message message = new Message() {
      @Serial
      private static final long serialVersionUID = 1L;
      private String msg;

      @Override
      public Throwable getThrowable() {
        return t;
      }

      @Override
      public Object[] getParameters() {
        return args;
      }

      @Override
      public String getFormattedMessage() {
        if (msg == null) {
          msg = MessageFormatter.format(format, args);
        }
        return msg;
      }

      @Override
      public String getFormat() {
        return msg;
      }
    };

    this.logger.logIfEnabled(FQCN, getLevel(level), null, message, t);
  }

}

final class Log4j2LoggerFactory extends LoggerFactory {

  Log4j2LoggerFactory() {
    LogManager.class.getName();
  }

  @Override
  protected Logger createLogger(String name) {
    LoggerContext context = Log4j2Logger.loggerContext;
    if (context == null) {
      // Circular call in early-init scenario -> static field not initialized yet
      context = LogManager.getContext(Log4j2Logger.class.getClassLoader(), false);
    }
    ExtendedLogger logger = context.getLogger(name);

    return new Log4j2Logger(logger, logger.isDebugEnabled());
  }
}
