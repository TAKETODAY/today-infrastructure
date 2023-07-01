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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cn.taketoday.framework.logging.LoggerConfiguration.ConfigurationScope;
import cn.taketoday.framework.logging.LoggerConfiguration.LevelConfiguration;

import static org.assertj.core.api.Assertions.*;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/2 01:03
 */
class LoggerConfigurationTests {

  @Test
  void createWithLogLevelWhenNameIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new LoggerConfiguration(null, null, LogLevel.DEBUG))
            .withMessage("Name must not be null");
  }

  @Test
  void createWithLogLevelWhenEffectiveLevelIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new LoggerConfiguration("test", null, (LogLevel) null))
            .withMessage("EffectiveLevel must not be null");
  }

  @Test
  void createWithLevelConfigurationWhenNameIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new LoggerConfiguration(null, null, LevelConfiguration.of(LogLevel.DEBUG)))
            .withMessage("Name must not be null");
  }

  @Test
  void createWithLevelConfigurationWhenEffectiveLevelIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new LoggerConfiguration("test", null, (LevelConfiguration) null))
            .withMessage("EffectiveLevelConfiguration must not be null");
  }

  @Test
  void getNameReturnsName() {
    LoggerConfiguration configuration = new LoggerConfiguration("test", null,
            LevelConfiguration.of(LogLevel.DEBUG));
    assertThat(configuration.getName()).isEqualTo("test");
  }

  @Test
  void getConfiguredLevelWhenConfiguredReturnsLevel() {
    LoggerConfiguration configuration = new LoggerConfiguration("test", LevelConfiguration.of(LogLevel.DEBUG),
            LevelConfiguration.of(LogLevel.DEBUG));
    assertThat(configuration.getConfiguredLevel()).isEqualTo(LogLevel.DEBUG);
  }

  @Test
  void getConfiguredLevelWhenNotConfiguredReturnsNull() {
    LoggerConfiguration configuration = new LoggerConfiguration("test", null,
            LevelConfiguration.of(LogLevel.DEBUG));
    assertThat(configuration.getConfiguredLevel()).isNull();
  }

  @Test
  void getEffectiveLevelReturnsEffectiveLevel() {
    LoggerConfiguration configuration = new LoggerConfiguration("test", null,
            LevelConfiguration.of(LogLevel.DEBUG));
    assertThat(configuration.getEffectiveLevel()).isEqualTo(LogLevel.DEBUG);
  }

  @Test
  void getLevelConfigurationWithDirectScopeWhenConfiguredReturnsConfiguration() {
    LevelConfiguration assigned = LevelConfiguration.of(LogLevel.DEBUG);
    LoggerConfiguration configuration = new LoggerConfiguration("test", assigned,
            LevelConfiguration.of(LogLevel.DEBUG));
    assertThat(configuration.getLevelConfiguration(ConfigurationScope.DIRECT)).isEqualTo(assigned);
  }

  @Test
  void getLevelConfigurationWithDirectScopeWhenNotConfiguredReturnsNull() {
    LoggerConfiguration configuration = new LoggerConfiguration("test", null,
            LevelConfiguration.of(LogLevel.DEBUG));
    assertThat(configuration.getLevelConfiguration(ConfigurationScope.DIRECT)).isNull();
  }

  @Test
  void getLevelConfigurationWithInheritedScopeReturnsConfiguration() {
    LevelConfiguration effective = LevelConfiguration.of(LogLevel.DEBUG);
    LoggerConfiguration configuration = new LoggerConfiguration("test", null, effective);
    assertThat(configuration.getLevelConfiguration(ConfigurationScope.INHERITED)).isEqualTo(effective);
  }

  /**
   * Tests for {@link LevelConfiguration}.
   */
  @Nested
  class LevelConfigurationTests {

    @Test
    void ofWhenLogLevelIsNullThrowsException() {
      assertThatIllegalArgumentException().isThrownBy(() -> LevelConfiguration.of(null))
              .withMessage("LogLevel must not be null");
    }

    @Test
    void ofCreatesConfiguration() {
      LevelConfiguration configuration = LevelConfiguration.of(LogLevel.DEBUG);
      assertThat(configuration.getLevel()).isEqualTo(LogLevel.DEBUG);
    }

    @Test
    void ofCustomWhenNameIsNullThrowsException() {
      assertThatIllegalArgumentException().isThrownBy(() -> LevelConfiguration.ofCustom(null))
              .withMessage("Name must not be empty");
    }

    @Test
    void ofCustomWhenNameIsEmptyThrowsException() {
      assertThatIllegalArgumentException().isThrownBy(() -> LevelConfiguration.ofCustom(""))
              .withMessage("Name must not be empty");
    }

    @Test
    void ofCustomCreatesConfiguration() {
      LevelConfiguration configuration = LevelConfiguration.ofCustom("FINE");
      assertThat(configuration).isNotNull();
    }

    @Test
    void getNameWhenFromLogLevelReturnsName() {
      LevelConfiguration configuration = LevelConfiguration.of(LogLevel.DEBUG);
      assertThat(configuration.getName()).isEqualTo("DEBUG");
    }

    @Test
    void getNameWhenCustomReturnsName() {
      LevelConfiguration configuration = LevelConfiguration.ofCustom("FINE");
      assertThat(configuration.getName()).isEqualTo("FINE");
    }

    @Test
    void getLevelWhenCustomThrowsException() {
      LevelConfiguration configuration = LevelConfiguration.ofCustom("FINE");
      assertThatIllegalStateException().isThrownBy(() -> configuration.getLevel())
              .withMessage("Unable to provide LogLevel for 'FINE'");
    }

    @Test
    void getLevelReturnsLevel() {
      LevelConfiguration configuration = LevelConfiguration.of(LogLevel.DEBUG);
      assertThat(configuration.getLevel()).isEqualTo(LogLevel.DEBUG);
    }

    @Test
    void isCustomWhenNotCustomReturnsFalse() {
      LevelConfiguration configuration = LevelConfiguration.of(LogLevel.DEBUG);
      assertThat(configuration.isCustom()).isFalse();
    }

    @Test
    void isCustomWhenCustomReturnsTrue() {
      LevelConfiguration configuration = LevelConfiguration.ofCustom("DEBUG");
      assertThat(configuration.isCustom()).isTrue();
    }

  }

}