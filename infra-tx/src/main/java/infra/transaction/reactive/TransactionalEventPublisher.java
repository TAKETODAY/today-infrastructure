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

package infra.transaction.reactive;

import java.util.function.Function;

import infra.context.ApplicationEvent;
import infra.context.ApplicationEventPublisher;
import infra.context.PayloadApplicationEvent;
import reactor.core.publisher.Mono;

/**
 * A delegate for publishing transactional events in a reactive setup.
 * Includes the current Reactor-managed {@link TransactionContext} as
 * a source object for every {@link ApplicationEvent} to be published.
 *
 * <p>This delegate is just a convenience. The current {@link TransactionContext}
 * can be directly included as the event source as well, and then published
 * through an {@link ApplicationEventPublisher} such as the Infra
 * {@link infra.context.ApplicationContext}:
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
   * typically a Infra {@link infra.context.ApplicationContext}
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
