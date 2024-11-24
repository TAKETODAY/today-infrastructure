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

import infra.beans.factory.BeanFactory;
import infra.context.ApplicationContext;
import infra.context.ApplicationEvent;

/**
 * Base class for events raised for an {@code ApplicationContext}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-09 23:05
 */
@SuppressWarnings("serial")
public abstract class ApplicationContextEvent extends ApplicationEvent {

  public ApplicationContextEvent(ApplicationContext source) {
    super(source);
  }

  /**
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  public final <T> T getSource(Class<T> requiredType) {
    if (requiredType.isInstance(super.getSource())) {
      throw new IllegalArgumentException("source must be a " + requiredType);
    }
    return (T) super.getSource();
  }

  /**
   * @since 4.0
   */
  public final BeanFactory getBeanFactory() {
    return getApplicationContext().getBeanFactory();
  }

  /**
   * @since 4.0
   */
  public final <T> T unwrapFactory(Class<T> requiredType) {
    return getApplicationContext().unwrapFactory(requiredType);
  }

  /**
   * Get the {@code ApplicationContext} that the event was raised for.
   */
  public final ApplicationContext getApplicationContext() {
    return (ApplicationContext) getSource();
  }

}
