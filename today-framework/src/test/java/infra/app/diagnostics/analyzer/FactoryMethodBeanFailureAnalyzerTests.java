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

import infra.app.diagnostics.FailureAnalysis;
import infra.app.diagnostics.LoggingFailureAnalysisReporter;
import infra.beans.FatalBeanException;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.lang.Nullable;
import infra.test.util.TestPropertyValues;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/1 10:26
 */
class FactoryMethodBeanFailureAnalyzerTests {

  @Nullable
  private FactoryMethodBeanFailureAnalyzer analyzer;

  @Test
  void failureAnalysisForNullBeanByType() {
    assertDescription(StringNullBeanOnConstructor.class);
    assertDescription(StringNullBeanOnMethod.class);
    assertDescription(StringNullBeanOnField.class);

    assertAction(StringNullBeanOnField.class);
    assertAction(StringNullBeanOnMethod.class);
    assertAction(StringNullBeanOnConstructor.class);

  }

  void assertDescription(Class<?> config) {
    FailureAnalysis analysis = analyzeFailure(createFailure(config));
    assertThat(analysis.getDescription()).startsWith("Only one bean named 'string' which qualifies as autowire candidate.");
  }

  void assertAction(Class<?> config) {
    FailureAnalysis analysis = analyzeFailure(createFailure(config));

    assertThat(analysis.getAction())
            .startsWith("Consider to make ")
            .endsWith(" @Nullable or @Autowired(required = false) in your configuration.");
  }

  private FatalBeanException createFailure(Class<?> config, String... environment) {
    try {
      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
      this.analyzer = new FactoryMethodBeanFailureAnalyzer();
      TestPropertyValues.of(environment).applyTo(context);
      context.register(config);
      context.refresh();
      context.close();
      throw new RuntimeException();
    }
    catch (FatalBeanException ex) {
      return ex;
    }
  }

  private FailureAnalysis analyzeFailure(Exception failure) {
    if (analyzer == null) {
      analyzer = new FactoryMethodBeanFailureAnalyzer();
    }
    FailureAnalysis analysis = this.analyzer.analyze(failure);
    if (analysis != null) {
      new LoggingFailureAnalysisReporter().report(analysis);
    }
    return analysis;
  }

  @Configuration(proxyBeanMethods = false)
  @ImportAutoConfiguration(TestNullBeanConfiguration.class)
  @Import({ StringHandler.class })
  static class StringNullBeanOnConstructor {

  }

  @Configuration(proxyBeanMethods = false)
  @ImportAutoConfiguration(TestNullBeanConfiguration.class)
  @Import({ NullStringMethod.class })
  static class StringNullBeanOnMethod {

  }

  @Configuration(proxyBeanMethods = false)
  @ImportAutoConfiguration(TestNullBeanConfiguration.class)
  @Import({ NullStringAutowired.class })
  static class StringNullBeanOnField {

  }

  @Configuration(proxyBeanMethods = false)
  static class TestNullBeanConfiguration {

    @Bean
    @Nullable
    String string() {
      return null;
    }

  }

  static class StringHandler {

    StringHandler(String foo) { }

  }

  static class NullStringAutowired {

    @Autowired
    String foo;

  }

  static class NullStringMethod {

    String foo;

    @Autowired
    public void setFoo(String foo) {
      this.foo = foo;
    }
  }

}