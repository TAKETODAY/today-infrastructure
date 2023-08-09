/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.util.function.Predicate;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationEventPublisher;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Nullable;

/**
 * Interface to be implemented by objects that can manage a number of
 * {@link ApplicationListener} objects and publish events to them.
 *
 * <p>An {@link ApplicationEventPublisher}, typically
 * a Framework {@link ApplicationContext}, can use an
 * {@code ApplicationEventMulticaster} as a delegate for actually publishing events.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ApplicationListener
 * @since 4.0 2021/12/8 15:18
 */
public interface ApplicationEventMulticaster {

  /**
   * Add a listener to be notified of all events.
   *
   * @param listener the listener to add
   * @see #removeApplicationListener(ApplicationListener)
   * @see #removeApplicationListeners(Predicate)
   */
  void addApplicationListener(ApplicationListener<?> listener);

  /**
   * Add a listener bean to be notified of all events.
   *
   * @param listenerBeanName the name of the listener bean to add
   * @see #removeApplicationListenerBean(String)
   * @see #removeApplicationListenerBeans(Predicate)
   */
  void addApplicationListenerBean(String listenerBeanName);

  /**
   * Remove a listener from the notification list.
   *
   * @param listener the listener to remove
   * @see #addApplicationListener(ApplicationListener)
   * @see #removeApplicationListeners(Predicate)
   */
  void removeApplicationListener(ApplicationListener<?> listener);

  /**
   * Remove a listener bean from the notification list.
   *
   * @param listenerBeanName the name of the listener bean to remove
   * @see #addApplicationListenerBean(String)
   * @see #removeApplicationListenerBeans(Predicate)
   */
  void removeApplicationListenerBean(String listenerBeanName);

  /**
   * Remove all matching listeners from the set of registered
   * {@code ApplicationListener} instances (which includes adapter classes
   * such as {@link ApplicationListenerMethodAdapter}, e.g. for annotated
   * {@link EventListener} methods).
   * <p>Note: This just applies to instance registrations, not to listeners
   * registered by bean name.
   *
   * @param predicate the predicate to identify listener instances to remove,
   * e.g. checking {@link SmartApplicationListener#getListenerId()}
   * @see #addApplicationListener(ApplicationListener)
   * @see #removeApplicationListener(ApplicationListener)
   * @since 5.3.5
   */
  void removeApplicationListeners(Predicate<ApplicationListener<?>> predicate);

  /**
   * Remove all matching listener beans from the set of registered
   * listener bean names (referring to bean classes which in turn
   * implement the {@link ApplicationListener} interface directly).
   * <p>Note: This just applies to bean name registrations, not to
   * programmatically registered {@code ApplicationListener} instances.
   *
   * @param predicate the predicate to identify listener bean names to remove
   * @see #addApplicationListenerBean(String)
   * @see #removeApplicationListenerBean(String)
   * @since 5.3.5
   */
  void removeApplicationListenerBeans(Predicate<String> predicate);

  /**
   * Remove all listeners registered with this multicaster.
   * <p>After a remove call, the multicaster will perform no action
   * on event notification until new listeners are registered.
   *
   * @see #removeApplicationListeners(Predicate)
   */
  void removeAllListeners();

  /**
   * Multicast the given application event to appropriate listeners.
   * <p>Consider using {@link #multicastEvent(ApplicationEvent, ResolvableType)}
   * if possible as it provides better support for generics-based events.
   * <p>If a matching {@code ApplicationListener} does not support asynchronous
   * execution, it must be run within the calling thread of this multicast call.
   *
   * @param event the event to multicast
   * @see ApplicationListener#supportsAsyncExecution()
   */
  void multicastEvent(ApplicationEvent event);

  /**
   * Multicast the given application event to appropriate listeners.
   * <p>If the {@code eventType} is {@code null}, a default type is built
   * based on the {@code event} instance.
   * <p>If a matching {@code ApplicationListener} does not support asynchronous
   * execution, it must be run within the calling thread of this multicast call.
   *
   * @param event the event to multicast
   * @param eventType the type of event (can be {@code null})
   * @see ApplicationListener#supportsAsyncExecution()
   */
  void multicastEvent(ApplicationEvent event, @Nullable ResolvableType eventType);

}
