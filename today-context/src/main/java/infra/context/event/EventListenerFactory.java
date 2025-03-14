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

package infra.context.event;

import java.lang.reflect.Method;

import infra.context.ApplicationListener;

/**
 * Strategy interface for creating {@link ApplicationListener} for methods
 * annotated with {@link EventListener}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface EventListenerFactory {

  /**
   * Specify if this factory supports the specified {@link Method}.
   *
   * @param method an {@link EventListener} annotated method
   * @return {@code true} if this factory supports the specified method
   */
  boolean supportsMethod(Method method);

  /**
   * Create an {@link ApplicationListener} for the specified method.
   *
   * @param beanName the name of the bean
   * @param type the target type of the instance
   * @param method the {@link EventListener} annotated method
   * @return an application listener, suitable to invoke the specified method
   */
  ApplicationListener<?> createApplicationListener(String beanName, Class<?> type, Method method);

}
