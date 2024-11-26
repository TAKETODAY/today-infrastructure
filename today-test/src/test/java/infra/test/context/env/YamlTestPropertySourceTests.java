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
