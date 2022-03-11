/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.context.event;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Nullable;

/**
 * Extended variant of the standard {@link ApplicationListener} interface,
 * exposing further metadata such as the supported event and source type.
 *
 * <p>For full introspection of generic event types, consider implementing
 * the {@link GenericApplicationListener} interface instead.
 *
 * @author Juergen Hoeller
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
