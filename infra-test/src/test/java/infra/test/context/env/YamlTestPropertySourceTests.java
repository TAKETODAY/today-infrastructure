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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Configuration;
import infra.core.env.ConfigurableEnvironment;
import infra.core.io.PropertySourceFactory;
import infra.test.context.TestPropertySource;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource} support
 * with a custom YAML {@link PropertySourceFactory}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
@YamlTestProperties("test-properties.yaml")
class YamlTestPropertySourceTests {

  @ParameterizedTest
  @CsvSource(delimiterString = "->", textBlock = """
      environments.dev.url   -> https://dev.example.com
      environments.dev.name  -> 'Developer Setup'
      environments.prod.url  -> https://prod.example.com
      environments.prod.name -> 'My Cool App'
      """)
  void propertyIsAvailableInEnvironment(String property, String value, @Autowired ConfigurableEnvironment env) {
    assertThat(env.getProperty(property)).isEqualTo(value);
  }

  @Configuration
  static class Config {
    /* no user beans required for these tests */
  }

}
