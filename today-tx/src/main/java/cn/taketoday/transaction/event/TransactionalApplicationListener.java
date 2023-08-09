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

import java.util.function.Consumer;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.PayloadApplicationEvent;
import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.PlatformTransactionManager;

/**
 * An {@link ApplicationListener} that is invoked according to a {@link TransactionPhase}.
 * This is a programmatic equivalent of the {@link TransactionalEventListener} annotation.
 *
 * <p>Adding {@link cn.taketoday.core.Ordered} to your listener implementation
 * allows you to prioritize that listener amongst other listeners running before or after
 * transaction completion.
 *
 * <p><b>NOTE: Transactional event listeners only work with thread-bound transactions
 * managed by a {@link PlatformTransactionManager
 * PlatformTransactionManager}.</b> A reactive transaction managed by a
 * {@link cn.taketoday.transaction.ReactiveTransactionManager ReactiveTransactionManager}
 * uses the Reactor context instead of thread-local variables, so from the perspective of
 * an event listener, there is no compatible active transaction that it can participate in.
 *
 * @param <E> the specific {@code ApplicationEvent} subclass to listen to
 * @author Juergen Hoeller
 * @author Oliver Drotbohm
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TransactionalEventListener
 * @see TransactionalApplicationListenerAdapter
 * @see #forPayload
 * @since 4.0
 */
public interface TransactionalApplicationListener<E extends ApplicationEvent>
    extends ApplicationListener<E>, Ordered {

  /**
   * Return the execution order within transaction synchronizations.
   * <p>Default is {@link Ordered#LOWEST_PRECEDENCE}.
   *
   * @see cn.taketoday.transaction.support.TransactionSynchronization#getOrder()
   */
  @Override
  default int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

  /**
   * Transaction-synchronized listeners do not support asynchronous execution,
   * only their target listener ({@link #processEvent}) potentially does.
   */
  @Override
  default boolean supportsAsyncExecution() {
    return false;
  }

  /**
   * Return an identifier for the listener to be able to refer to it individually.
   * <p>It might be necessary for specific completion callback implementations
   * to provide a specific id, whereas for other scenarios an empty String
   * (as the common default value) is acceptable as well.
   *
   * @see cn.taketoday.context.event.SmartApplicationListener#getListenerId()
   * @see TransactionalEventListener#id
   * @see #addCallback
   */
  default String getListenerId() {
    return "";
  }

  /**
   * Return the {@link TransactionPhase} in which the listener will be invoked.
   * <p>The default phase is {@link TransactionPhase#AFTER_COMMIT}.
   */
  default TransactionPhase getTransactionPhase() {
    return TransactionPhase.AFTER_COMMIT;
  }

  /**
   * Add a callback to be invoked on processing within transaction synchronization,
   * i.e. when {@link #processEvent} is being triggered during actual transactions.
   *
   * @param callback the synchronization callback to apply
   */
  void addCallback(SynchronizationCallback callback);

  /**
   * Immediately process the given {@link Object}. In contrast to
   * {@link #onApplicationEvent(ApplicationEvent)}, a call to this method will
   * directly process the given event without deferring it to the associated
   * {@link #getTransactionPhase() transaction phase}.
   *
   * @param event the event to process through the target listener implementation
   */
  void processEvent(E event);

  /**
   * Create a new {@code TransactionalApplicationListener} for the given payload consumer,
   * to be applied in the default phase {@link TransactionPhase#AFTER_COMMIT}.
   *
   * @param consumer the event payload consumer
   * @param <T> the type of the event payload
   * @return a corresponding {@code TransactionalApplicationListener} instance
   * @see PayloadApplicationEvent#getPayload()
   * @see TransactionalApplicationListenerAdapter
   */
  static <T> TransactionalApplicationListener<PayloadApplicationEvent<T>> forPayload(Consumer<T> consumer) {
    return forPayload(TransactionPhase.AFTER_COMMIT, consumer);
  }

  /**
   * Create a new {@code TransactionalApplicationListener} for the given payload consumer.
   *
   * @param phase the transaction phase in which to invoke the listener
   * @param consumer the event payload consumer
   * @param <T> the type of the event payload
   * @return a corresponding {@code TransactionalApplicationListener} instance
   * @see PayloadApplicationEvent#getPayload()
   * @see TransactionalApplicationListenerAdapter
   */
  static <T> TransactionalApplicationListener<PayloadApplicationEvent<T>> forPayload(
      TransactionPhase phase, Consumer<T> consumer) {

    TransactionalApplicationListenerAdapter<PayloadApplicationEvent<T>> listener =
        new TransactionalApplicationListenerAdapter<>(event -> consumer.accept(event.getPayload()));
    listener.setTransactionPhase(phase);
    return listener;
  }

  /**
   * Callback to be invoked on synchronization-driven event processing,
   * wrapping the target listener invocation ({@link #processEvent}).
   *
   * @see #addCallback
   * @see #processEvent
   */
  interface SynchronizationCallback {

    /**
     * Called before transactional event listener invocation.
     *
     * @param event the event that transaction synchronization is about to process
     */
    default void preProcessEvent(ApplicationEvent event) { }

    /**
     * Called after a transactional event listener invocation.
     *
     * @param event the event that transaction synchronization finished processing
     * @param ex an exception that occurred during listener invocation, if any
     */
    default void postProcessEvent(ApplicationEvent event, @Nullable Throwable ex) { }
  }

}
