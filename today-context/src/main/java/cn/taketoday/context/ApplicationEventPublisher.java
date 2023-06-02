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

/**
 * Interface that encapsulates event publication functionality.
 *
 * <p>Serves as a super-interface for {@link ApplicationContext}.
 *
 * @author TODAY
 * @see ApplicationContext
 * @see ApplicationEventPublisherAware
 * @see ApplicationEvent
 * @see cn.taketoday.context.event.ApplicationEventMulticaster
 * @see cn.taketoday.context.event.EventPublicationInterceptor
 * @since 2018-09-09 21:26
 */
public interface ApplicationEventPublisher {

  /**
   * Notify all <strong>matching</strong> listeners registered with this
   * application of an event.
   * <p>Such an event publication step is effectively a hand-off to the
   * multicaster and does not imply synchronous/asynchronous execution
   * or even immediate execution at all. Event listeners are encouraged
   * to be as efficient as possible, individually using asynchronous
   * execution for longer-running and potentially blocking operations.
   *
   * @param event the event to publish
   * @see ApplicationEvent
   */
  void publishEvent(Object event);

}
