/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.test.context.junit.jupiter.transaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sql.DataSource;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.jdbc.datasource.DataSourceTransactionManager;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import infra.test.context.junit.jupiter.FailingTestCase;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.junit4.TimedTransactionalRunnerTests;
import infra.test.transaction.TransactionAssert;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.annotation.Propagation;
import infra.transaction.annotation.Transactional;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

/**
 * JUnit Jupiter based integration tests which verify support for Infra
 * {@link Transactional @Transactional} annotation in conjunction with JUnit
 * Jupiter's {@link Timeout @Timeout}.
 *
 * @author Sam Brannen
 * @see TimedTransactionalRunnerTests
 * @since 4.0
 */
class TimedTransactionalExtensionTests {

  @Test
  void infraTransactionsWorkWithJUnitJupiterTimeouts() {
    Events events = EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(TestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(4).succeeded(2).failed(2));

    events.failed().assertThatEvents().haveExactly(2,
            event(test("WithExceededJUnitJupiterTimeout"),
                    finishedWithFailure(
                            instanceOf(TimeoutException.class),
                            message(msg -> msg.endsWith("timed out after 10 milliseconds")))));
  }

  @JUnitConfig
  @Transactional
  @FailingTestCase
  static class TestCase {

    @Test
    @Timeout(1)
    void transactionalWithJUnitJupiterTimeout() {
      TransactionAssert.assertThatTransaction().isActive();
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.MILLISECONDS)
    void transactionalWithExceededJUnitJupiterTimeout() throws Exception {
      TransactionAssert.assertThatTransaction().isActive();
      Thread.sleep(200);
    }

    @Test
    @Timeout(1)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void notTransactionalWithJUnitJupiterTimeout() {
      TransactionAssert.assertThatTransaction().isNotActive();
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.MILLISECONDS)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void notTransactionalWithExceededJUnitJupiterTimeout() throws Exception {
      TransactionAssert.assertThatTransaction().isNotActive();
      Thread.sleep(200);
    }

    @Configuration
    static class Config {

      @Bean
      PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
      }

      @Bean
      DataSource dataSource() {
        return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
      }
    }

  }

}
