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

package infra.context.annotation.config;

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