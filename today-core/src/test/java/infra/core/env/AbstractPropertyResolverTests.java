/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.core.env;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import infra.lang.Nullable;
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
    setSpringProperty(null);
  }

  @Test
  void getDefaultEscapeCharacterWithSpringPropertySetToCharacterMinValue() {
    setSpringProperty("" + Character.MIN_VALUE);

    assertThatIllegalArgumentException()
            .isThrownBy(AbstractPropertyResolver::getDefaultEscapeCharacter)
            .withMessage("Value for property [%s] must not be Character.MIN_VALUE",
                    DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME);

    assertThat(AbstractPropertyResolver.defaultEscapeCharacter).isEqualTo(UNDEFINED_ESCAPE_CHARACTER);
  }

  @Test
  void getDefaultEscapeCharacterWithSpringPropertySetToXyz() {
    setSpringProperty("XYZ");

    assertThatIllegalArgumentException()
            .isThrownBy(AbstractPropertyResolver::getDefaultEscapeCharacter)
            .withMessage("Value [XYZ] for property [%s] must be a single character or an empty string",
                    DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME);

    assertThat(AbstractPropertyResolver.defaultEscapeCharacter).isEqualTo(UNDEFINED_ESCAPE_CHARACTER);
  }

  @Test
  void getDefaultEscapeCharacterWithSpringPropertySetToEmptyString() {
    setSpringProperty("");
    assertEscapeCharacter(null);
  }

  @Test
  void getDefaultEscapeCharacterWithoutSpringPropertySet() {
    assertEscapeCharacter('\\');
  }

  @Test
  void getDefaultEscapeCharacterWithSpringPropertySetToBackslash() {
    setSpringProperty("\\");
    assertEscapeCharacter('\\');
  }

  @Test
  void getDefaultEscapeCharacterWithSpringPropertySetToTilde() {
    setSpringProperty("~");
    assertEscapeCharacter('~');
  }

  @Test
  void getDefaultEscapeCharacterFromMultipleThreads() {
    setSpringProperty("~");

    IntStream.range(1, 32).parallel().forEach(__ ->
            assertThat(AbstractPropertyResolver.getDefaultEscapeCharacter()).isEqualTo('~'));

    assertThat(AbstractPropertyResolver.defaultEscapeCharacter).isEqualTo('~');
  }

  private static void setSpringProperty(String value) {
    TodayStrategies.setProperty(DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME, value);
  }

  private static void assertEscapeCharacter(@Nullable Character expected) {
    assertThat(AbstractPropertyResolver.getDefaultEscapeCharacter()).isEqualTo(expected);
    assertThat(AbstractPropertyResolver.defaultEscapeCharacter).isEqualTo(expected);
  }

}