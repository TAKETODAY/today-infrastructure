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

package cn.taketoday.transaction.event;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.PayloadApplicationEvent;
import cn.taketoday.transaction.support.TransactionSynchronization;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 */
public class TransactionalApplicationListenerAdapterTests {

  @Test
  public void invokesCompletionCallbackOnSuccess() {
    CapturingSynchronizationCallback callback = new CapturingSynchronizationCallback();
    PayloadApplicationEvent<Object> event = new PayloadApplicationEvent<>(this, new Object());

    TransactionalApplicationListener<PayloadApplicationEvent<Object>> adapter =
            TransactionalApplicationListener.forPayload(p -> { });
    adapter.addCallback(callback);
    runInTransaction(() -> adapter.onApplicationEvent(event));

    assertThat(callback.preEvent).isEqualTo(event);
    assertThat(callback.postEvent).isEqualTo(event);
    assertThat(callback.ex).isNull();
    assertThat(adapter.getTransactionPhase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    assertThat(adapter.getListenerId()).isEqualTo("");
  }

  @Test
  public void invokesExceptionHandlerOnException() {
    CapturingSynchronizationCallback callback = new CapturingSynchronizationCallback();
    PayloadApplicationEvent<String> event = new PayloadApplicationEvent<>(this, "event");
    RuntimeException ex = new RuntimeException("event");

    TransactionalApplicationListener<PayloadApplicationEvent<String>> adapter =
            TransactionalApplicationListener.forPayload(
                    TransactionPhase.BEFORE_COMMIT, p -> { throw ex; });
    adapter.addCallback(callback);

    assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> runInTransaction(() -> adapter.onApplicationEvent(event)))
            .withMessage("event");

    assertThat(callback.preEvent).isEqualTo(event);
    assertThat(callback.postEvent).isEqualTo(event);
    assertThat(callback.ex).isEqualTo(ex);
    assertThat(adapter.getTransactionPhase()).isEqualTo(TransactionPhase.BEFORE_COMMIT);
    assertThat(adapter.getListenerId()).isEqualTo("");
  }

  @Test
  public void useSpecifiedIdentifier() {
    CapturingSynchronizationCallback callback = new CapturingSynchronizationCallback();
    PayloadApplicationEvent<String> event = new PayloadApplicationEvent<>(this, "event");

    TransactionalApplicationListenerAdapter<PayloadApplicationEvent<String>> adapter =
            new TransactionalApplicationListenerAdapter<>(e -> { });
    adapter.setTransactionPhase(TransactionPhase.BEFORE_COMMIT);
    adapter.setListenerId("identifier");
    adapter.addCallback(callback);
    runInTransaction(() -> adapter.onApplicationEvent(event));

    assertThat(callback.preEvent).isEqualTo(event);
    assertThat(callback.postEvent).isEqualTo(event);
    assertThat(callback.ex).isNull();
    assertThat(adapter.getTransactionPhase()).isEqualTo(TransactionPhase.BEFORE_COMMIT);
    assertThat(adapter.getListenerId()).isEqualTo("identifier");
  }

  private static void runInTransaction(Runnable runnable) {
    TransactionSynchronizationManager.setActualTransactionActive(true);
    TransactionSynchronizationManager.initSynchronization();
    try {
      runnable.run();
      TransactionSynchronizationManager.getSynchronizations().forEach(it -> {
        it.beforeCommit(false);
        it.afterCommit();
        it.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
      });
    }
    finally {
      TransactionSynchronizationManager.clearSynchronization();
      TransactionSynchronizationManager.setActualTransactionActive(false);
    }
  }

}
