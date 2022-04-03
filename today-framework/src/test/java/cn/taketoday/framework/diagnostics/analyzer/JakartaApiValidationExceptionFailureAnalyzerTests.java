/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.diagnostics.analyzer;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ValidationExceptionFailureAnalyzer}
 *
 * @author Andy Wilkinson
 */
//@ClassPathExclusions("hibernate-validator-*.jar")
class JakartaApiValidationExceptionFailureAnalyzerTests {

  @Test
  @Disabled("TODO")
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

  @EnableAutoConfiguration
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
