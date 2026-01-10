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
