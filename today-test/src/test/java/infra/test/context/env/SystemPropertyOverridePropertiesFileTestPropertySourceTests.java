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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Configuration;
import infra.core.env.Environment;
import infra.test.context.ContextConfiguration;
import infra.test.context.TestPropertySource;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource}
 * support with an explicitly named properties file that overrides a
 * system property.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ExtendWith(InfraExtension.class)
@ContextConfiguration
@TestPropertySource("SystemPropertyOverridePropertiesFileTestPropertySourceTests.properties")
class SystemPropertyOverridePropertiesFileTestPropertySourceTests {

  private static final String KEY = SystemPropertyOverridePropertiesFileTestPropertySourceTests.class.getSimpleName() + ".riddle";

  @Autowired
  protected Environment env;

  @BeforeAll
  static void setSystemProperty() {
    System.setProperty(KEY, "override me!");
  }

  @AfterAll
  static void removeSystemProperty() {
    System.setProperty(KEY, "");
  }

  @Test
  void verifyPropertiesAreAvailableInEnvironment() {
    assertThat(env.getProperty(KEY)).isEqualTo("enigma");
  }

  // -------------------------------------------------------------------

  @Configuration
  static class Config {
    /* no user beans required for these tests */
  }

}
