/*
 * Copyright 2002-present the original author or authors.
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

package infra.core.env;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import infra.lang.TodayStrategies;

import static infra.core.env.AbstractPropertyResolver.DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME;
import static infra.core.env.AbstractPropertyResolver.UNDEFINED_ESCAPE_CHARACTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/6/18 22:45
 */
class AbstractPropertyResolverTests {

  @BeforeEach
  void resetStateBeforeEachTest() {
    resetState();
  }

  @AfterAll
  static void resetState() {
    AbstractPropertyResolver.defaultEscapeCharacter = UNDEFINED_ESCAPE_CHARACTER;
    setInfraProperty(null);
  }

  @Test
  void getDefaultEscapeCharacterWithInfraPropertySetToCharacterMinValue() {
    setInfraProperty("" + Character.MIN_VALUE);

    assertThatIllegalArgumentException()
            .isThrownBy(AbstractPropertyResolver::getDefaultEscapeCharacter)
            .withMessage("Value for property [%s] must not be Character.MIN_VALUE",
                    DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME);

    assertThat(AbstractPropertyResolver.defaultEscapeCharacter).isEqualTo(UNDEFINED_ESCAPE_CHARACTER);
  }

  @Test
  void getDefaultEscapeCharacterWithInfraPropertySetToXyz() {
    setInfraProperty("XYZ");

    assertThatIllegalArgumentException()
            .isThrownBy(AbstractPropertyResolver::getDefaultEscapeCharacter)
            .withMessage("Value [XYZ] for property [%s] must be a single character or an empty string",
                    DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME);

    assertThat(AbstractPropertyResolver.defaultEscapeCharacter).isEqualTo(UNDEFINED_ESCAPE_CHARACTER);
  }

  @Test
  void getDefaultEscapeCharacterWithInfraPropertySetToEmptyString() {
    setInfraProperty("");
    assertEscapeCharacter(null);
  }

  @Test
  void getDefaultEscapeCharacterWithoutInfraPropertySet() {
    assertEscapeCharacter('\\');
  }

  @Test
  void getDefaultEscapeCharacterWithInfraPropertySetToBackslash() {
    setInfraProperty("\\");
    assertEscapeCharacter('\\');
  }

  @Test
  void getDefaultEscapeCharacterWithInfraPropertySetToTilde() {
    setInfraProperty("~");
    assertEscapeCharacter('~');
  }

  @Test
  void getDefaultEscapeCharacterFromMultipleThreads() {
    setInfraProperty("~");

    IntStream.range(1, 32).parallel().forEach(__ ->
            assertThat(AbstractPropertyResolver.getDefaultEscapeCharacter()).isEqualTo('~'));

    assertThat(AbstractPropertyResolver.defaultEscapeCharacter).isEqualTo('~');
  }

  private static void setInfraProperty(String value) {
    TodayStrategies.setProperty(DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME, value);
  }

  private static void assertEscapeCharacter(@Nullable Character expected) {
    assertThat(AbstractPropertyResolver.getDefaultEscapeCharacter()).isEqualTo(expected);
    assertThat(AbstractPropertyResolver.defaultEscapeCharacter).isEqualTo(expected);
  }

}