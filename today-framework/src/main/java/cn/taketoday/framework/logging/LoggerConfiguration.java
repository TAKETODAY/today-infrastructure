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

/**
 * Immutable class that represents the configuration of a {@link LoggingSystem}'s logger.
 *
 * @author Ben Hale
 * @since 4.0
 */
public final class LoggerConfiguration {

  private final String name;

  private final LogLevel configuredLevel;

  private final LogLevel effectiveLevel;

  /**
   * Create a new {@link LoggerConfiguration instance}.
   *
   * @param name the name of the logger
   * @param configuredLevel the configured level of the logger
   * @param effectiveLevel the effective level of the logger
   */
  public LoggerConfiguration(String name, LogLevel configuredLevel, LogLevel effectiveLevel) {
    Assert.notNull(name, "Name must not be null");
    Assert.notNull(effectiveLevel, "EffectiveLevel must not be null");
    this.name = name;
    this.configuredLevel = configuredLevel;
    this.effectiveLevel = effectiveLevel;
  }

  /**
   * Returns the configured level of the logger.
   *
   * @return the configured level of the logger
   */
  public LogLevel getConfiguredLevel() {
    return this.configuredLevel;
  }

  /**
   * Returns the effective level of the logger.
   *
   * @return the effective level of the logger
   */
  public LogLevel getEffectiveLevel() {
    return this.effectiveLevel;
  }

  /**
   * Returns the name of the logger.
   *
   * @return the name of the logger
   */
  public String getName() {
    return this.name;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (obj instanceof LoggerConfiguration other) {
      return Objects.equals(this.name, other.name)
              && Objects.equals(this.configuredLevel, other.configuredLevel)
              && Objects.equals(this.effectiveLevel, other.effectiveLevel);
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, configuredLevel, effectiveLevel);
  }

  @Override
  public String toString() {
    return "LoggerConfiguration [name=" + this.name + ", configuredLevel=" + this.configuredLevel
            + ", effectiveLevel=" + this.effectiveLevel + "]";
  }

}
