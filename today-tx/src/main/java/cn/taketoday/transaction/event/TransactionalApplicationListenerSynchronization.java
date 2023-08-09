/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.util.List;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.core.Ordered;
import cn.taketoday.transaction.reactive.TransactionContext;
import cn.taketoday.transaction.support.SynchronizationInfo;
import cn.taketoday.transaction.support.TransactionSynchronization;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
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
      cn.taketoday.transaction.reactive.TransactionSynchronizationManager rtsm =
          new cn.taketoday.transaction.reactive.TransactionSynchronizationManager(txContext);
      if (rtsm.isSynchronizationActive() && rtsm.isActualTransactionActive()) {
        rtsm.registerSynchronization(new ReactiveSynchronization<>(event, listener, callbacks));
        return true;
      }
    }
    return false;
  }

  private static class PlatformSynchronization<AE extends ApplicationEvent>
      extends TransactionalApplicationListenerSynchronization<AE>
      implements cn.taketoday.transaction.support.TransactionSynchronization {

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
      implements cn.taketoday.transaction.reactive.TransactionSynchronization {

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
