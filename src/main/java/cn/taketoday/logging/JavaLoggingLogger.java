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
import java.net.URL;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * @author TODAY <br>
 * 2019-11-03 14:45
 */
final class JavaLoggingLogger extends cn.taketoday.logging.Logger {
  private static final String thisFQCN = JavaLoggingLogger.class.getName();

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

  @Override
  protected void logInternal(Level level, Object message, Throwable t) {
    final java.util.logging.Level levelToJavaLevel = levelToJavaLevel(level);
    if (this.logger.isLoggable(levelToJavaLevel)) {
      LogRecord rec;
      if (message instanceof LogRecord) {
        rec = (LogRecord) message;
      }
      else {
        rec = new LocationResolvingLogRecord(levelToJavaLevel, String.valueOf(message));
        rec.setLoggerName(getName());
        rec.setResourceBundleName(this.logger.getResourceBundleName());
        rec.setResourceBundle(this.logger.getResourceBundle());
        rec.setThrown(t);
      }
      logger.log(rec);
    }
  }

  @Override
  protected void logInternal(Level level, String format, Throwable t, Object[] args) {
    java.util.logging.Level levelToJavaLevel = levelToJavaLevel(level);
    if (this.logger.isLoggable(levelToJavaLevel)) {
      String message = MessageFormatter.format(format, args);
      LocationResolvingLogRecord rec = new LocationResolvingLogRecord(levelToJavaLevel, String.valueOf(message));
      rec.setLoggerName(getName());
      rec.setResourceBundleName(this.logger.getResourceBundleName());
      rec.setResourceBundle(this.logger.getResourceBundle());
      rec.setThrown(t);
      logger.log(rec);
    }
  }

  @SuppressWarnings("serial")
  private static class LocationResolvingLogRecord extends LogRecord {

    private volatile boolean resolved;

    public LocationResolvingLogRecord(java.util.logging.Level level, String msg) {
      super(level, msg);
    }

    @Override
    public String getSourceClassName() {
      if (!this.resolved) {
        resolve();
      }
      return super.getSourceClassName();
    }

    @Override
    public void setSourceClassName(String sourceClassName) {
      super.setSourceClassName(sourceClassName);
      this.resolved = true;
    }

    @Override
    public String getSourceMethodName() {
      if (!this.resolved) {
        resolve();
      }
      return super.getSourceMethodName();
    }

    @Override
    public void setSourceMethodName(String sourceMethodName) {
      super.setSourceMethodName(sourceMethodName);
      this.resolved = true;
    }

    private void resolve() {
      StackTraceElement[] stack = new Throwable().getStackTrace();
      String sourceClassName = null;
      String sourceMethodName = null;
      boolean found = false;
      for (StackTraceElement element : stack) {
        String className = element.getClassName();
        if (thisFQCN.equals(className)) {
          found = true;
        }
        else if (found) {
          sourceClassName = className;
          sourceMethodName = element.getMethodName();
          break;
        }
      }
      setSourceClassName(sourceClassName);
      setSourceMethodName(sourceMethodName);
    }

    protected Object writeReplace() {
      LogRecord serialized = new LogRecord(getLevel(), getMessage());
      serialized.setLoggerName(getLoggerName());
      serialized.setResourceBundle(getResourceBundle());
      serialized.setResourceBundleName(getResourceBundleName());
      serialized.setSourceClassName(getSourceClassName());
      serialized.setSourceMethodName(getSourceMethodName());
      serialized.setSequenceNumber(getSequenceNumber());
      serialized.setParameters(getParameters());
      serialized.setThrown(getThrown());
      return serialized;
    }
  }

}

final class JavaLoggingFactory extends LoggerFactory {
  static {
    URL resource = Thread.currentThread().getContextClassLoader().getResource("logging.properties");
    if (resource != null) {
      try {
        LogManager.getLogManager().readConfiguration(resource.openStream());
      }
      catch (SecurityException | IOException e) {
        System.err.println("Can't load config file 'logging.properties'");
        e.printStackTrace();
      }
    }
  }

  @Override
  protected JavaLoggingLogger createLogger(String name) {
    return new JavaLoggingLogger(name);
  }
}
