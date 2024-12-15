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
import infra.transaction.PlatformTransactionManager;
import infra.transaction.annotation.Transactional;
import infra.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify the behavior requested in
 *
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
@Transactional
class LookUpTxMgrByTypeAndDefaultNameTests {

  @Autowired
  CallCountingTransactionManager transactionManager;

  @Autowired
  CallCountingTransactionManager txManager2;

  @Test
  void transactionalTest() {
    assertThat(transactionManager.begun).isEqualTo(1);
    assertThat(transactionManager.inflight).isEqualTo(1);
    assertThat(transactionManager.commits).isEqualTo(0);
    assertThat(transactionManager.rollbacks).isEqualTo(0);

    assertThat(txManager2.begun).isEqualTo(0);
    assertThat(txManager2.inflight).isEqualTo(0);
    assertThat(txManager2.commits).isEqualTo(0);
    assertThat(txManager2.rollbacks).isEqualTo(0);
  }

  @AfterTransaction
  void afterTransaction() {
    assertThat(transactionManager.begun).isEqualTo(1);
    assertThat(transactionManager.inflight).isEqualTo(0);
    assertThat(transactionManager.commits).isEqualTo(0);
    assertThat(transactionManager.rollbacks).isEqualTo(1);

    assertThat(txManager2.begun).isEqualTo(0);
    assertThat(txManager2.inflight).isEqualTo(0);
    assertThat(txManager2.commits).isEqualTo(0);
    assertThat(txManager2.rollbacks).isEqualTo(0);
  }

  @Configuration
  static class Config {

    @Bean
    PlatformTransactionManager transactionManager() {
      return new CallCountingTransactionManager();
    }

    @Bean
    PlatformTransactionManager txManager2() {
      return new CallCountingTransactionManager();
    }

  }

}
