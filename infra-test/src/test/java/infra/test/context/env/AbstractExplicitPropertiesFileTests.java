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
