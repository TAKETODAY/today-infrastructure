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

package infra.transaction.event;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import infra.context.ApplicationEvent;
import infra.context.PayloadApplicationEvent;
import infra.core.Ordered;
import infra.transaction.event.TransactionalApplicationListener.SynchronizationCallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 21:50
 */
class TransactionalApplicationListenerTests {

  @Test
  void defaultGetOrderReturnsLowestPrecedence() {
    TransactionalApplicationListener<ApplicationEvent> listener = new TestTransactionalApplicationListener();
    assertThat(listener.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
  }

  @Test
  void defaultSupportsAsyncExecutionReturnsFalse() {
    TransactionalApplicationListener<ApplicationEvent> listener = new TestTransactionalApplicationListener();
    assertThat(listener.supportsAsyncExecution()).isFalse();
  }

  @Test
  void defaultGetListenerIdReturnsEmptyString() {
    TransactionalApplicationListener<ApplicationEvent> listener = new TestTransactionalApplicationListener();
    assertThat(listener.getListenerId()).isEqualTo("");
  }

  @Test
  void defaultGetTransactionPhaseReturnsAfterCommit() {
    TransactionalApplicationListener<ApplicationEvent> listener = new TestTransactionalApplicationListener();
    assertThat(listener.getTransactionPhase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
  }

  @Test
  void forPayloadWithConsumerCreatesListener() {
    boolean[] called = { false };
    Consumer<String> consumer = s -> called[0] = true;

    TransactionalApplicationListener<PayloadApplicationEvent<String>> listener =
            TransactionalApplicationListener.forPayload(consumer);

    assertThat(listener).isNotNull();
    assertThat(listener.getTransactionPhase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
  }

  @Test
  void forPayloadWithPhaseAndConsumerCreatesListener() {
    boolean[] called = { false };
    Consumer<String> consumer = s -> called[0] = true;

    TransactionalApplicationListener<PayloadApplicationEvent<String>> listener =
            TransactionalApplicationListener.forPayload(TransactionPhase.AFTER_ROLLBACK, consumer);

    assertThat(listener).isNotNull();
    assertThat(listener.getTransactionPhase()).isEqualTo(TransactionPhase.AFTER_ROLLBACK);
  }

  @Test
  void synchronizationCallbackDefaultMethodsDoNotThrowExceptions() {
    SynchronizationCallback callback = new SynchronizationCallback() { };
    ApplicationEvent event = new ApplicationEvent("test") { };

    assertThatCode(() -> callback.preProcessEvent(event)).doesNotThrowAnyException();
    assertThatCode(() -> callback.postProcessEvent(event, null)).doesNotThrowAnyException();
  }

  @Test
  void synchronizationCallbackPreProcessEventWithException() {
    boolean[] called = { false };
    SynchronizationCallback callback = new SynchronizationCallback() {
      @Override
      public void preProcessEvent(ApplicationEvent event) {
        called[0] = true;
      }
    };
    ApplicationEvent event = new ApplicationEvent("test") { };

    callback.preProcessEvent(event);

    assertThat(called[0]).isTrue();
  }

  @Test
  void synchronizationCallbackPostProcessEventWithException() {
    boolean[] called = { false };
    Exception testException = new RuntimeException("test");
    SynchronizationCallback callback = new SynchronizationCallback() {
      @Override
      public void postProcessEvent(ApplicationEvent event, Throwable ex) {
        called[0] = true;
        assertThat(ex).isEqualTo(testException);
      }
    };
    ApplicationEvent event = new ApplicationEvent("test") { };

    callback.postProcessEvent(event, testException);

    assertThat(called[0]).isTrue();
  }

  static class TestTransactionalApplicationListener implements TransactionalApplicationListener<ApplicationEvent> {
    @Override
    public void addCallback(SynchronizationCallback callback) {
    }

    @Override
    public void processEvent(ApplicationEvent event) {
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
    }
  }

}