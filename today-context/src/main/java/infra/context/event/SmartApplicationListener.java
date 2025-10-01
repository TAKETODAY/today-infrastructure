/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.event;

import org.jspecify.annotations.Nullable;

import infra.context.ApplicationEvent;
import infra.context.ApplicationListener;
import infra.core.Ordered;

/**
 * Extended variant of the standard {@link ApplicationListener} interface,
 * exposing further metadata such as the supported event and source type.
 *
 * <p>For full introspection of generic event types, consider implementing
 * the {@link GenericApplicationListener} interface instead.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see GenericApplicationListener
 * @see GenericApplicationListenerAdapter
 * @since 4.0
 */
public interface SmartApplicationListener extends ApplicationListener<ApplicationEvent>, Ordered {

  /**
   * Determine whether this listener actually supports the given event type.
   *
   * @param eventType the event type (never {@code null})
   */
  boolean supportsEventType(Class<? extends ApplicationEvent> eventType);

  /**
   * Determine whether this listener actually supports the given source type.
   * <p>The default implementation always returns {@code true}.
   *
   * @param sourceType the source type, or {@code null} if no source
   */
  default boolean supportsSourceType(@Nullable Class<?> sourceType) {
    return true;
  }

  /**
   * Determine this listener's order in a set of listeners for the same event.
   * <p>The default implementation returns {@link #LOWEST_PRECEDENCE}.
   */
  @Override
  default int getOrder() {
    return LOWEST_PRECEDENCE;
  }

  /**
   * Return an optional identifier for the listener.
   * <p>The default value is an empty String.
   *
   * @see EventListener#id
   * @see ApplicationEventMulticaster#removeApplicationListeners
   */
  default String getListenerId() {
    return "";
  }

}
