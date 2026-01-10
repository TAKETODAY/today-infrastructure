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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.diagnostics.analyzer;

import org.junit.jupiter.api.Test;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.properties.ConfigurationProperties;
import infra.context.properties.EnableConfigurationProperties;
import infra.test.classpath.ClassPathExclusions;
import infra.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ValidationExceptionFailureAnalyzer}
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("hibernate-validator-*.jar")
class JakartaApiValidationExceptionFailureAnalyzerTests {

  @Test
  void validatedPropertiesTest() {
    assertThatExceptionOfType(Exception.class)
            .isThrownBy(() -> new AnnotationConfigApplicationContext(TestConfiguration.class).close())
            .satisfies((ex) -> assertThat(new ValidationExceptionFailureAnalyzer().analyze(ex)).isNotNull());
  }

  @Test
  void nonValidatedPropertiesTest() {
    new AnnotationConfigApplicationContext(NonValidatedTestConfiguration.class).close();
  }

  @EnableConfigurationProperties(TestProperties.class)
  static class TestConfiguration {

    TestConfiguration(TestProperties testProperties) {
    }

  }

  @ConfigurationProperties("test")
  @Validated
  static class TestProperties {

  }

  @EnableConfigurationProperties(NonValidatedTestProperties.class)
  static class NonValidatedTestConfiguration {

    NonValidatedTestConfiguration(NonValidatedTestProperties testProperties) {
    }

  }

  @ConfigurationProperties("test")
  static class NonValidatedTestProperties {

  }

}
