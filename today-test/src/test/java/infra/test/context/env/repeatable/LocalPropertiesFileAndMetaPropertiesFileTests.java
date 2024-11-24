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

package infra.test.context.env.repeatable;

import org.junit.jupiter.api.Test;

import infra.test.context.TestPropertySource;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource} as a
 * repeatable annotation.
 *
 * <p>Verify a property value is defined both in the properties file which is declared
 * via {@link MetaFileTestProperty @MetaFileTestProperty} and in the properties file
 * which is declared locally via {@code @TestPropertySource}.
 *
 * @author Anatoliy Korovin
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@TestPropertySource("local.properties")
@MetaFileTestProperty
class LocalPropertiesFileAndMetaPropertiesFileTests extends AbstractRepeatableTestPropertySourceTests {

  @Test
  void test() {
    assertEnvironmentValue("key1", "local file");
    assertEnvironmentValue("key2", "meta file");
  }

}
