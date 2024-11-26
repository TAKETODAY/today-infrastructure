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
 * <a href="https://jira.spring.io/browse/SPR-9645">SPR-9645</a>.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
@Transactional
class LookUpTxMgrByTypeTests {

  @Autowired
  CallCountingTransactionManager txManager;

  @Test
  void transactionalTest() {
    assertThat(txManager.begun).isEqualTo(1);
    assertThat(txManager.inflight).isEqualTo(1);
    assertThat(txManager.commits).isEqualTo(0);
    assertThat(txManager.rollbacks).isEqualTo(0);
  }

  @AfterTransaction
  void afterTransaction() {
    assertThat(txManager.begun).isEqualTo(1);
    assertThat(txManager.inflight).isEqualTo(0);
    assertThat(txManager.commits).isEqualTo(0);
    assertThat(txManager.rollbacks).isEqualTo(1);
  }

  @Configuration
  static class Config {

    @Bean
    PlatformTransactionManager txManager() {
      return new CallCountingTransactionManager();
    }

  }

}
