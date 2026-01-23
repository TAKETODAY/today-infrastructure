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

package infra.jdbc.config;

import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.Test;

import infra.app.diagnostics.FailureAnalysis;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.jdbc.CannotGetJdbcConnectionException;
import infra.jdbc.datasource.DataSourceUtils;
import infra.test.util.TestPropertyValues;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HikariDriverConfigurationFailureAnalyzer}.
 *
 * @author Stephane Nicoll
 */
class HikariDriverConfigurationFailureAnalyzerTests {

  @Test
  void failureAnalysisIsPerformed() {
    FailureAnalysis failureAnalysis = performAnalysis(TestConfiguration.class);
    assertThat(failureAnalysis).isNotNull();
    assertThat(failureAnalysis.getDescription()).isEqualTo(
            "Configuration of the Hikari connection pool failed: 'dataSourceClassName' is not supported.");
    assertThat(failureAnalysis.getAction()).contains("Infra auto-configures only a driver");
  }

  @Test
  void unrelatedIllegalStateExceptionIsSkipped() {
    FailureAnalysis failureAnalysis = new HikariDriverConfigurationFailureAnalyzer()
            .analyze(new RuntimeException("foo", new IllegalStateException("bar")));
    assertThat(failureAnalysis).isNull();
  }

  private FailureAnalysis performAnalysis(Class<?> configuration) {
    CannotGetJdbcConnectionException failure = createFailure(configuration);
    assertThat(failure).isNotNull();
    return new HikariDriverConfigurationFailureAnalyzer().analyze(failure);
  }

  private CannotGetJdbcConnectionException createFailure(Class<?> configuration) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    TestPropertyValues.of("datasource.type=" + HikariDataSource.class.getName(),
                    "datasource.hikari.data-source-class-name=com.example.Foo")
            .applyTo(context);
    context.register(configuration);
    try {
      context.refresh();

      HikariDataSource bean = context.getBean(HikariDataSource.class);
      DataSourceUtils.getConnection(bean);
      context.close();
      return null;
    }
    catch (CannotGetJdbcConnectionException ex) {
      return ex;
    }
  }

  @Configuration(proxyBeanMethods = false)
  @ImportAutoConfiguration({ DataSourceAutoConfiguration.class })
  static class TestConfiguration {

  }

}
