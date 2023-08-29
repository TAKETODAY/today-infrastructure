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

package cn.taketoday.context.annotation.config;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/29 16:11
 */
class AutoConfigurationMetadataTests {

  @Test
  void loadShouldLoadProperties() {
    assertThat(load()).isNotNull();
  }

  @Test
  void wasProcessedWhenProcessedShouldReturnTrue() {
    assertThat(load().wasProcessed("test")).isTrue();
  }

  @Test
  void wasProcessedWhenNotProcessedShouldReturnFalse() {
    assertThat(load().wasProcessed("testx")).isFalse();
  }

  @Test
  void getIntegerShouldReturnValue() {
    assertThat(load().getInteger("test", "int")).isEqualTo(123);
  }

  @Test
  void getIntegerWhenMissingShouldReturnNull() {
    assertThat(load().getInteger("test", "intx")).isNull();
  }

  @Test
  void getIntegerWithDefaultWhenMissingShouldReturnDefault() {
    assertThat(load().getInteger("test", "intx", 345)).isEqualTo(345);
  }

  @Test
  void getSetShouldReturnValue() {
    assertThat(load().getSet("test", "set")).containsExactly("a", "b", "c");
  }

  @Test
  void getSetWhenMissingShouldReturnNull() {
    assertThat(load().getSet("test", "setx")).isNull();
  }

  @Test
  void getSetWithDefaultWhenMissingShouldReturnDefault() {
    assertThat(load().getSet("test", "setx", Collections.singleton("x"))).containsExactly("x");
  }

  @Test
  void getShouldReturnValue() {
    assertThat(load().get("test", "string")).isEqualTo("abc");
  }

  @Test
  void getWhenMissingShouldReturnNull() {
    assertThat(load().get("test", "stringx")).isNull();
  }

  @Test
  void getWithDefaultWhenMissingShouldReturnDefault() {
    assertThat(load().get("test", "stringx", "xyz")).isEqualTo("xyz");
  }

  private AutoConfigurationMetadata load() {
    return AutoConfigurationMetadata.load(null,
            "META-INF/AutoConfigurationMetadataTests.properties");
  }

}