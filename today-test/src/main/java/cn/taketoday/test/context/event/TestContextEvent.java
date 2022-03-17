/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.context.event;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.test.context.TestContext;

/**
 * Base class for events published by the {@link EventPublishingTestExecutionListener}.
 *
 * @author Frank Scheffler
 * @author Sam Brannen
 * @since 5.2
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
   * {@linkplain cn.taketoday.context.event.EventListener#condition conditions}.
   *
   * @return the {@code TestContext} associated with this event (never {@code null})
   * @see #getSource()
   */
  public final TestContext getTestContext() {
    return getSource();
  }

}
