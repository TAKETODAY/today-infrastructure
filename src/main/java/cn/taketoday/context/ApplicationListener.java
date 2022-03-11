/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context;

import java.util.EventListener;
import java.util.function.Consumer;

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
 * @see cn.taketoday.context.event.ApplicationEventMulticaster
 * @see cn.taketoday.context.event.SmartApplicationListener
 * @see cn.taketoday.context.event.GenericApplicationListener
 * @see cn.taketoday.context.event.EventListener
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
