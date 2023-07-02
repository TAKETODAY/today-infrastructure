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

import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.util.SystemPropertyUtils;

/**
 * Abstract base class for {@link LoggingSystem} implementations.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractLoggingSystem extends LoggingSystem {

  protected static final Comparator<LoggerConfiguration> CONFIGURATION_COMPARATOR
          = new LoggerConfigurationComparator(ROOT_LOGGER_NAME);

  private final ClassLoader classLoader;

  public AbstractLoggingSystem(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public void beforeInitialize() {

  }

  @Override
  public void initialize(LoggingStartupContext startupContext,
          @Nullable String configLocation, @Nullable LogFile logFile) {
    if (StringUtils.isNotEmpty(configLocation)) {
      initializeWithSpecificConfig(startupContext, configLocation, logFile);
      return;
    }
    initializeWithConventions(startupContext, logFile);
  }

  private void initializeWithSpecificConfig(
          LoggingStartupContext context, String configLocation, LogFile logFile) {
    configLocation = SystemPropertyUtils.resolvePlaceholders(configLocation);
    loadConfiguration(context, configLocation, logFile);
  }

  private void initializeWithConventions(LoggingStartupContext startupContext, @Nullable LogFile logFile) {
    String config = getSelfInitializationConfig();
    if (config != null && logFile == null) {
      // self initialization has occurred, reinitialize in case of property changes
      reinitialize(startupContext);
      return;
    }
    if (config == null) {
      config = getInfraInitializationConfig();
    }
    if (config != null) {
      loadConfiguration(startupContext, config, logFile);
      return;
    }
    loadDefaults(startupContext, logFile);
  }

  /**
   * Return any self initialization config that has been applied. By default this method
   * checks {@link #getStandardConfigLocations()} and assumes that any file that exists
   * will have been applied.
   *
   * @return the self initialization config or {@code null}
   */
  @Nullable
  protected String getSelfInitializationConfig() {
    return findConfig(getStandardConfigLocations());
  }

  /**
   * Return any infra specific initialization config that should be applied. By default
   * this method checks {@link #getInfraConfigLocations()}.
   *
   * @return the infra initialization config or {@code null}
   */
  @Nullable
  protected String getInfraInitializationConfig() {
    return findConfig(getInfraConfigLocations());
  }

  @Nullable
  private String findConfig(String[] locations) {
    for (String location : locations) {
      ClassPathResource resource = new ClassPathResource(location, this.classLoader);
      if (resource.exists()) {
        return "classpath:" + location;
      }
    }
    return null;
  }

  /**
   * Return the standard config locations for this system.
   *
   * @return the standard config locations
   * @see #getSelfInitializationConfig()
   */
  protected abstract String[] getStandardConfigLocations();

  /**
   * Return the infra config locations for this system. By default this method returns
   * a set of locations based on {@link #getStandardConfigLocations()}.
   *
   * @return the infra config locations
   * @see #getInfraInitializationConfig()
   */
  protected String[] getInfraConfigLocations() {
    String[] locations = getStandardConfigLocations();
    for (int i = 0; i < locations.length; i++) {
      String extension = StringUtils.getFilenameExtension(locations[i]);
      locations[i] = locations[i].substring(0, locations[i].length() - extension.length() - 1)
              + "-infra." + extension;
    }
    return locations;
  }

  /**
   * Load sensible defaults for the logging system.
   *
   * @param startupContext the logging startup context
   * @param logFile the file to load or {@code null} if no log file is to be written
   */
  protected abstract void loadDefaults(
          LoggingStartupContext startupContext, @Nullable LogFile logFile);

  /**
   * Load a specific configuration.
   *
   * @param context the logging initialization context
   * @param location the location of the configuration to load (never {@code null})
   * @param logFile the file to load or {@code null} if no log file is to be written
   */
  protected abstract void loadConfiguration(
          LoggingStartupContext context, String location, @Nullable LogFile logFile);

  /**
   * Reinitialize the logging system if required. Called when
   * {@link #getSelfInitializationConfig()} is used and the log file hasn't changed. May
   * be used to reload configuration (for example to pick up additional System
   * properties).
   *
   * @param startupContext the logging startup context
   */
  protected void reinitialize(LoggingStartupContext startupContext) {

  }

  protected final ClassLoader getClassLoader() {
    return this.classLoader;
  }

  protected final String getPackagedConfigFile(String fileName) {
    String defaultPath = ClassUtils.getPackageName(getClass());
    defaultPath = defaultPath.replace('.', '/');
    defaultPath = defaultPath + "/" + fileName;
    defaultPath = "classpath:" + defaultPath;
    return defaultPath;
  }

  protected final void applySystemProperties(Environment environment, @Nullable LogFile logFile) {
    new LoggingSystemProperties(environment, getDefaultValueResolver(environment), null).apply(logFile);
  }

  /**
   * Return the default value resolver to use when resolving system properties.
   *
   * @param environment the environment
   * @return the default value resolver
   */
  protected Function<String, String> getDefaultValueResolver(Environment environment) {
    String defaultLogCorrelationPattern = getDefaultLogCorrelationPattern();
    return (name) -> {
      if (StringUtils.isNotEmpty(defaultLogCorrelationPattern)
              && LoggingSystemProperty.CORRELATION_PATTERN.getApplicationPropertyName().equals(name)
              && environment.getProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, Boolean.class, false)) {
        return defaultLogCorrelationPattern;
      }
      return null;
    };
  }

  /**
   * Return the default log correlation pattern or {@code null} if log correlation
   * patterns are not supported.
   *
   * @return the default log correlation pattern
   */
  @Nullable
  protected String getDefaultLogCorrelationPattern() {
    return null;
  }

  /**
   * Maintains a mapping between native levels and {@link LogLevel}.
   *
   * @param <T> the native level type
   */
  protected static class LogLevels<T> {

    private final Map<LogLevel, T> systemToNative;

    private final Map<T, LogLevel> nativeToSystem;

    public LogLevels() {
      this.systemToNative = new EnumMap<>(LogLevel.class);
      this.nativeToSystem = new HashMap<>();
    }

    public void map(LogLevel system, T nativeLevel) {
      this.systemToNative.putIfAbsent(system, nativeLevel);
      this.nativeToSystem.putIfAbsent(nativeLevel, system);
    }

    public LogLevel convertNativeToSystem(T level) {
      return this.nativeToSystem.get(level);
    }

    public T convertSystemToNative(LogLevel level) {
      return this.systemToNative.get(level);
    }

    public Set<LogLevel> getSupported() {
      return new LinkedHashSet<>(this.nativeToSystem.values());
    }

  }

}
