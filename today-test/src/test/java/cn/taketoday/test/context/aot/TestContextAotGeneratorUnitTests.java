/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.test.context.aot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import cn.taketoday.lang.TodayStrategies;

import static cn.taketoday.test.context.aot.TestContextAotGenerator.FAIL_ON_ERROR_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TestContextAotGenerator}.
 *
 * @author Sam Brannen
 */
class TestContextAotGeneratorUnitTests {

  @BeforeEach
  @AfterEach
  void resetFlag() {
    TodayStrategies.setProperty(FAIL_ON_ERROR_PROPERTY_NAME, null);
  }

  @Test
  void failOnErrorEnabledByDefault() {
    assertThat(createGenerator().failOnError).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = { "true", "  True\t" })
  void failOnErrorEnabledViaSpringProperty(String value) {
    TodayStrategies.setProperty(FAIL_ON_ERROR_PROPERTY_NAME, value);
    assertThat(createGenerator().failOnError).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = { "false", "  False\t", "x" })
  void failOnErrorDisabledViaSpringProperty(String value) {
    TodayStrategies.setProperty(FAIL_ON_ERROR_PROPERTY_NAME, value);
    assertThat(createGenerator().failOnError).isFalse();
  }

  private static TestContextAotGenerator createGenerator() {
    return new TestContextAotGenerator(null);
  }

}
