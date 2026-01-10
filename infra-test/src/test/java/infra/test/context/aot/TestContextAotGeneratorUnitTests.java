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

package infra.test.context.aot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import infra.lang.TodayStrategies;

import static infra.test.context.aot.TestContextAotGenerator.FAIL_ON_ERROR_PROPERTY_NAME;
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
  void failOnErrorEnabledViaInfraProperty(String value) {
    TodayStrategies.setProperty(FAIL_ON_ERROR_PROPERTY_NAME, value);
    assertThat(createGenerator().failOnError).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = { "false", "  False\t", "x" })
  void failOnErrorDisabledViaInfraProperty(String value) {
    TodayStrategies.setProperty(FAIL_ON_ERROR_PROPERTY_NAME, value);
    assertThat(createGenerator().failOnError).isFalse();
  }

  private static TestContextAotGenerator createGenerator() {
    return new TestContextAotGenerator(null);
  }

}
