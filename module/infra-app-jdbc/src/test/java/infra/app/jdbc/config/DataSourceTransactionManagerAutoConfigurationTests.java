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

import java.util.UUID;

import javax.sql.DataSource;

import infra.annotation.config.transaction.TransactionAutoConfiguration;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.config.AutoConfigurations;
import infra.jdbc.datasource.DataSourceTransactionManager;
import infra.jdbc.support.JdbcTransactionManager;
import infra.transaction.TransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/31 14:04
 */
class DataSourceTransactionManagerAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(TransactionAutoConfiguration.class,
                  DataSourceTransactionManagerAutoConfiguration.class))
          .withPropertyValues("datasource.url:jdbc:hsqldb:mem:test-" + UUID.randomUUID());

  @Test
  void transactionManagerWithoutDataSourceIsNotConfigured() {
    this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(TransactionManager.class));
  }

  @Test
  void transactionManagerWithExistingDataSourceIsConfigured() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .run((context) -> {
              assertThat(context).hasSingleBean(TransactionManager.class)
                      .hasSingleBean(JdbcTransactionManager.class);
              assertThat(context.getBean(JdbcTransactionManager.class).getDataSource())
                      .isSameAs(context.getBean(DataSource.class));
            });
  }

  @Test
  void transactionManagerWithCustomizationIsConfigured() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .withPropertyValues("transaction.default-timeout=1m",
                    "transaction.rollback-on-commit-failure=true")
            .run((context) -> {
              assertThat(context).hasSingleBean(TransactionManager.class)
                      .hasSingleBean(JdbcTransactionManager.class);
              JdbcTransactionManager transactionManager = context.getBean(JdbcTransactionManager.class);
              assertThat(transactionManager.getDefaultTimeout()).isEqualTo(60);
              assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
            });
  }

  @Test
  void transactionManagerWithExistingTransactionManagerIsNotOverridden() {
    this.contextRunner
            .withBean("myTransactionManager", TransactionManager.class, () -> mock(TransactionManager.class))
            .run((context) -> assertThat(context).hasSingleBean(TransactionManager.class)
                    .hasBean("myTransactionManager"));
  }

  @Test
  void transactionManagerWithDaoExceptionTranslationDisabled() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .withPropertyValues("dao.exceptiontranslation.enabled=false")
            .run((context) -> assertThat(context.getBean(TransactionManager.class))
                    .isExactlyInstanceOf(DataSourceTransactionManager.class));
  }

  @Test
  void transactionManagerWithDaoExceptionTranslationEnabled() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .withPropertyValues("dao.exceptiontranslation.enabled=true")
            .run((context) -> assertThat(context.getBean(TransactionManager.class))
                    .isExactlyInstanceOf(JdbcTransactionManager.class));
  }

  @Test
    // gh-24321
  void transactionManagerWithDaoExceptionTranslationDefault() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .run((context) -> assertThat(context.getBean(TransactionManager.class))
                    .isExactlyInstanceOf(JdbcTransactionManager.class));
  }

  @Test
  void transactionWithMultipleDataSourcesIsNotConfigured() {
    this.contextRunner.withUserConfiguration(MultiDataSourceConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean(TransactionManager.class));
  }

  @Test
  void transactionWithMultipleDataSourcesAndPrimaryCandidateIsConfigured() {
    this.contextRunner.withUserConfiguration(MultiDataSourceUsingPrimaryConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(TransactionManager.class).hasSingleBean(JdbcTransactionManager.class);
      assertThat(context.getBean(JdbcTransactionManager.class).getDataSource())
              .isSameAs(context.getBean("test1DataSource"));
    });
  }

}