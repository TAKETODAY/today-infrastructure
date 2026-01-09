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
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 2019-11-03 14:45
 */
final class JavaLoggingLogger extends infra.logging.Logger {

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
  @SuppressWarnings("NullAway")
  protected void logInternal(Level level, String format, @Nullable Throwable t, @Nullable Object @Nullable [] args) {
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

    public LocationResolvingLogRecord(java.util.logging.Level level, @Nullable String msg) {
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
