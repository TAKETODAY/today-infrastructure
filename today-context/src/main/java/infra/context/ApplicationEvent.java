/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
