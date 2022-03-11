/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import java.util.List;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.transaction.support.TransactionSynchronization;

/**
 * {@link TransactionSynchronization} implementation for event processing with a
 * {@link TransactionalApplicationListener}.
 *
 * @param <E> the specific {@code ApplicationEvent} subclass to listen to
 * @author Juergen Hoeller
 * @since 4.0
 */
class TransactionalApplicationListenerSynchronization<E extends ApplicationEvent>
        implements TransactionSynchronization {

  private final E event;

  private final TransactionalApplicationListener<E> listener;

  private final List<TransactionalApplicationListener.SynchronizationCallback> callbacks;

  public TransactionalApplicationListenerSynchronization(
          E event, TransactionalApplicationListener<E> listener,
          List<TransactionalApplicationListener.SynchronizationCallback> callbacks) {

    this.event = event;
    this.listener = listener;
    this.callbacks = callbacks;
  }

  @Override
  public int getOrder() {
    return this.listener.getOrder();
  }

  @Override
  public void beforeCommit(boolean readOnly) {
    if (this.listener.getTransactionPhase() == TransactionPhase.BEFORE_COMMIT) {
      processEventWithCallbacks();
    }
  }

  @Override
  public void afterCompletion(int status) {
    TransactionPhase phase = this.listener.getTransactionPhase();
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

  private void processEventWithCallbacks() {
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

}
