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

import java.util.concurrent.CopyOnWriteArrayList;

import infra.context.ApplicationEvent;
import infra.context.ApplicationListener;
import infra.core.Ordered;
import infra.lang.Assert;

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
    Assert.notNull(callback, "SynchronizationCallback is required");
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
