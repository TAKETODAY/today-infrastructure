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

package infra.context.event;

import java.util.function.Consumer;

import infra.context.ApplicationEvent;
import infra.context.ApplicationListener;
import infra.core.ResolvableType;

/**
 * Extended variant of the standard {@link ApplicationListener} interface,
 * exposing further metadata such as the supported event and source type.
 *
 * <p>this interface supersedes the Class-based
 * {@link SmartApplicationListener} with full handling of generic event types.
 * it formally extends {@link SmartApplicationListener}, adapting
 * {@link #supportsEventType(Class)} to {@link #supportsEventType(ResolvableType)}
 * with a default method.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SmartApplicationListener
 * @see GenericApplicationListenerAdapter
 * @since 4.0
 */
public interface GenericApplicationListener extends SmartApplicationListener {

  /**
   * Overrides {@link SmartApplicationListener#supportsEventType(Class)} with
   * delegation to {@link #supportsEventType(ResolvableType)}.
   */
  @Override
  default boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
    return supportsEventType(ResolvableType.forClass(eventType));
  }

  /**
   * Determine whether this listener actually supports the given event type.
   *
   * @param eventType the event type (never {@code null})
   */
  boolean supportsEventType(ResolvableType eventType);

  /**
   * Create a new {@code ApplicationListener} for the given event type.
   *
   * @param eventType the event to listen to
   * @param consumer the consumer to invoke when a matching event is fired
   * @param <E> the specific {@code ApplicationEvent} subclass to listen to
   * @return a corresponding {@code ApplicationListener} instance
   */
  static <E extends ApplicationEvent> GenericApplicationListener forEventType(Class<E> eventType, Consumer<E> consumer) {
    return new GenericApplicationListenerDelegate<>(eventType, consumer);
  }

}
