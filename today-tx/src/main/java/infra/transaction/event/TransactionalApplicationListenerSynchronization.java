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

package infra.transaction.event;

import java.util.List;

import infra.context.ApplicationEvent;
import infra.core.Ordered;
import infra.transaction.reactive.TransactionContext;
import infra.transaction.support.SynchronizationInfo;
import infra.transaction.support.TransactionSynchronization;
import infra.transaction.support.TransactionSynchronizationManager;
import reactor.core.publisher.Mono;

/**
 * {@link TransactionSynchronization} implementation for event processing with a
 * {@link TransactionalApplicationListener}.
 *
 * @param <E> the specific {@code ApplicationEvent} subclass to listen to
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class TransactionalApplicationListenerSynchronization<E extends ApplicationEvent> implements Ordered {

  private final E event;

  private final TransactionalApplicationListener<E> listener;

  private final List<TransactionalApplicationListener.SynchronizationCallback> callbacks;

  public TransactionalApplicationListenerSynchronization(E event, TransactionalApplicationListener<E> listener,
      List<TransactionalApplicationListener.SynchronizationCallback> callbacks) {

    this.event = event;
    this.listener = listener;
    this.callbacks = callbacks;
  }

  @Override
  public int getOrder() {
    return this.listener.getOrder();
  }

  public TransactionPhase getTransactionPhase() {
    return this.listener.getTransactionPhase();
  }

  public void processEventWithCallbacks() {
    this.callbacks.forEach(callback -> callback.preProcessEvent(this.event));
    try {
      this.listener.processEvent(this.event);
    }
    catch (RuntimeException | Error ex) {
      this.callbacks.forEach(callback -> callback.postProcessEvent(this.event, ex));
      throw ex;
    }
    this.callbacks.forEach(callback -> callback.postProcessEvent(this.event, null));
  }

  public static <E extends ApplicationEvent> boolean register(
      E event, TransactionalApplicationListener<E> listener,
      List<TransactionalApplicationListener.SynchronizationCallback> callbacks) {

    SynchronizationInfo info = TransactionSynchronizationManager.getSynchronizationInfo();
    if (info.isSynchronizationActive() && info.isActualTransactionActive()) {
      info.registerSynchronization(new PlatformSynchronization<>(event, listener, callbacks));
      return true;
    }
    else if (event.getSource() instanceof TransactionContext txContext) {
      infra.transaction.reactive.TransactionSynchronizationManager rtsm =
          new infra.transaction.reactive.TransactionSynchronizationManager(txContext);
      if (rtsm.isSynchronizationActive() && rtsm.isActualTransactionActive()) {
        rtsm.registerSynchronization(new ReactiveSynchronization<>(event, listener, callbacks));
        return true;
      }
    }
    return false;
  }

  private static class PlatformSynchronization<AE extends ApplicationEvent>
      extends TransactionalApplicationListenerSynchronization<AE>
      implements infra.transaction.support.TransactionSynchronization {

    public PlatformSynchronization(AE event, TransactionalApplicationListener<AE> listener,
        List<TransactionalApplicationListener.SynchronizationCallback> callbacks) {

      super(event, listener, callbacks);
    }

    @Override
    public void beforeCommit(boolean readOnly) {
      if (getTransactionPhase() == TransactionPhase.BEFORE_COMMIT) {
        processEventWithCallbacks();
      }
    }

    @Override
    public void afterCompletion(int status) {
      TransactionPhase phase = getTransactionPhase();
      if (phase == TransactionPhase.AFTER_COMMIT && status == STATUS_COMMITTED) {
        processEventWithCallbacks();
      }
      else if (phase == TransactionPhase.AFTER_ROLLBACK && status == STATUS_ROLLED_BACK) {
        processEventWithCallbacks();
      }
      else if (phase == TransactionPhase.AFTER_COMPLETION) {
        processEventWithCallbacks();
      }
    }
  }

  private static class ReactiveSynchronization<AE extends ApplicationEvent>
      extends TransactionalApplicationListenerSynchronization<AE>
      implements infra.transaction.reactive.TransactionSynchronization {

    public ReactiveSynchronization(AE event, TransactionalApplicationListener<AE> listener,
        List<TransactionalApplicationListener.SynchronizationCallback> callbacks) {

      super(event, listener, callbacks);
    }

    @Override
    public Mono<Void> beforeCommit(boolean readOnly) {
      if (getTransactionPhase() == TransactionPhase.BEFORE_COMMIT) {
        return Mono.fromRunnable(this::processEventWithCallbacks);
      }
      return Mono.empty();
    }

    @Override
    public Mono<Void> afterCompletion(int status) {
      TransactionPhase phase = getTransactionPhase();
      if (phase == TransactionPhase.AFTER_COMMIT && status == STATUS_COMMITTED) {
        return Mono.fromRunnable(this::processEventWithCallbacks);
      }
      else if (phase == TransactionPhase.AFTER_ROLLBACK && status == STATUS_ROLLED_BACK) {
        return Mono.fromRunnable(this::processEventWithCallbacks);
      }
      else if (phase == TransactionPhase.AFTER_COMPLETION) {
        return Mono.fromRunnable(this::processEventWithCallbacks);
      }
      return Mono.empty();
    }
  }

}
