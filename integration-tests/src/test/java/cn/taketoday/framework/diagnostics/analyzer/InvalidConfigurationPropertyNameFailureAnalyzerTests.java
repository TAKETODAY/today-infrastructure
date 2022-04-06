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

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.framework.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InvalidConfigurationPropertyNameFailureAnalyzer}.
 *
 * @author Madhura Bhave
 */
class InvalidConfigurationPropertyNameFailureAnalyzerTests {

  private InvalidConfigurationPropertyNameFailureAnalyzer analyzer = new InvalidConfigurationPropertyNameFailureAnalyzer();

  @Test
  void analysisWhenRootCauseIsBeanCreationFailureShouldContainBeanName() {
    BeanCreationException failure = createFailure(InvalidPrefixConfiguration.class);
    FailureAnalysis analysis = this.analyzer.analyze(failure);
    assertThat(analysis.getDescription())
            .contains(String.format("%n    Invalid characters: %s%n    Bean: %s%n    Reason: %s", "'F', 'P'",
                    "invalidPrefixProperties", "Canonical names should be kebab-case ('-' separated), "
                            + "lowercase alpha-numeric characters and must start with a letter"));
  }

  private BeanCreationException createFailure(Class<?> configuration) {
    try {
      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
      context.register(configuration);
      context.refresh();
      context.close();
      return null;
    }
    catch (BeanCreationException ex) {
      return ex;
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(InvalidPrefixProperties.class)
  static class InvalidPrefixConfiguration {

    @Bean(name = "invalidPrefixProperties")
    InvalidPrefixProperties invalidPrefixProperties() {
      return new InvalidPrefixProperties();
    }

  }

  @ConfigurationProperties("FooPrefix")
  static class InvalidPrefixProperties {

    private String value;

    String getValue() {
      return this.value;
    }

    void setValue(String value) {
      this.value = value;
    }

  }

}
