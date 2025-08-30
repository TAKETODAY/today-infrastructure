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
import infra.context.ConfigurableApplicationContext;

/**
 * Event raised when an {@code ApplicationContext} gets restarted.
 *
 * <p>Note that {@code ContextRestartedEvent} is a specialization of
 * {@link ContextStartedEvent}.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see ConfigurableApplicationContext#restart()
 * @see ContextPausedEvent
 * @see ContextStartedEvent
 * @since 5.0
 */
@SuppressWarnings("serial")
public class ContextRestartedEvent extends ContextStartedEvent {

  /**
   * Create a new {@code ContextRestartedEvent}.
   *
   * @param source the {@code ApplicationContext} that has been restarted
   * (must not be {@code null})
   */
  public ContextRestartedEvent(ApplicationContext source) {
    super(source);
  }

}
