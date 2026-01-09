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
