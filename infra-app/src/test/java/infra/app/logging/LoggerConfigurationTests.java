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

package infra.app.logging;

import org.junit.jupiter.api.Test;

import infra.app.logging.LoggerConfiguration.ConfigurationScope;
import infra.app.logging.LoggerConfiguration.LevelConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class LoggerConfigurationTests {

  @Test
  void distinguishesDirectAndInheritedLevels() {
    LevelConfiguration direct = LevelConfiguration.of(LogLevel.DEBUG);
    LevelConfiguration inherited = LevelConfiguration.of(LogLevel.INFO);
    LoggerConfiguration configuration = new LoggerConfiguration("example", direct, inherited);

    assertThat(configuration.getLevelConfiguration(ConfigurationScope.DIRECT)).isSameAs(direct);
    assertThat(configuration.getLevelConfiguration(ConfigurationScope.INHERITED)).isSameAs(inherited);
    assertThat(configuration.getConfiguredLevel()).isEqualTo(LogLevel.DEBUG);
    assertThat(configuration.getEffectiveLevel()).isEqualTo(LogLevel.INFO);
    assertThat(direct.getName()).isEqualTo("DEBUG");
    assertThat(direct.isCustom()).isFalse();
  }

  @Test
  void exposesInheritedCustomLevelWithoutConvertingItToAnEnum() {
    LevelConfiguration inherited = LevelConfiguration.ofCustom("NOTICE");
    LoggerConfiguration configuration = new LoggerConfiguration("example", null, inherited);

    assertThat(configuration.getConfiguredLevel()).isNull();
    assertThat(configuration.getLevelConfiguration()).isSameAs(inherited);
    assertThat(inherited.getName()).isEqualTo("NOTICE");
    assertThat(inherited.isCustom()).isTrue();
    assertThatIllegalStateException().isThrownBy(configuration::getEffectiveLevel).withMessageContaining("NOTICE");
    assertThatIllegalArgumentException().isThrownBy(() -> LevelConfiguration.ofCustom(" "));
  }

  @Test
  void equalityUsesTheNameAndBothLevelConfigurations() {
    LoggerConfiguration configuration = new LoggerConfiguration("example", LogLevel.DEBUG, LogLevel.INFO);

    assertThat(configuration.equals(configuration)).isTrue();
    assertThat(configuration.equals(null)).isFalse();
    assertThat(configuration).isNotEqualTo("example")
            .isEqualTo(new LoggerConfiguration("example", LogLevel.DEBUG, LogLevel.INFO))
            .hasSameHashCodeAs(new LoggerConfiguration("example", LogLevel.DEBUG, LogLevel.INFO))
            .isNotEqualTo(new LoggerConfiguration("other", LogLevel.DEBUG, LogLevel.INFO))
            .isNotEqualTo(new LoggerConfiguration("example", null, LogLevel.INFO))
            .isNotEqualTo(new LoggerConfiguration("example", LogLevel.DEBUG, LogLevel.WARN));
    assertThat(configuration.toString()).contains("example", "DEBUG", "INFO");
  }

  @Test
  void customLevelsWithTheSameNameAreEqualButDistinctFromStandardLevels() {
    LevelConfiguration custom = LevelConfiguration.ofCustom("INFO");

    assertThat(custom.equals(custom)).isTrue();
    assertThat(custom.equals(null)).isFalse();
    assertThat(custom).isNotEqualTo("INFO").isNotEqualTo(LevelConfiguration.of(LogLevel.INFO))
            .isNotEqualTo(LevelConfiguration.ofCustom("NOTICE"))
            .isEqualTo(LevelConfiguration.ofCustom("INFO")).hasSameHashCodeAs(LevelConfiguration.ofCustom("INFO"));
    assertThat(custom.toString()).contains("INFO");
  }

}
