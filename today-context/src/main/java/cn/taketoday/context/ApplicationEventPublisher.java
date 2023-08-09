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
package cn.taketoday.context;

/**
 * Interface that encapsulates event publication functionality.
 *
 * <p>Serves as a super-interface for {@link ApplicationContext}.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ApplicationContext
 * @see ApplicationEventPublisherAware
 * @see ApplicationEvent
 * @see cn.taketoday.context.event.ApplicationEventMulticaster
 * @see cn.taketoday.context.event.EventPublicationInterceptor
 * @since 2018-09-09 21:26
 */
public interface ApplicationEventPublisher {

  /**
   * Notify all <strong>matching</strong> listeners registered with this
   * application of an application event. Events may be framework events
   * (such as ContextRefreshedEvent) or application-specific events.
   * <p>Such an event publication step is effectively a hand-off to the
   * multicaster and does not imply synchronous/asynchronous execution
   * or even immediate execution at all. Event listeners are encouraged
   * to be as efficient as possible, individually using asynchronous
   * execution for longer-running and potentially blocking operations.
   * <p>For usage in a reactive call stack, include event publication
   * as a simple hand-off:
   * {@code Mono.fromRunnable(() -> eventPublisher.publishEvent(...))}.
   * As with any asynchronous execution, thread-local data is not going
   * to be available for reactive listener methods. All state which is
   * necessary to process the event needs to be included in the event
   * instance itself.
   * <p>For the convenient inclusion of the current transaction context
   * in a reactive hand-off, consider using
   * {@link cn.taketoday.transaction.reactive.TransactionalEventPublisher#publishEvent(Function)}.
   * For thread-bound transactions, this is not necessary since the
   * state will be implicitly available through thread-local storage.
   *
   * @param event the event to publish
   * @see #publishEvent(Object)
   * @see ApplicationListener#supportsAsyncExecution()
   * @see cn.taketoday.context.event.ContextRefreshedEvent
   * @see cn.taketoday.context.event.ContextClosedEvent
   */
  default void publishEvent(ApplicationEvent event) {
    publishEvent((Object) event);
  }

  /**
   * Notify all <strong>matching</strong> listeners registered with this
   * application of an event.
   * <p>If the specified {@code event} is not an {@link ApplicationEvent},
   * it is wrapped in a {@link PayloadApplicationEvent}.
   * <p>Such an event publication step is effectively a hand-off to the
   * multicaster and does not imply synchronous/asynchronous execution
   * or even immediate execution at all. Event listeners are encouraged
   * to be as efficient as possible, individually using asynchronous
   * execution for longer-running and potentially blocking operations.
   * <p>For the convenient inclusion of the current transaction context
   * in a reactive hand-off, consider using
   * {@link cn.taketoday.transaction.reactive.TransactionalEventPublisher#publishEvent(Object)}.
   * For thread-bound transactions, this is not necessary since the
   * state will be implicitly available through thread-local storage.
   *
   * @param event the event to publish
   * @see #publishEvent(ApplicationEvent)
   * @see PayloadApplicationEvent
   */
  void publishEvent(Object event);

}
