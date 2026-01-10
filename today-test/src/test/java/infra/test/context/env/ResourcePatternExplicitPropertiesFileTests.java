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

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Configuration;
import infra.core.env.Environment;
import infra.test.context.TestPropertySource;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource} support
 * for resource patterns for resource locations.
 *
 * @author Sam Brannen
 */
@JUnitConfig
@TestPropertySource("classpath*:/infra/test/context/env/pattern/file?.properties")
class ResourcePatternExplicitPropertiesFileTests {

  @Test
  void verifyPropertiesAreAvailableInEnvironment(@Autowired Environment env) {
    assertEnvironmentContainsProperties(env, "from.p1", "from.p2", "from.p3");
  }

  private static void assertEnvironmentContainsProperties(Environment env, String... names) {
    for (String name : names) {
      assertThat(env.containsProperty(name)).as("environment contains property '%s'", name).isTrue();
    }
  }

  @Configuration
  static class Config {
    /* no user beans required for these tests */
  }

}
