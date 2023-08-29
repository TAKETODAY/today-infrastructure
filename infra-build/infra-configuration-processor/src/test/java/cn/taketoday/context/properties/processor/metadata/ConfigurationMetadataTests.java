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

package cn.taketoday.context.properties.processor.metadata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationMetadata}.
 *
 * @author Stephane Nicoll
 */
class ConfigurationMetadataTests {

  @Test
  void toDashedCaseCamelCase() {
    assertThat(toDashedCase("simpleCamelCase")).isEqualTo("simple-camel-case");
  }

  @Test
  void toDashedCaseUpperCamelCaseSuffix() {
    assertThat(toDashedCase("myDLQ")).isEqualTo("my-d-l-q");
  }

  @Test
  void toDashedCaseUpperCamelCaseMiddle() {
    assertThat(toDashedCase("someDLQKey")).isEqualTo("some-d-l-q-key");
  }

  @Test
  void toDashedCaseWordsUnderscore() {
    assertThat(toDashedCase("Word_With_underscore")).isEqualTo("word-with-underscore");
  }

  @Test
  void toDashedCaseWordsSeveralUnderscores() {
    assertThat(toDashedCase("Word___With__underscore")).isEqualTo("word---with--underscore");
  }

  @Test
  void toDashedCaseLowerCaseUnderscore() {
    assertThat(toDashedCase("lower_underscore")).isEqualTo("lower-underscore");
  }

  @Test
  void toDashedCaseUpperUnderscoreSuffix() {
    assertThat(toDashedCase("my_DLQ")).isEqualTo("my-d-l-q");
  }

  @Test
  void toDashedCaseUpperUnderscoreMiddle() {
    assertThat(toDashedCase("some_DLQ_key")).isEqualTo("some-d-l-q-key");
  }

  @Test
  void toDashedCaseMultipleUnderscores() {
    assertThat(toDashedCase("super___crazy")).isEqualTo("super---crazy");
  }

  @Test
  void toDashedCaseLowercase() {
    assertThat(toDashedCase("lowercase")).isEqualTo("lowercase");
  }

  private String toDashedCase(String name) {
    return ConfigurationMetadata.toDashedCase(name);
  }

}
