/*
 * Copyright 2012-present the original author or authors.
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

package infra.maven;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AbstractRunMojo.SystemPropertyFormatter}.
 */
class SystemPropertyFormatterTests {

  @Test
  void parseEmpty() {
    Assertions.assertThat(AbstractRunMojo.SystemPropertyFormatter.format(null, null)).isEmpty();
  }

  @Test
  void parseOnlyKey() {
    Assertions.assertThat(AbstractRunMojo.SystemPropertyFormatter.format("key1", null)).isEqualTo("-Dkey1");
  }

  @Test
  void parseKeyWithValue() {
    Assertions.assertThat(AbstractRunMojo.SystemPropertyFormatter.format("key1", "value1")).isEqualTo("-Dkey1=\"value1\"");
  }

  @Test
  void parseKeyWithEmptyValue() {
    Assertions.assertThat(AbstractRunMojo.SystemPropertyFormatter.format("key1", "")).isEqualTo("-Dkey1");
  }

  @Test
  void parseKeyWithOnlySpaces() {
    Assertions.assertThat(AbstractRunMojo.SystemPropertyFormatter.format("key1", "   ")).isEqualTo("-Dkey1=\"   \"");
  }

}
