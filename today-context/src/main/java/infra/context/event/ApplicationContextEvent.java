/*
 * Copyright 2017 - 2025 the original author or authors.
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

import infra.context.ApplicationContext;
import infra.context.ApplicationEvent;

/**
 * Base class for events raised for an {@link ApplicationContext}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2018-09-09 23:05
 */
@SuppressWarnings("serial")
public abstract class ApplicationContextEvent extends ApplicationEvent {

  /**
   * Create a new {@code ApplicationContextEvent}.
   *
   * @param source the {@link ApplicationContext} that the event is raised for
   * (must not be {@code null})
   */
  public ApplicationContextEvent(ApplicationContext source) {
    super(source);
  }

  /**
   * Get the {@link ApplicationContext} that the event was raised for.
   *
   * @return the {@code ApplicationContext} that the event was raised for
   * @see #getApplicationContext()
   * @since 5.0
   */
  @Override
  public ApplicationContext getSource() {
    return getApplicationContext();
  }

  /**
   * Get the {@link ApplicationContext} that the event was raised for.
   *
   * @see #getSource()
   */
  public final ApplicationContext getApplicationContext() {
    return (ApplicationContext) super.getSource();
  }

}
