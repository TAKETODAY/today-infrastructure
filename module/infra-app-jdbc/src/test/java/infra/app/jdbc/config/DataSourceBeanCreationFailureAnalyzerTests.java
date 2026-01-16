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

package infra.app.jdbc.config;

import org.junit.jupiter.api.Test;

import infra.app.diagnostics.FailureAnalysis;
import infra.beans.factory.BeanCreationException;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.mock.env.MockEnvironment;
import infra.test.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/31 14:02
 */
@ClassPathExclusions({ "h2-*.jar", "hsqldb-*.jar", "derby*.jar" })
class DataSourceBeanCreationFailureAnalyzerTests {

  private final MockEnvironment environment = new MockEnvironment();

  @Test
  void failureAnalysisIsPerformed() {
    FailureAnalysis failureAnalysis = performAnalysis(TestConfiguration.class);
    assertThat(failureAnalysis.getDescription()).contains("'url' attribute is not specified",
            "no embedded datasource could be configured", "Failed to determine a suitable driver class");
    assertThat(failureAnalysis.getAction()).contains(
            "If you want an embedded database (H2, HSQL or Derby), please put it on the classpath",
            "If you have database settings to be loaded from a particular profile you may need to activate it",
            "(no profiles are currently active)");
  }

  @Test
  void failureAnalysisIsPerformedWithActiveProfiles() {
    this.environment.setActiveProfiles("first", "second");
    FailureAnalysis failureAnalysis = performAnalysis(TestConfiguration.class);
    assertThat(failureAnalysis.getAction()).contains("(the profiles first,second are currently active)");
  }

  private FailureAnalysis performAnalysis(Class<?> configuration) {
    BeanCreationException failure = createFailure(configuration);
    assertThat(failure).isNotNull();
    var failureAnalyzer = new DataSourceBeanCreationFailureAnalyzer(this.environment);
    return failureAnalyzer.analyze(failure);
  }

  private BeanCreationException createFailure(Class<?> configuration) {
    try {
      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
      context.setEnvironment(this.environment);
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
  @ImportAutoConfiguration(DataSourceAutoConfiguration.class)
  static class TestConfiguration {

  }

}