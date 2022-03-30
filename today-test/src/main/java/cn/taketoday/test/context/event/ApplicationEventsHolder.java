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

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;

/**
 * Holder class to expose the application events published during the execution
 * of a test in the form of a thread-bound {@link ApplicationEvents} object.
 *
 * <p>{@code ApplicationEvents} are registered in this holder and managed by
 * the {@link ApplicationEventsTestExecutionListener}.
 *
 * <p>Although this class is {@code public}, it is only intended for use within
 * the <em>Spring TestContext Framework</em> or in the implementation of
 * third-party extensions. Test authors should therefore allow the current
 * instance of {@code ApplicationEvents} to be
 * {@link cn.taketoday.beans.factory.annotation.Autowired @Autowired}
 * into a field in the test class or injected via a parameter in test and
 * lifecycle methods when using JUnit Jupiter and the {@link
 * ApplicationExtension SpringExtension}.
 *
 * @author Sam Brannen
 * @author Oliver Drotbohm
 * @see ApplicationEvents
 * @see RecordApplicationEvents
 * @see ApplicationEventsTestExecutionListener
 * @since 4.0
 */
public abstract class ApplicationEventsHolder {

  private static final ThreadLocal<DefaultApplicationEvents> applicationEvents = new ThreadLocal<>();

  private ApplicationEventsHolder() {
    // no-op to prevent instantiation of this holder class
  }

  /**
   * Get the {@link ApplicationEvents} for the current thread.
   *
   * @return the current {@code ApplicationEvents}, or {@code null} if not registered
   */
  @Nullable
  public static ApplicationEvents getApplicationEvents() {
    return applicationEvents.get();
  }

  /**
   * Get the {@link ApplicationEvents} for the current thread.
   *
   * @return the current {@code ApplicationEvents}
   * @throws IllegalStateException if an instance of {@code ApplicationEvents}
   * has not been registered for the current thread
   */
  public static ApplicationEvents getRequiredApplicationEvents() {
    ApplicationEvents events = applicationEvents.get();
    Assert.state(events != null, "Failed to retrieve ApplicationEvents for the current thread. " +
            "Ensure that your test class is annotated with @RecordApplicationEvents " +
            "and that the ApplicationEventsTestExecutionListener is registered.");
    return events;
  }

  /**
   * Register a new {@link DefaultApplicationEvents} instance to be used for the
   * current thread, if necessary.
   * <p>If {@link #registerApplicationEvents()} has already been called for the
   * current thread, this method does not do anything.
   */
  static void registerApplicationEventsIfNecessary() {
    if (getApplicationEvents() == null) {
      registerApplicationEvents();
    }
  }

  /**
   * Register a new {@link DefaultApplicationEvents} instance to be used for the
   * current thread.
   */
  static void registerApplicationEvents() {
    applicationEvents.set(new DefaultApplicationEvents());
  }

  /**
   * Remove the registration of the {@link ApplicationEvents} for the current thread.
   */
  static void unregisterApplicationEvents() {
    applicationEvents.remove();
  }

}
