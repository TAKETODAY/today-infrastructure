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

package infra.transaction.support;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/17 22:34
 */
class SmartTransactionObjectTests {

  @Test
  void shouldReturnFalseForRollbackOnlyByDefault() {
    SmartTransactionObject transactionObject = new SmartTransactionObject() { };

    assertThat(transactionObject.isRollbackOnly()).isFalse();
  }

  @Test
  void shouldReturnTrueWhenRollbackOnlyIsOverridden() {
    SmartTransactionObject transactionObject = new SmartTransactionObject() {
      @Override
      public boolean isRollbackOnly() {
        return true;
      }
    };

    assertThat(transactionObject.isRollbackOnly()).isTrue();
  }

  @Test
  void shouldNotThrowExceptionWhenFlushingByDefault() {
    SmartTransactionObject transactionObject = new SmartTransactionObject() { };

    assertThatNoException().isThrownBy(transactionObject::flush);
  }

  @Test
  void shouldExecuteCustomFlushImplementation() {
    AtomicBoolean flushed = new AtomicBoolean(false);

    SmartTransactionObject transactionObject = new SmartTransactionObject() {
      @Override
      public void flush() {
        flushed.set(true);
      }
    };

    transactionObject.flush();
    assertThat(flushed.get()).isTrue();
  }

  @Test
  void shouldCombineCustomRollbackOnlyAndFlushBehaviors() {
    AtomicBoolean flushed = new AtomicBoolean(false);

    SmartTransactionObject transactionObject = new SmartTransactionObject() {
      @Override
      public boolean isRollbackOnly() {
        return true;
      }

      @Override
      public void flush() {
        flushed.set(true);
      }
    };

    assertThat(transactionObject.isRollbackOnly()).isTrue();

    transactionObject.flush();
    assertThat(flushed.get()).isTrue();
  }

}