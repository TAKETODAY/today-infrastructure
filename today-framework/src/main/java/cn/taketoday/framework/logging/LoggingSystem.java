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

package cn.taketoday.framework.logging;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * Common abstraction over logging systems.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Ben Hale
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class LoggingSystem {

  /**
   * A System property that can be used to indicate the {@link LoggingSystem} to use.
   */
  public static final String SYSTEM_PROPERTY = LoggingSystem.class.getName();

  /**
   * The value of the {@link #SYSTEM_PROPERTY} that can be used to indicate that no
   * {@link LoggingSystem} should be used.
   */
  public static final String NONE = "none";

  /**
   * The name used for the root logger. LoggingSystem implementations should ensure that
   * this is the name used to represent the root logger, regardless of the underlying
   * implementation.
   */
  public static final String ROOT_LOGGER_NAME = "ROOT";

  private static final LoggingSystemFactory SYSTEM_FACTORY = LoggingSystemFactory.fromTodayStrategies();

  /**
   * Return the {@link LoggingSystemProperties} that should be applied.
   *
   * @param environment the {@link ConfigurableEnvironment} used to obtain value
   * @return the {@link LoggingSystemProperties} to apply
   */
  public LoggingSystemProperties getSystemProperties(ConfigurableEnvironment environment) {
    return new LoggingSystemProperties(environment);
  }

  /**
   * Reset the logging system to be limit output. This method may be called before
   * {@link #initialize(LoggingStartupContext, String, LogFile)} to reduce
   * logging noise until the system has been fully initialized.
   */
  public abstract void beforeInitialize();

  /**
   * Fully initialize the logging system.
   *
   * @param startupContext the logging initialization context
   * @param configLocation a log configuration location or {@code null} if default
   * initialization is required
   * @param logFile the log output file that should be written or {@code null} for
   * console only output
   */
  public void initialize(LoggingStartupContext startupContext,
          @Nullable String configLocation, @Nullable LogFile logFile) {

  }

  /**
   * Clean up the logging system. The default implementation does nothing. Subclasses
   * should override this method to perform any logging system-specific cleanup.
   */
  public void cleanUp() {

  }

  /**
   * Returns a {@link Runnable} that can handle shutdown of this logging system when the
   * JVM exits. The default implementation returns {@code null}, indicating that no
   * shutdown is required.
   *
   * @return the shutdown handler, or {@code null}
   */
  @Nullable
  public Runnable getShutdownHandler() {
    return null;
  }

  /**
   * Returns a set of the {@link LogLevel LogLevels} that are actually supported by the
   * logging system.
   *
   * @return the supported levels
   */
  public Set<LogLevel> getSupportedLogLevels() {
    return EnumSet.allOf(LogLevel.class);
  }

  /**
   * Sets the logging level for a given logger.
   *
   * @param loggerName the name of the logger to set ({@code null} can be used for the
   * root logger).
   * @param level the log level ({@code null} can be used to remove any custom level for
   * the logger and use the default configuration instead)
   */
  public void setLogLevel(@Nullable String loggerName, LogLevel level) {
    throw new UnsupportedOperationException("Unable to set log level");
  }

  /**
   * Returns a collection of the current configuration for all a {@link LoggingSystem}'s
   * loggers.
   *
   * @return the current configurations
   */
  public List<LoggerConfiguration> getLoggerConfigurations() {
    throw new UnsupportedOperationException("Unable to get logger configurations");
  }

  /**
   * Returns the current configuration for a {@link LoggingSystem}'s logger.
   *
   * @param loggerName the name of the logger
   * @return the current configuration
   */
  public LoggerConfiguration getLoggerConfiguration(String loggerName) {
    throw new UnsupportedOperationException("Unable to get logger configuration");
  }

  /**
   * Detect and return the logging system in use. Supports Logback and Java Logging.
   *
   * @param classLoader the classloader
   * @return the logging system
   */
  public static LoggingSystem get(ClassLoader classLoader) {
    String loggingSystemClassName = System.getProperty(SYSTEM_PROPERTY);
    if (StringUtils.isNotEmpty(loggingSystemClassName)) {
      if (NONE.equals(loggingSystemClassName)) {
        return new NoOpLoggingSystem();
      }
      return get(classLoader, loggingSystemClassName);
    }
    LoggingSystem loggingSystem = SYSTEM_FACTORY.getLoggingSystem(classLoader);
    Assert.state(loggingSystem != null, "No suitable logging system located");
    return loggingSystem;
  }

  private static LoggingSystem get(ClassLoader classLoader, String loggingSystemClassName) {
    try {
      Class<?> systemClass = ClassUtils.forName(loggingSystemClassName, classLoader);
      Constructor<?> constructor = systemClass.getDeclaredConstructor(ClassLoader.class);
      constructor.setAccessible(true);
      return (LoggingSystem) constructor.newInstance(classLoader);
    }
    catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * {@link LoggingSystem} that does nothing.
   */
  static class NoOpLoggingSystem extends LoggingSystem {

    @Override
    public void beforeInitialize() {

    }

    @Override
    public void setLogLevel(String loggerName, LogLevel level) {

    }

    @Override
    public List<LoggerConfiguration> getLoggerConfigurations() {
      return Collections.emptyList();
    }

    @Override
    public LoggerConfiguration getLoggerConfiguration(String loggerName) {
      return null;
    }

  }

}
