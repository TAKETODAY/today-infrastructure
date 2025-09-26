/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.annotation.config.jdbc;

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
