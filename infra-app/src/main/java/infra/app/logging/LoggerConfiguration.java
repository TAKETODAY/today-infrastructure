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

import java.util.Objects;

import infra.lang.Assert;
import infra.util.ObjectUtils;

/**
 * Immutable class that represents the configuration of a {@link LoggingSystem}'s logger.
 *
 * @author Ben Hale
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class LoggerConfiguration {

  private final String name;

  @Nullable
  private final LevelConfiguration levelConfiguration;

  @Nullable
  private final LevelConfiguration inheritedLevelConfiguration;

  /**
   * Create a new {@link LoggerConfiguration instance}.
   *
   * @param name the name of the logger
   * @param configuredLevel the configured level of the logger
   * @param effectiveLevel the effective level of the logger
   */
  public LoggerConfiguration(String name, @Nullable LogLevel configuredLevel, LogLevel effectiveLevel) {
    Assert.notNull(name, "Name is required");
    Assert.notNull(effectiveLevel, "EffectiveLevel is required");
    this.name = name;
    this.levelConfiguration = (configuredLevel != null) ? LevelConfiguration.of(configuredLevel) : null;
    this.inheritedLevelConfiguration = LevelConfiguration.of(effectiveLevel);
  }

  /**
   * Create a new {@link LoggerConfiguration instance}.
   *
   * @param name the name of the logger
   * @param levelConfiguration the level configuration
   * @param inheritedLevelConfiguration the inherited level configuration
   */
  public LoggerConfiguration(String name,
          @Nullable LevelConfiguration levelConfiguration,
          LevelConfiguration inheritedLevelConfiguration) {
    Assert.notNull(name, "Name is required");
    Assert.notNull(inheritedLevelConfiguration, "EffectiveLevelConfiguration is required");
    this.name = name;
    this.levelConfiguration = levelConfiguration;
    this.inheritedLevelConfiguration = inheritedLevelConfiguration;
  }

  /**
   * Returns the name of the logger.
   *
   * @return the name of the logger
   */
  public String getName() {
    return this.name;
  }

  /**
   * Returns the configured level of the logger.
   *
   * @return the configured level of the logger
   * @see #getLevelConfiguration(ConfigurationScope)
   */
  @Nullable
  public LogLevel getConfiguredLevel() {
    LevelConfiguration configuration = getLevelConfiguration(ConfigurationScope.DIRECT);
    return (configuration != null) ? configuration.getLevel() : null;
  }

  /**
   * Returns the effective level of the logger.
   *
   * @return the effective level of the logger
   * @see #getLevelConfiguration(ConfigurationScope)
   */
  public LogLevel getEffectiveLevel() {
    return getLevelConfiguration().getLevel();
  }

  /**
   * Return the level configuration, considering inherited loggers.
   *
   * @return the level configuration
   */
  public LevelConfiguration getLevelConfiguration() {
    LevelConfiguration result = getLevelConfiguration(ConfigurationScope.INHERITED);
    Assert.state(result != null, "Inherited level configuration is required");
    return result;
  }

  /**
   * Return the level configuration for the given scope.
   *
   * @param scope the configuration scope
   * @return the level configuration or {@code null} for
   * {@link ConfigurationScope#DIRECT direct scope} results without applied
   * configuration
   */
  @Nullable
  public LevelConfiguration getLevelConfiguration(ConfigurationScope scope) {
    return (scope != ConfigurationScope.DIRECT) ? this.inheritedLevelConfiguration : this.levelConfiguration;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    LoggerConfiguration other = (LoggerConfiguration) obj;
    return ObjectUtils.nullSafeEquals(this.name, other.name)
            && ObjectUtils.nullSafeEquals(this.levelConfiguration, other.levelConfiguration)
            && ObjectUtils.nullSafeEquals(this.inheritedLevelConfiguration, other.inheritedLevelConfiguration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.name, this.levelConfiguration, this.inheritedLevelConfiguration);
  }

  @Override
  public String toString() {
    return "LoggerConfiguration [name=%s, levelConfiguration=%s, inheritedLevelConfiguration=%s]"
            .formatted(this.name, this.levelConfiguration, this.inheritedLevelConfiguration);
  }

  /**
   * Supported logger configurations scopes.
   */
  public enum ConfigurationScope {

    /**
     * Only return configuration that has been applied directly applied. Often
     * referred to as 'configured' or 'assigned' configuration.
     */
    DIRECT,

    /**
     * May return configuration that has been applied to a parent logger. Often
     * referred to as 'effective' configuration.
     */
    INHERITED

  }

  /**
   * Logger level configuration.
   */
  public static final class LevelConfiguration {

    private final String name;

    @Nullable
    private final LogLevel logLevel;

    private LevelConfiguration(String name, @Nullable LogLevel logLevel) {
      this.name = name;
      this.logLevel = logLevel;
    }

    /**
     * Return the name of the level.
     *
     * @return the level name
     */
    public String getName() {
      return this.name;
    }

    /**
     * Return the actual level value if possible.
     *
     * @return the level value
     * @throws IllegalStateException if this is a {@link #isCustom() custom} level
     */
    public LogLevel getLevel() {
      Assert.state(this.logLevel != null, "Unable to provide LogLevel for '" + this.name + "'");
      return this.logLevel;
    }

    /**
     * Return if this is a custom level and cannot be represented by {@link LogLevel}.
     *
     * @return if this is a custom level
     */
    public boolean isCustom() {
      return this.logLevel == null;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      LevelConfiguration other = (LevelConfiguration) obj;
      return this.logLevel == other.logLevel && ObjectUtils.nullSafeEquals(this.name, other.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.logLevel, this.name);
    }

    @Override
    public String toString() {
      return "LevelConfiguration [name=" + this.name + ", logLevel=" + this.logLevel + "]";
    }

    /**
     * Create a new {@link LevelConfiguration} instance of the given {@link LogLevel}.
     *
     * @param logLevel the log level
     * @return a new {@link LevelConfiguration} instance
     */
    public static LevelConfiguration of(LogLevel logLevel) {
      Assert.notNull(logLevel, "LogLevel is required");
      return new LevelConfiguration(logLevel.name(), logLevel);
    }

    /**
     * Create a new {@link LevelConfiguration} instance for a custom level name.
     *
     * @param name the log level name
     * @return a new {@link LevelConfiguration} instance
     */
    public static LevelConfiguration ofCustom(String name) {
      Assert.hasText(name, "Name must not be empty");
      return new LevelConfiguration(name, null);
    }

  }

}
