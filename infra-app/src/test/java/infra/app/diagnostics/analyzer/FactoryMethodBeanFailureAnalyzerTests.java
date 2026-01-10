/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.app.diagnostics.analyzer;

import org.jspecify.annotations.Nullable;
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