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

/**
 * @author TODAY 2018-09-09 21:26
 */
public interface ApplicationEventPublisher {

  /**
   * Publish event
   *
   * @param event
   *         Any Event object
   */
  void publishEvent(Object event);

  /**
   * Add an {@link ApplicationListener} that will be notified on context events
   * such as context refresh and context shutdown.
   * <p>
   *
   * @param listener
   *         the {@link ApplicationListener}
   *
   * @throws IllegalArgumentException
   *         if listener is null
   * @since 2.1.6
   */
  void addApplicationListener(ApplicationListener<?> listener);

  /**
   * @since 4.0
   */
  void addApplicationListener(String listenerBeanName);

  /**
   * Remove all listeners registered with this multicaster.
   * <p>After a remove call, the multicaster will perform no action
   * on event notification until new listeners are registered.
   */
  void removeAllListeners();

  void removeApplicationListener(String listenerBeanName);

  void removeApplicationListener(ApplicationListener<?> listener);

}
