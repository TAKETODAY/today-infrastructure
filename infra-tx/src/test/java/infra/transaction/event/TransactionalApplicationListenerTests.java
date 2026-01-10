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