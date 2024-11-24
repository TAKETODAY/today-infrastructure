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

package infra.test.context.event;

import infra.context.ApplicationEvent;
import infra.context.event.EventListener;
import infra.test.context.TestContext;

/**
 * Base class for events published by the {@link EventPublishingTestExecutionListener}.
 *
 * @author Frank Scheffler
 * @author Sam Brannen
 * @since 4.0
 */
@SuppressWarnings("serial")
public abstract class TestContextEvent extends ApplicationEvent {

  /**
   * Create a new {@code TestContextEvent}.
   *
   * @param source the {@code TestContext} associated with this event
   * (must not be {@code null})
   */
  public TestContextEvent(TestContext source) {
    super(source);
  }

  /**
   * Get the {@link TestContext} associated with this event.
   *
   * @return the {@code TestContext} associated with this event (never {@code null})
   * @see #getTestContext()
   */
  @Override
  public final TestContext getSource() {
    return (TestContext) super.getSource();
  }

  /**
   * Alias for {@link #getSource()}.
   * <p>This method may be favored over {@code getSource()} &mdash; for example,
   * to improve readability in SpEL expressions for event processing
   * {@linkplain EventListener#condition conditions}.
   *
   * @return the {@code TestContext} associated with this event (never {@code null})
   * @see #getSource()
   */
  public final TestContext getTestContext() {
    return getSource();
  }

}
