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

import java.util.Objects;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

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
  @Nullable
  public LevelConfiguration getLevelConfiguration() {
    return getLevelConfiguration(ConfigurationScope.INHERITED);
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
    return "LoggerConfiguration [name=" + this.name + ", levelConfiguration=" + this.levelConfiguration
            + ", inheritedLevelConfiguration=" + this.inheritedLevelConfiguration + "]";
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
