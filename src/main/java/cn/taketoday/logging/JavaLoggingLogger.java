/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import cn.taketoday.core.io.ClassPathResource;

/**
 * @author TODAY <br>
 * 2019-11-03 14:45
 */
final class JavaLoggingLogger extends cn.taketoday.logging.Logger {

  private final Logger logger;

  public JavaLoggingLogger(String name) {
    this.logger = Logger.getLogger(name);
  }

  @Override
  public String getName() {
    return logger.getName();
  }

  @Override
  public boolean isTraceEnabled() {
    return logger.isLoggable(java.util.logging.Level.FINEST);
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isLoggable(java.util.logging.Level.FINER);
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isLoggable(java.util.logging.Level.INFO);
  }

  @Override
  public boolean isWarnEnabled() {
    return logger.isLoggable(java.util.logging.Level.WARNING);
  }

  @Override
  public boolean isErrorEnabled() {
    return logger.isLoggable(java.util.logging.Level.SEVERE);
  }

  private java.util.logging.Level levelToJavaLevel(Level level) {
    switch (level) {
      case TRACE:
        return java.util.logging.Level.FINEST;
      case DEBUG:
        return java.util.logging.Level.FINER;
      case WARN:
        return java.util.logging.Level.WARNING;
      case ERROR:
        return java.util.logging.Level.SEVERE;
      case INFO:
      default:
        return java.util.logging.Level.INFO;
    }
  }

  private static final String thisFQCN = JavaLoggingLogger.class.getName();

  @Override
  protected void logInternal(Level level, String format, Throwable t, Object[] args) {

    final java.util.logging.Level levelToJavaLevel = levelToJavaLevel(level);

    if (logger.isLoggable(levelToJavaLevel)) {

      // millis and thread are filled by the constructor
      LogRecord record = new LogRecord(levelToJavaLevel, MessageFormatter.format(format, args));

      record.setLoggerName(getName());
      record.setThrown(t);
      fillCallerData(record, thisFQCN, FQCN);
      logger.log(record);
    }
  }

  /**
   * From io.netty.util.internal.logging.JdkLogger#fillCallerData
   *
   * <p>
   * Fill in caller data if possible.
   *
   * @param record
   *         The record to update
   */
  private static void fillCallerData(LogRecord record, String callerFQCN, String superFQCN) {
    StackTraceElement[] steArray = new Throwable().getStackTrace();

    int selfIndex = -1;
    for (int i = 0; i < steArray.length; i++) {
      final String className = steArray[i].getClassName();
      if (className.equals(callerFQCN) || className.equals(superFQCN)) {
        selfIndex = i;
        break;
      }
    }

    int found = -1;
    for (int i = selfIndex + 1; i < steArray.length; i++) {
      final String className = steArray[i].getClassName();
      if (!(className.equals(callerFQCN) || className.equals(superFQCN))) {
        found = i;
        break;
      }
    }

    if (found != -1) {
      StackTraceElement ste = steArray[found];
      // setting the class name has the side effect of setting
      // the needToInferCaller variable to false.
      record.setSourceClassName(ste.getClassName());
      record.setSourceMethodName(ste.getMethodName());
    }
  }
}

final class JavaLoggingFactory extends LoggerFactory {
  static {
    ClassPathResource resource = new ClassPathResource(
            "logging.properties", Thread.currentThread().getContextClassLoader());
    if (resource.exists()) {
      try {
        LogManager.getLogManager().readConfiguration(resource.getInputStream());
      }
      catch (SecurityException | IOException e) {
        System.err.println("Can't load config file \"" + resource + "\"");
        e.printStackTrace();
      }
    }
  }

  @Override
  protected JavaLoggingLogger createLogger(String name) {
    return new JavaLoggingLogger(name);
  }
}
