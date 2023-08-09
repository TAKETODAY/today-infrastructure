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

package cn.taketoday.transaction.reactive;

import java.util.function.Function;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationEventPublisher;
import cn.taketoday.context.PayloadApplicationEvent;
import reactor.core.publisher.Mono;

/**
 * A delegate for publishing transactional events in a reactive setup.
 * Includes the current Reactor-managed {@link TransactionContext} as
 * a source object for every {@link ApplicationEvent} to be published.
 *
 * <p>This delegate is just a convenience. The current {@link TransactionContext}
 * can be directly included as the event source as well, and then published
 * through an {@link ApplicationEventPublisher} such as the Infra
 * {@link cn.taketoday.context.ApplicationContext}:
 *
 * <pre>{@code
 * TransactionContextManager.currentContext()
 *     .map(source -> new PayloadApplicationEvent&lt;&gt;(source, "myPayload"))
 *     .doOnSuccess(this.eventPublisher::publishEvent)
 * }</pre>
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #publishEvent(Function)
 * @see #publishEvent(Object)
 * @see ApplicationEventPublisher
 * @since 4.0
 */
public class TransactionalEventPublisher {

  private final ApplicationEventPublisher eventPublisher;

  /**
   * Create a new delegate for publishing transactional events in a reactive setup.
   *
   * @param eventPublisher the actual event publisher to use,
   * typically a Infra {@link cn.taketoday.context.ApplicationContext}
   */
  public TransactionalEventPublisher(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  /**
   * Publish an event created through the given function which maps the transaction
   * source object (the {@link TransactionContext}) to the event instance.
   *
   * @param eventCreationFunction a function mapping the source object to the event instance,
   * e.g. {@code source -> new PayloadApplicationEvent&lt;&gt;(source, "myPayload")}
   * @return the Reactor {@link Mono} for the transactional event publication
   */
  public Mono<Void> publishEvent(Function<TransactionContext, ApplicationEvent> eventCreationFunction) {
    return TransactionContextManager.currentContext().map(eventCreationFunction)
        .doOnSuccess(this.eventPublisher::publishEvent).then();
  }

  /**
   * Publish an event created for the given payload.
   *
   * @param payload the payload to publish as an event
   * @return the Reactor {@link Mono} for the transactional event publication
   */
  public Mono<Void> publishEvent(Object payload) {
    if (payload instanceof ApplicationEvent) {
      return Mono.error(new IllegalArgumentException("Cannot publish ApplicationEvent with transactional " +
          "source - publish payload object or use publishEvent(Function<Object, ApplicationEvent>"));
    }
    return publishEvent(source -> new PayloadApplicationEvent<>(source, payload));
  }

}
