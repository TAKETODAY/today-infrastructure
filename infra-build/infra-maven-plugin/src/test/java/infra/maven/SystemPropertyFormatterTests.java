/*
 * Copyright 2017 - 2024 the original author or authors.
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
