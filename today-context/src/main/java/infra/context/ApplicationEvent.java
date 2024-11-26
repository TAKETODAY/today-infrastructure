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

package infra.context;

import java.io.Serial;
import java.time.Clock;
import java.util.EventObject;

import infra.context.event.EventListener;

/**
 * Class to be extended by all application events. Abstract as it
 * doesn't make sense for generic events to be published directly.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ApplicationListener
 * @see EventListener
 * @since 2018-09-09 21:23
 */
public abstract class ApplicationEvent extends EventObject {

  @Serial
  private static final long serialVersionUID = 1L;

  /** System time when the event happened. */
  private final long timestamp;

  /**
   * Create a new {@code ApplicationEvent} with its {@link #getTimestamp() timestamp}
   * set to {@link System#currentTimeMillis()}.
   *
   * @param source the object on which the event initially occurred or with
   * which the event is associated (never {@code null})
   * @see #ApplicationEvent(Object, Clock)
   */
  public ApplicationEvent(Object source) {
    super(source);
    this.timestamp = System.currentTimeMillis();
  }

  /**
   * Create a new {@code ApplicationEvent} with its {@link #getTimestamp() timestamp}
   * set to the value returned by {@link Clock#millis()} in the provided {@link Clock}.
   * <p>This constructor is typically used in testing scenarios.
   *
   * @param source the object on which the event initially occurred or with
   * which the event is associated (never {@code null})
   * @param clock a clock which will provide the timestamp
   * @see #ApplicationEvent(Object)
   */
  public ApplicationEvent(Object source, Clock clock) {
    super(source);
    this.timestamp = clock.millis();
  }

  /**
   * Return the time in milliseconds when the event occurred.
   *
   * @see #ApplicationEvent(Object)
   * @see #ApplicationEvent(Object, Clock)
   */
  public final long getTimestamp() {
    return this.timestamp;
  }

}
