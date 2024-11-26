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

import org.junit.jupiter.api.Test;

import infra.test.context.TestPropertySource;

/**
 * Integration tests that verify detection of default properties files
 * when {@link TestPropertySource @TestPropertySource} is <em>empty</em>
 * at multiple levels within a class hierarchy.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@TestPropertySource
class ExtendedDefaultPropertiesFileDetectionTestPropertySourceTests extends
        DefaultPropertiesFileDetectionTestPropertySourceTests {

  @Test
  @Override
  void verifyPropertiesAreAvailableInEnvironment() {
    super.verifyPropertiesAreAvailableInEnvironment();
    // from ExtendedDefaultPropertiesFileDetectionTestPropertySourceTests.properties
    assertEnvironmentValue("enigma", "auto detected");
  }

}
