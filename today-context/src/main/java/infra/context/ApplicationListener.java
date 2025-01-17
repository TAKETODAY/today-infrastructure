/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

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
