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

import java.util.concurrent.CopyOnWriteArrayList;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Assert;

/**
 * {@link TransactionalApplicationListener} adapter that delegates the processing of
 * an event to a target {@link ApplicationListener} instance. Supports the exact
 * same features as any regular {@link ApplicationListener} but is aware of the
 * transactional context of the event publisher.
 *
 * @param <E> the specific {@code ApplicationEvent} subclass to listen to
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TransactionalApplicationListener
 * @see TransactionalEventListener
 * @see TransactionalApplicationListenerMethodAdapter
 * @since 4.0
 */
public class TransactionalApplicationListenerAdapter<E extends ApplicationEvent>
    implements TransactionalApplicationListener<E>, Ordered {

  private String listenerId = "";

  private int order = Ordered.LOWEST_PRECEDENCE;

  private final ApplicationListener<E> targetListener;

  private TransactionPhase transactionPhase = TransactionPhase.AFTER_COMMIT;

  private final CopyOnWriteArrayList<SynchronizationCallback> callbacks = new CopyOnWriteArrayList<>();

  /**
   * Construct a new TransactionalApplicationListenerAdapter.
   *
   * @param targetListener the actual listener to invoke in the specified transaction phase
   * @see #setTransactionPhase
   * @see TransactionalApplicationListener#forPayload
   */
  public TransactionalApplicationListenerAdapter(ApplicationListener<E> targetListener) {
    this.targetListener = targetListener;
  }

  /**
   * Specify the synchronization order for the listener.
   */
  public void setOrder(int order) {
    this.order = order;
  }

  /**
   * Return the synchronization order for the listener.
   */
  @Override
  public int getOrder() {
    return this.order;
  }

  /**
   * Specify the transaction phase to invoke the listener in.
   * <p>The default is {@link TransactionPhase#AFTER_COMMIT}.
   */
  public void setTransactionPhase(TransactionPhase transactionPhase) {
    this.transactionPhase = transactionPhase;
  }

  /**
   * Return the transaction phase to invoke the listener in.
   */
  @Override
  public TransactionPhase getTransactionPhase() {
    return this.transactionPhase;
  }

  /**
   * Specify an id to identify the listener with.
   * <p>The default is an empty String.
   */
  public void setListenerId(String listenerId) {
    this.listenerId = listenerId;
  }

  /**
   * Return an id to identify the listener with.
   */
  @Override
  public String getListenerId() {
    return this.listenerId;
  }

  @Override
  public void addCallback(SynchronizationCallback callback) {
    Assert.notNull(callback, "SynchronizationCallback must not be null");
    this.callbacks.add(callback);
  }

  @Override
  public void processEvent(E event) {
    this.targetListener.onApplicationEvent(event);
  }

  @Override
  public void onApplicationEvent(E event) {
    TransactionalApplicationListenerSynchronization.register(event, this, this.callbacks);
  }

}
