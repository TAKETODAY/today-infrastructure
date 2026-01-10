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

package infra.annotation.config.jdbc;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import javax.sql.DataSource;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Primary;
import infra.context.annotation.config.AutoConfigurations;
import infra.jdbc.core.JdbcOperations;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.core.namedparam.NamedParameterJdbcOperations;
import infra.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import infra.jdbc.support.SQLExceptionTranslator;
import infra.jdbc.support.SQLStateSQLExceptionTranslator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JdbcTemplateAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @author Dan Zheng
 */
class JdbcTemplateAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withPropertyValues("datasource.generate-unique-name=true").withConfiguration(
                  AutoConfigurations.of(DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class));

  @Test
  void testJdbcTemplateExists() {
    this.contextRunner.run((context) -> {
      assertThat(context).hasSingleBean(JdbcOperations.class);
      JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
      assertThat(jdbcTemplate.getDataSource()).isEqualTo(context.getBean(DataSource.class));
      assertThat(jdbcTemplate.getFetchSize()).isEqualTo(-1);
      assertThat(jdbcTemplate.getQueryTimeout()).isEqualTo(-1);
      assertThat(jdbcTemplate.getMaxRows()).isEqualTo(-1);
    });
  }

  @Test
  void testJdbcTemplateWithCustomProperties() {
    this.contextRunner.withPropertyValues("jdbc.template.fetch-size:100",
            "jdbc.template.query-timeout:60", "jdbc.template.max-rows:1000").run((context) -> {
      assertThat(context).hasSingleBean(JdbcOperations.class);
      JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
      assertThat(jdbcTemplate.getDataSource()).isNotNull();
      assertThat(jdbcTemplate.getFetchSize()).isEqualTo(100);
      assertThat(jdbcTemplate.getQueryTimeout()).isEqualTo(60);
      assertThat(jdbcTemplate.getMaxRows()).isEqualTo(1000);
    });
  }

  @Test
  void testJdbcTemplateExistsWithCustomDataSource() {
    this.contextRunner.withUserConfiguration(TestDataSourceConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(JdbcOperations.class);
      JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
      assertThat(jdbcTemplate.getDataSource()).isEqualTo(context.getBean("customDataSource"));
    });
  }

  @Test
  void testNamedParameterJdbcTemplateExists() {
    this.contextRunner.run((context) -> {
      assertThat(context).hasSingleBean(NamedParameterJdbcOperations.class);
      NamedParameterJdbcTemplate namedParameterJdbcTemplate = context.getBean(NamedParameterJdbcTemplate.class);
      assertThat(namedParameterJdbcTemplate.getJdbcOperations()).isEqualTo(context.getBean(JdbcOperations.class));
    });
  }

  @Test
  void testMultiDataSource() {
    this.contextRunner.withUserConfiguration(MultiDataSourceConfiguration.class).run((context) -> {
      assertThat(context).doesNotHaveBean(JdbcOperations.class);
      assertThat(context).doesNotHaveBean(NamedParameterJdbcOperations.class);
    });
  }

  @Test
  void testMultiJdbcTemplate() {
    this.contextRunner.withUserConfiguration(MultiJdbcTemplateConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean(NamedParameterJdbcOperations.class));
  }

  @Test
  void testMultiDataSourceUsingPrimary() {
    this.contextRunner.withUserConfiguration(MultiDataSourceUsingPrimaryConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(JdbcOperations.class);
      assertThat(context).hasSingleBean(NamedParameterJdbcOperations.class);
      assertThat(context.getBean(JdbcTemplate.class).getDataSource())
              .isEqualTo(context.getBean("test1DataSource"));
    });
  }

  @Test
  void testMultiJdbcTemplateUsingPrimary() {
    this.contextRunner.withUserConfiguration(MultiJdbcTemplateUsingPrimaryConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(NamedParameterJdbcOperations.class);
      assertThat(context.getBean(NamedParameterJdbcTemplate.class).getJdbcOperations())
              .isEqualTo(context.getBean("test1Template"));
    });
  }

  @Test
  void testExistingCustomJdbcTemplate() {
    this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(JdbcOperations.class);
      assertThat(context.getBean(JdbcOperations.class)).isEqualTo(context.getBean("customJdbcOperations"));
    });
  }

  @Test
  void testExistingCustomNamedParameterJdbcTemplate() {
    this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(NamedParameterJdbcOperations.class);
      assertThat(context.getBean(NamedParameterJdbcOperations.class))
              .isEqualTo(context.getBean("customNamedParameterJdbcOperations"));
    });
  }

  @Test
  void shouldConfigureJdbcTemplateWithSQLExceptionTranslatorIfPresent() {
    SQLStateSQLExceptionTranslator sqlExceptionTranslator = new SQLStateSQLExceptionTranslator();
    this.contextRunner.withBean(SQLExceptionTranslator.class, () -> sqlExceptionTranslator).run((context) -> {
      assertThat(context).hasSingleBean(JdbcTemplate.class);
      JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
      assertThat(jdbcTemplate.getExceptionTranslator()).isSameAs(sqlExceptionTranslator);
    });
  }

  @Test
  void shouldNotConfigureJdbcTemplateWithSQLExceptionTranslatorIfNotUnique() {
    SQLStateSQLExceptionTranslator sqlExceptionTranslator1 = new SQLStateSQLExceptionTranslator();
    SQLStateSQLExceptionTranslator sqlExceptionTranslator2 = new SQLStateSQLExceptionTranslator();
    this.contextRunner
            .withBean("sqlExceptionTranslator1", SQLExceptionTranslator.class, () -> sqlExceptionTranslator1)
            .withBean("sqlExceptionTranslator2", SQLExceptionTranslator.class, () -> sqlExceptionTranslator2)
            .run((context) -> {
              assertThat(context).hasSingleBean(JdbcTemplate.class);
              JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
              assertThat(jdbcTemplate.getExceptionTranslator()).isNotSameAs(sqlExceptionTranslator1)
                      .isNotSameAs(sqlExceptionTranslator2);
            });
  }

  @Configuration(proxyBeanMethods = false)
  static class CustomConfiguration {

    @Bean
    JdbcOperations customJdbcOperations(DataSource dataSource) {
      return new JdbcTemplate(dataSource);
    }

    @Bean
    NamedParameterJdbcOperations customNamedParameterJdbcOperations(DataSource dataSource) {
      return new NamedParameterJdbcTemplate(dataSource);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class TestDataSourceConfiguration {

    @Bean
    DataSource customDataSource() {
      return new TestDataSource();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class MultiJdbcTemplateConfiguration {

    @Bean
    JdbcTemplate test1Template() {
      return mock(JdbcTemplate.class);
    }

    @Bean
    JdbcTemplate test2Template() {
      return mock(JdbcTemplate.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class MultiJdbcTemplateUsingPrimaryConfiguration {

    @Bean
    @Primary
    JdbcTemplate test1Template() {
      return mock(JdbcTemplate.class);
    }

    @Bean
    JdbcTemplate test2Template() {
      return mock(JdbcTemplate.class);
    }

  }

  static class DataSourceInitializationValidator {

    private final Integer count;

    DataSourceInitializationValidator(JdbcTemplate jdbcTemplate) {
      this.count = jdbcTemplate.queryForObject("SELECT COUNT(*) from BAR", Integer.class);
    }

  }

  static class DataSourceMigrationValidator {

    private final Integer count;

    DataSourceMigrationValidator(JdbcTemplate jdbcTemplate) {
      this.count = jdbcTemplate.queryForObject("SELECT COUNT(*) from CITY", Integer.class);
    }

  }

  static class NamedParameterDataSourceMigrationValidator {

    private final Integer count;

    NamedParameterDataSourceMigrationValidator(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
      this.count = namedParameterJdbcTemplate.queryForObject("SELECT COUNT(*) from CITY", Collections.emptyMap(),
              Integer.class);
    }

  }

}
