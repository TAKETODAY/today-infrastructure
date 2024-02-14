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

package cn.taketoday.context.event;

import java.io.Serial;

import cn.taketoday.context.ApplicationContext;

/**
 * Event raised when an {@code ApplicationContext} gets initialized or refreshed.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ContextClosedEvent
 * @since 4.0 2021/11/12 17:08
 */
public class ContextRefreshedEvent extends ApplicationContextEvent {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Create a new ContextRefreshedEvent.
   *
   * @param source the {@code ApplicationContext} that has been initialized
   * or refreshed (must not be {@code null})
   */
  public ContextRefreshedEvent(ApplicationContext source) {
    super(source);
  }

}

