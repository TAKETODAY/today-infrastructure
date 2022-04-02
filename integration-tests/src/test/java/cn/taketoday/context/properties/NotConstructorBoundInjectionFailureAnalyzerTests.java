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

package cn.taketoday.context.properties;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.FatalBeanException;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.framework.diagnostics.LoggingFailureAnalysisReporter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NotConstructorBoundInjectionFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 */
class NotConstructorBoundInjectionFailureAnalyzerTests {

  private final NotConstructorBoundInjectionFailureAnalyzer analyzer = new NotConstructorBoundInjectionFailureAnalyzer();

  @Test
  void failureAnalysisForConfigurationPropertiesThatShouldHaveBeenConstructorBound() {
    FailureAnalysis analysis = analyzeFailure(
            createFailure(ShouldHaveUsedConstructorBindingPropertiesConfiguration.class));
    assertThat(analysis.getDescription()).isEqualTo(ConstructorBoundProperties.class.getSimpleName()
            + " is annotated with @" + ConstructorBinding.class.getSimpleName()
            + " but it is defined as a regular bean which caused dependency injection to fail.");
    assertThat(analysis.getAction())
            .isEqualTo("Update your configuration so that " + ConstructorBoundProperties.class.getSimpleName()
                    + " is defined via @" + ConfigurationPropertiesScan.class.getSimpleName() + " or @"
                    + EnableConfigurationProperties.class.getSimpleName() + ".");
  }

  @Test
  void failureAnalysisForNonConstructorBoundProperties() {
    FailureAnalysis analysis = analyzeFailure(createFailure(JavaBeanBoundPropertiesConfiguration.class));
    assertThat(analysis).isNull();
  }

  private FatalBeanException createFailure(Class<?> config) {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(config);
      context.refresh();
      return null;
    }
    catch (FatalBeanException ex) {
      return ex;
    }
  }

  private FailureAnalysis analyzeFailure(Exception failure) {
    assertThat(failure).isNotNull();
    FailureAnalysis analysis = this.analyzer.analyze(failure);
    if (analysis != null) {
      new LoggingFailureAnalysisReporter().report(analysis);
    }
    return analysis;
  }

  @ConfigurationProperties("test")
  static class ConstructorBoundProperties {

    private final String name;

    ConstructorBoundProperties(String name) {
      this.name = name;
    }

    String getName() {
      return this.name;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @Import(ConstructorBoundProperties.class)
  static class ShouldHaveUsedConstructorBindingPropertiesConfiguration {

  }

  @ConfigurationProperties("test")
  static class JavaBeanBoundProperties {

    private String name;

    @Autowired
    JavaBeanBoundProperties(String dependency) {

    }

    String getName() {
      return this.name;
    }

    void setName(String name) {
      this.name = name;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @Import(JavaBeanBoundProperties.class)
  static class JavaBeanBoundPropertiesConfiguration {

  }

}
