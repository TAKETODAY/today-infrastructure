/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.framework.logging.java;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.core.io.Resource;
import cn.taketoday.framework.io.ApplicationResourceLoader;
import cn.taketoday.framework.logging.AbstractLoggingSystem;
import cn.taketoday.framework.logging.LogFile;
import cn.taketoday.framework.logging.LogLevel;
import cn.taketoday.framework.logging.LoggerConfiguration;
import cn.taketoday.framework.logging.LoggingStartupContext;
import cn.taketoday.framework.logging.LoggingSystem;
import cn.taketoday.framework.logging.LoggingSystemFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StreamUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link LoggingSystem} for {@link Logger java.util.logging}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Ben Hale
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JavaLoggingSystem extends AbstractLoggingSystem {

  private static final LogLevels<Level> LEVELS = new LogLevels<>();

  static {
    LEVELS.map(LogLevel.TRACE, Level.FINEST);
    LEVELS.map(LogLevel.DEBUG, Level.FINE);
    LEVELS.map(LogLevel.INFO, Level.INFO);
    LEVELS.map(LogLevel.WARN, Level.WARNING);
    LEVELS.map(LogLevel.ERROR, Level.SEVERE);
    LEVELS.map(LogLevel.FATAL, Level.SEVERE);
    LEVELS.map(LogLevel.OFF, Level.OFF);
  }

  private final Set<Logger> configuredLoggers = Collections.synchronizedSet(new HashSet<>());

  public JavaLoggingSystem(ClassLoader classLoader) {
    super(classLoader);
  }

  @Override
  protected String[] getStandardConfigLocations() {
    return new String[] { "logging.properties" };
  }

  @Override
  public void beforeInitialize() {
    super.beforeInitialize();
    Logger.getLogger("").setLevel(Level.SEVERE);
  }

  @Override
  protected void loadDefaults(LoggingStartupContext startupContext, @Nullable LogFile logFile) {
    if (logFile != null) {
      loadConfiguration(getPackagedConfigFile("logging-file.properties"), logFile);
    }
    else {
      loadConfiguration(getPackagedConfigFile("logging.properties"), null);
    }
  }

  @Override
  protected void loadConfiguration(LoggingStartupContext context, String location, @Nullable LogFile logFile) {
    loadConfiguration(location, logFile);
  }

  protected void loadConfiguration(String location, @Nullable LogFile logFile) {
    Assert.notNull(location, "Location is required");
    try {
      Resource resource = new ApplicationResourceLoader().getResource(location);

      String config = StreamUtils.copyToString(resource.getInputStream());
      if (logFile != null) {
        config = config.replace("${LOG_FILE}", StringUtils.cleanPath(logFile.toString()));
      }
      LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(config.getBytes()));
    }
    catch (Exception ex) {
      throw new IllegalStateException("Could not initialize Java logging from " + location, ex);
    }
  }

  @Override
  public Set<LogLevel> getSupportedLogLevels() {
    return LEVELS.getSupported();
  }

  @Override
  public void setLogLevel(@Nullable String loggerName, LogLevel level) {
    if (loggerName == null || ROOT_LOGGER_NAME.equals(loggerName)) {
      loggerName = "";
    }
    Logger logger = Logger.getLogger(loggerName);
    if (logger != null) {
      this.configuredLoggers.add(logger);
      logger.setLevel(LEVELS.convertSystemToNative(level));
    }
  }

  @Override
  public List<LoggerConfiguration> getLoggerConfigurations() {
    List<LoggerConfiguration> result = new ArrayList<>();
    Enumeration<String> names = LogManager.getLogManager().getLoggerNames();
    while (names.hasMoreElements()) {
      result.add(getLoggerConfiguration(names.nextElement()));
    }
    result.sort(CONFIGURATION_COMPARATOR);
    return Collections.unmodifiableList(result);
  }

  @Override
  @Nullable
  public LoggerConfiguration getLoggerConfiguration(String loggerName) {
    Logger logger = Logger.getLogger(loggerName);
    if (logger == null) {
      return null;
    }
    LogLevel level = LEVELS.convertNativeToSystem(logger.getLevel());
    LogLevel effectiveLevel = LEVELS.convertNativeToSystem(getEffectiveLevel(logger));
    String name = StringUtils.isNotEmpty(logger.getName()) ? logger.getName() : ROOT_LOGGER_NAME;
    return new LoggerConfiguration(name, level, effectiveLevel);
  }

  private Level getEffectiveLevel(Logger root) {
    Logger logger = root;
    while (logger.getLevel() == null) {
      logger = logger.getParent();
    }
    return logger.getLevel();
  }

  @Override
  public Runnable getShutdownHandler() {
    return () -> LogManager.getLogManager().reset();
  }

  @Override
  public void cleanUp() {
    this.configuredLoggers.clear();
  }

  /**
   * {@link LoggingSystemFactory} that returns {@link JavaLoggingSystem} if possible.
   */
  @Order(Ordered.LOWEST_PRECEDENCE)
  public static class Factory implements LoggingSystemFactory {

    private static final boolean PRESENT = ClassUtils.isPresent(
            "java.util.logging.LogManager", Factory.class.getClassLoader());

    @Override
    public LoggingSystem getLoggingSystem(ClassLoader classLoader) {
      if (PRESENT) {
        return new JavaLoggingSystem(classLoader);
      }
      return null;
    }

  }

}
