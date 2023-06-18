/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.net.URL;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * @author TODAY <br>
 * 2019-11-03 14:45
 */
final class JavaLoggingLogger extends cn.taketoday.logging.Logger {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Logger logger;

  public JavaLoggingLogger(Logger logger, boolean debugEnabled) {
    super(debugEnabled);
    this.logger = logger;
  }

  @Override
  public String getName() {
    return logger.getName();
  }

  @Override
  public boolean isTraceEnabled() {
    return debugEnabled && logger.isLoggable(java.util.logging.Level.FINEST);
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
    return switch (level) {
      case TRACE -> java.util.logging.Level.FINEST;
      case DEBUG -> java.util.logging.Level.FINER;
      case WARN -> java.util.logging.Level.WARNING;
      case ERROR -> java.util.logging.Level.SEVERE;
      case INFO -> java.util.logging.Level.INFO;
    };
  }

  @Override
  protected void logInternal(Level level, String format, Throwable t, Object[] args) {
    java.util.logging.Level levelToJavaLevel = levelToJavaLevel(level);
    if (logger.isLoggable(levelToJavaLevel)) {
      String message = MessageFormatter.format(format, args);
      LocationResolvingLogRecord rec = new LocationResolvingLogRecord(levelToJavaLevel, message);
      rec.setLoggerName(getName());
      rec.setResourceBundleName(logger.getResourceBundleName());
      rec.setResourceBundle(logger.getResourceBundle());
      rec.setThrown(t);
      logger.log(rec);
    }
  }

  private static class LocationResolvingLogRecord extends LogRecord {

    @Serial
    private static final long serialVersionUID = 1L;

    private volatile boolean resolved;

    public LocationResolvingLogRecord(java.util.logging.Level level, String msg) {
      super(level, msg);
    }

    @Override
    public String getSourceClassName() {
      if (!resolved) {
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
      if (!resolved) {
        resolve();
      }
      return super.getSourceMethodName();
    }

    @Override
    public void setSourceMethodName(String sourceMethodName) {
      super.setSourceMethodName(sourceMethodName);
      this.resolved = true;
    }

    private Optional<StackWalker.StackFrame> eatStackFrame(Stream<StackWalker.StackFrame> stream) {
      Predicate<StackWalker.StackFrame> loggerPredicate = new Predicate<>() {
        boolean found = false;

        @Override
        public boolean test(StackWalker.StackFrame stackFrame) {
          String className = stackFrame.getClassName();
          if (FQCN.equals(className)) {
            found = true;
            return false;
          }
          return found;
        }
      };
      return stream.filter(loggerPredicate)
              .findFirst();
    }

    private void resolve() {
      StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
              .walk(this::eatStackFrame)
              .ifPresent(stackFrame -> {
                setSourceClassName(stackFrame.getClassName());
                setSourceMethodName(stackFrame.getMethodName());
              });
    }

    @Serial
    protected Object writeReplace() {
      LogRecord serialized = new LogRecord(getLevel(), getMessage());
      serialized.setLoggerName(getLoggerName());
      serialized.setResourceBundle(getResourceBundle());
      serialized.setResourceBundleName(getResourceBundleName());
      serialized.setSourceClassName(getSourceClassName());
      serialized.setSourceMethodName(getSourceMethodName());
      serialized.setSequenceNumber(getSequenceNumber());
      serialized.setParameters(getParameters());
      serialized.setLongThreadID(getLongThreadID());
      serialized.setInstant(getInstant());
      serialized.setThrown(getThrown());
      return serialized;
    }
  }

}

final class JavaLoggingFactory extends LoggerFactory {
  static {
    URL resource = Thread.currentThread().getContextClassLoader().getResource("logging.properties");
    if (resource != null) {
      try (InputStream inputStream = resource.openStream()) {
        LogManager.getLogManager().readConfiguration(inputStream);
      }
      catch (SecurityException | IOException e) {
        System.err.println("Can't load config file 'logging.properties'");
        e.printStackTrace();
      }
    }
  }

  @Override
  protected JavaLoggingLogger createLogger(String name) {
    Logger logger = Logger.getLogger(name);
    return new JavaLoggingLogger(logger, logger.isLoggable(java.util.logging.Level.FINER));
  }
}
