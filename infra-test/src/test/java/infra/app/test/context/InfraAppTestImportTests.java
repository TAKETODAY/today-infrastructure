/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.test.context;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.core.env.Environment;
import infra.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfraTest @InfraTest} with {@link Import @Import}.
 *
 * @author Andy Wilkinson
 */
@TestPropertySource(properties = "a=alpha")
@Import(InfraAppTestImportTests.ImportedByContainingTests.class)
class InfraAppTestImportTests {

  @Nested
  @InfraTest(classes = Config.class)
  @Import(ImportedByNestedTests.class)
  class NestedTests {

    @Autowired(required = false)
    private ImportedByContainingTests importedByContainingTests;

    @Autowired(required = false)
    private ImportedByNestedTests importedByNestedTests;

    @Autowired
    private Environment environment;

    @Test
    void sameClassImportIsHonored() {
      assertThat(this.importedByNestedTests).isNotNull();
    }

    @Test
    void containingClassImportIsHonored() {
      assertThat(this.importedByContainingTests).isNotNull();
    }

    @Test
    void containingClassTestPropertySourceIsHonored() {
      assertThat(this.environment.getProperty("a")).isEqualTo("alpha");
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

  }

  static class ImportedByNestedTests {

  }

  static class ImportedByContainingTests {

  }

}
