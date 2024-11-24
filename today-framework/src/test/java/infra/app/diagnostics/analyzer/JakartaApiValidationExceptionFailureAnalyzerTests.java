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

package infra.app.diagnostics.analyzer;

import org.junit.jupiter.api.Test;

import infra.app.diagnostics.analyzer.ValidationExceptionFailureAnalyzer;
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
