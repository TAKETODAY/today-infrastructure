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

package infra.test.context.transaction.manager;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.transaction.AfterTransaction;
import infra.transaction.TransactionManager;
import infra.transaction.annotation.TransactionManagementConfigurer;
import infra.transaction.annotation.Transactional;
import infra.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies the behavior for transaction manager lookups
 * when only one transaction manager is configured as a bean in the application
 * context and a non-bean transaction manager is configured via the
 * {@link TransactionManagementConfigurer} API.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
@Transactional
class LookUpTxMgrViaTransactionManagementConfigurerWithSingleTxMgrBeanTests {

  @Autowired
  CallCountingTransactionManager txManager;

  @Autowired
  Config config;

  @Test
  void transactionalTest() {
    assertThat(txManager.begun).isEqualTo(0);
    assertThat(txManager.inflight).isEqualTo(0);
    assertThat(txManager.commits).isEqualTo(0);
    assertThat(txManager.rollbacks).isEqualTo(0);

    CallCountingTransactionManager annotationDriven = config.annotationDriven;
    assertThat(annotationDriven.begun).isEqualTo(1);
    assertThat(annotationDriven.inflight).isEqualTo(1);
    assertThat(annotationDriven.commits).isEqualTo(0);
    assertThat(annotationDriven.rollbacks).isEqualTo(0);
  }

  @AfterTransaction
  void afterTransaction() {
    assertThat(txManager.begun).isEqualTo(0);
    assertThat(txManager.inflight).isEqualTo(0);
    assertThat(txManager.commits).isEqualTo(0);
    assertThat(txManager.rollbacks).isEqualTo(0);

    CallCountingTransactionManager annotationDriven = config.annotationDriven;
    assertThat(annotationDriven.begun).isEqualTo(1);
    assertThat(annotationDriven.inflight).isEqualTo(0);
    assertThat(annotationDriven.commits).isEqualTo(0);
    assertThat(annotationDriven.rollbacks).isEqualTo(1);
  }

  @Configuration
  static class Config implements TransactionManagementConfigurer {

    final CallCountingTransactionManager annotationDriven = new CallCountingTransactionManager();

    @Bean
    TransactionManager txManager() {
      return new CallCountingTransactionManager();
    }

    @Override
    public TransactionManager annotationDrivenTransactionManager() {
      return annotationDriven;
    }

  }

}
