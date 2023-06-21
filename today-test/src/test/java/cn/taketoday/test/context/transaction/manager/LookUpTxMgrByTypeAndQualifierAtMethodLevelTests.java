/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.context.transaction.manager;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;
import cn.taketoday.test.context.transaction.AfterTransaction;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.annotation.Transactional;
import cn.taketoday.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify the behavior requested in
 * <a href="https://jira.spring.io/browse/SPR-9645">SPR-9645</a>.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
class LookUpTxMgrByTypeAndQualifierAtMethodLevelTests {

  @Autowired
  CallCountingTransactionManager txManager1;

  @Autowired
  CallCountingTransactionManager txManager2;

  @Transactional("txManager1")
  @Test
  void transactionalTest() {
    assertThat(txManager1.begun).isEqualTo(1);
    assertThat(txManager1.inflight).isEqualTo(1);
    assertThat(txManager1.commits).isEqualTo(0);
    assertThat(txManager1.rollbacks).isEqualTo(0);

    assertThat(txManager2.begun).isEqualTo(0);
    assertThat(txManager2.inflight).isEqualTo(0);
    assertThat(txManager2.commits).isEqualTo(0);
    assertThat(txManager2.rollbacks).isEqualTo(0);
  }

  @AfterTransaction
  void afterTransaction() {
    assertThat(txManager1.begun).isEqualTo(1);
    assertThat(txManager1.inflight).isEqualTo(0);
    assertThat(txManager1.commits).isEqualTo(0);
    assertThat(txManager1.rollbacks).isEqualTo(1);

    assertThat(txManager2.begun).isEqualTo(0);
    assertThat(txManager2.inflight).isEqualTo(0);
    assertThat(txManager2.commits).isEqualTo(0);
    assertThat(txManager2.rollbacks).isEqualTo(0);
  }

  @Configuration
  static class Config {

    @Bean
    PlatformTransactionManager txManager1() {
      return new CallCountingTransactionManager();
    }

    @Bean
    PlatformTransactionManager txManager2() {
      return new CallCountingTransactionManager();
    }

  }

}
