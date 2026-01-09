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

package infra.context;

import java.util.EventListener;
import java.util.function.Consumer;

import infra.context.event.ApplicationEventMulticaster;
import infra.context.event.GenericApplicationListener;
import infra.context.event.SimpleApplicationEventMulticaster;
import infra.context.event.SmartApplicationListener;

/**
 * Interface to be implemented by application event listeners.
 *
 * <p>Based on the standard {@code java.util.EventListener} interface
 * for the Observer design pattern.
 *
 * <p>As of 4.0, an {@code ApplicationListener} can generically declare
 * the event type that it is interested in. When registered with a
 * {@code ApplicationContext}, events will be filtered accordingly, with the
 * listener getting invoked for matching event objects only.
 *
 * @param <E> the specific event to listen to
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2018-09-09 21:23
 * @see ApplicationEvent
 * @see ApplicationEventMulticaster
 * @see SmartApplicationListener
 * @see GenericApplicationListener
 * @see infra.context.event.EventListener
 */
@FunctionalInterface
public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {

  /**
   * Handle an application event.
   *
   * @param event the event to respond to
   */
  void onApplicationEvent(E event);

  /**
   * Return whether this listener supports asynchronous execution.
   *
   * @return {@code true} if this listener instance can be executed asynchronously
   * depending on the multicaster configuration (the default), or {@code false} if it
   * needs to immediately run within the original thread which published the event
   * @see SimpleApplicationEventMulticaster#setTaskExecutor
   * @since 4.0
   */
  default boolean supportsAsyncExecution() {
    return true;
  }

  /**
   * Create a new {@code ApplicationListener} for the given payload consumer.
   *
   * @param consumer the event payload consumer
   * @param <T> the type of the event payload
   * @return a corresponding {@code ApplicationListener} instance
   * @see PayloadApplicationEvent
   * @since 4.0
   */
  static <T> ApplicationListener<PayloadApplicationEvent<T>> forPayload(Consumer<T> consumer) {
    return event -> consumer.accept(event.getPayload());
  }

}
