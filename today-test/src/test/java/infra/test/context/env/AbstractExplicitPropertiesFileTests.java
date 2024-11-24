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

package infra.test.context.env;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Configuration;
import infra.core.env.Environment;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
abstract class AbstractExplicitPropertiesFileTests {

  @Autowired
  Environment env;

  @Test
  @DisplayName("verify properties are available in the Environment")
  void verifyPropertiesAreAvailableInEnvironment() {
    String userHomeKey = "user.home";
    assertThat(env.getProperty(userHomeKey)).isEqualTo(System.getProperty(userHomeKey));
    assertThat(env.getProperty("explicit")).isEqualTo("enigma");
  }

  @Configuration
  static class Config {
    /* no user beans required for these tests */
  }

}
