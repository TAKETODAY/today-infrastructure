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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.event;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationEvent;

/**
 * Base class for events raised for an {@code ApplicationContext}.
 *
 * @author TODAY 2018-09-09 23:05
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
