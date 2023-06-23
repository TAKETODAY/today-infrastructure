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

}
