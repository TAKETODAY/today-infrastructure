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

package cn.taketoday.test.context.env;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.env.Environment;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource} support
 * with custom resource encoding.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
class CustomEncodingTestPropertySourceTests {

  @Nested
  @TestPropertySource(locations = "test-ISO-8859-1.properties", encoding = "UTF-8")
  @DirtiesContext
  class IncorrectEncodingTests {

    @Test
    void propertyIsAvailableInEnvironment(@Autowired Environment env) {
      // The "é" characters in "Générales" get converted to U+FFFD : REPLACEMENT CHARACTER.
      assertThat(env.getProperty("text")).isEqualTo("G\uFFFDn\uFFFDrales");
    }
  }

  @Nested
  @TestPropertySource(locations = "test-ISO-8859-1.properties", encoding = "ISO-8859-1")
  @DirtiesContext
  class ExplicitEncodingTests {

    @Test
    void propertyIsAvailableInEnvironment(@Autowired Environment env) {
      assertThat(env.getProperty("text")).isEqualTo("Générales");
    }
  }

  @Configuration
  static class Config {
    /* no user beans required for these tests */
  }

}
