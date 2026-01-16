/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.logging;

import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import infra.core.env.Environment;
import infra.core.io.ClassPathResource;
import infra.util.ClassUtils;
import infra.util.StringUtils;
import infra.util.SystemPropertyUtils;

/**
 * Abstract base class for {@link LoggingSystem} implementations.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("NullAway")
public abstract class AbstractLoggingSystem extends LoggingSystem {

  private final ClassLoader classLoader;

  public AbstractLoggingSystem(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  protected Comparator<LoggerConfiguration> createConfigurationComparator() {
    return new LoggerConfigurationComparator(ROOT_LOGGER_NAME);
  }

  @Override
  public void beforeInitialize() {
  }

  @Override
  public void initialize(LoggingStartupContext startupContext, @Nullable String configLocation, @Nullable LogFile logFile) {
    if (StringUtils.isNotEmpty(configLocation)) {
      initializeWithSpecificConfig(startupContext, configLocation, logFile);
      return;
    }
    initializeWithConventions(startupContext, logFile);
  }

  private void initializeWithSpecificConfig(LoggingStartupContext context, String configLocation, @Nullable LogFile logFile) {
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
  protected @Nullable String getSelfInitializationConfig() {
    return findConfig(getStandardConfigLocations());
  }

  /**
   * Return any infra specific initialization config that should be applied. By default
   * this method checks {@link #getInfraConfigLocations()}.
   *
   * @return the infra initialization config or {@code null}
   */
  protected @Nullable String getInfraInitializationConfig() {
    return findConfig(getInfraConfigLocations());
  }

  private @Nullable String findConfig(String[] locations) {
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
      int extensionLength = (extension != null) ? (extension.length() + 1) : 0;
      locations[i] = locations[i].substring(0, locations[i].length() - extensionLength) + "-infra." + extension;
    }
    return locations;
  }

  /**
   * Load sensible defaults for the logging system.
   *
   * @param startupContext the logging startup context
   * @param logFile the file to load or {@code null} if no log file is to be written
   */
  protected abstract void loadDefaults(LoggingStartupContext startupContext, @Nullable LogFile logFile);

  /**
   * Load a specific configuration.
   *
   * @param context the logging initialization context
   * @param location the location of the configuration to load (never {@code null})
   * @param logFile the file to load or {@code null} if no log file is to be written
   */
  protected abstract void loadConfiguration(LoggingStartupContext context, String location, @Nullable LogFile logFile);

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
  protected Function<String, @Nullable String> getDefaultValueResolver(Environment environment) {
    return name -> {
      String defaultLogCorrelationPattern = getDefaultLogCorrelationPattern();
      if (StringUtils.isNotEmpty(defaultLogCorrelationPattern)
              && Objects.equals(name, LoggingSystemProperty.CORRELATION_PATTERN.applicationPropertyName)
              && environment.getFlag(EXPECT_CORRELATION_ID_PROPERTY, false)) {
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
  protected @Nullable String getDefaultLogCorrelationPattern() {
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
