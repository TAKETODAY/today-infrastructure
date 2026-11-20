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

package infra.test.context.transaction;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import infra.app.logging.LogLevel;
import infra.app.logging.LoggingSystem;
import infra.test.context.TestContext;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.support.DefaultTransactionDefinition;
import infra.transaction.support.SimpleTransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TransactionContextTests {

  @ParameterizedTest
  @EnumSource(value = LogLevel.class, names = { "TRACE", "OFF" })
  void rollbackFlagResetsForEachTransactionAndCompletionFailureClearsStatus(LogLevel level) {
    LoggingSystem logging = LoggingSystem.get(getClass().getClassLoader());
    String name = TransactionContext.class.getName();
    LogLevel previous = logging.getLoggerConfiguration(name).getConfiguredLevel();
    logging.setLogLevel(name, level);
    try {
      PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
      var definition = new DefaultTransactionDefinition();
      var status = new SimpleTransactionStatus();
      given(manager.getTransaction(definition)).willReturn(status);
      var context = new TransactionContext(mock(TestContext.class), manager, definition, true);
      assertThatIllegalStateException().isThrownBy(() -> context.setFlaggedForRollback(false))
              .withMessageContaining("transaction does not exist");
      assertThatIllegalStateException().isThrownBy(context::endTransaction)
              .withMessageContaining("transaction does not exist");
      context.startTransaction();
      assertThatIllegalStateException().isThrownBy(context::startTransaction)
              .withMessageContaining("ending the existing transaction");
      context.setFlaggedForRollback(false);
      context.endTransaction();
      verify(manager).commit(status);
      context.startTransaction();
      assertThat(context.isFlaggedForRollback()).isTrue();
      willThrow(new IllegalStateException("rollback failed")).given(manager).rollback(status);
      assertThatIllegalStateException().isThrownBy(context::endTransaction).withMessage("rollback failed");
      assertThat(context.getTransactionStatus()).isNull();
    }
    finally {
      logging.setLogLevel(name, previous);
    }
  }

}
