/*
 * Copyright 2002-present the original author or authors.
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

package infra.test.context.transaction.manager;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Qualifier;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Primary;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.transaction.AfterTransaction;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.TransactionManager;
import infra.transaction.annotation.TransactionManagementConfigurer;
import infra.transaction.annotation.Transactional;
import infra.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies the behavior for transaction manager lookups
 * when one transaction manager is {@link Primary @Primary} and an additional
 * transaction manager is configured via the
 * {@link TransactionManagementConfigurer} API.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
@Transactional
class LookUpTxMgrViaTransactionManagementConfigurerWithPrimaryTxMgrTests {

  @Autowired
  CallCountingTransactionManager primary;

  @Autowired
  @Qualifier("annotationDrivenTransactionManager")
  CallCountingTransactionManager annotationDriven;

  @Test
  void transactionalTest() {
    assertThat(primary.begun).isEqualTo(0);
    assertThat(primary.inflight).isEqualTo(0);
    assertThat(primary.commits).isEqualTo(0);
    assertThat(primary.rollbacks).isEqualTo(0);

    assertThat(annotationDriven.begun).isEqualTo(1);
    assertThat(annotationDriven.inflight).isEqualTo(1);
    assertThat(annotationDriven.commits).isEqualTo(0);
    assertThat(annotationDriven.rollbacks).isEqualTo(0);
  }

  @AfterTransaction
  void afterTransaction() {
    assertThat(primary.begun).isEqualTo(0);
    assertThat(primary.inflight).isEqualTo(0);
    assertThat(primary.commits).isEqualTo(0);
    assertThat(primary.rollbacks).isEqualTo(0);

    assertThat(annotationDriven.begun).isEqualTo(1);
    assertThat(annotationDriven.inflight).isEqualTo(0);
    assertThat(annotationDriven.commits).isEqualTo(0);
    assertThat(annotationDriven.rollbacks).isEqualTo(1);
  }

  @Configuration
  static class Config implements TransactionManagementConfigurer {

    @Bean
    @Primary
    PlatformTransactionManager primary() {
      return new CallCountingTransactionManager();
    }

    @Bean
    @Override
    public TransactionManager annotationDrivenTransactionManager() {
      return new CallCountingTransactionManager();
    }

  }

}
