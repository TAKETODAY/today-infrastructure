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

package cn.taketoday.context.event;

import java.util.function.Consumer;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.core.ResolvableType;

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
