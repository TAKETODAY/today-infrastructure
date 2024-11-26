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

import infra.test.context.env.YamlTestProperties;

/**
 * Analogous to {@link LocalPropertiesFileAndMetaPropertiesFileTests} except
 * that the local file is YAML.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@YamlTestProperties("local.yaml")
@MetaFileTestProperty
class LocalYamlFileAndMetaPropertiesFileTests extends AbstractRepeatableTestPropertySourceTests {

  @Test
  void test() {
    assertEnvironmentValue("key1", "local file");
    assertEnvironmentValue("key2", "meta file");

    assertEnvironmentValue("environments.dev.url", "https://dev.example.com");
    assertEnvironmentValue("environments.dev.name", "Developer Setup");
  }

}
