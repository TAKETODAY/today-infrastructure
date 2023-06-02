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

package cn.taketoday.annotation.config.jdbc;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import javax.sql.DataSource;

import cn.taketoday.annotation.config.transaction.TransactionAutoConfiguration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.jdbc.datasource.DataSourceTransactionManager;
import cn.taketoday.jdbc.support.JdbcTransactionManager;
import cn.taketoday.transaction.TransactionManager;

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
    // gh-24321
  void transactionManagerWithDaoExceptionTranslationDisabled() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .withPropertyValues("dao.exceptiontranslation.enabled=false")
            .run((context) -> assertThat(context.getBean(TransactionManager.class))
                    .isExactlyInstanceOf(DataSourceTransactionManager.class));
  }

  @Test
    // gh-24321
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