/*
 * Copyright 2017 - 2025 the original author or authors.
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