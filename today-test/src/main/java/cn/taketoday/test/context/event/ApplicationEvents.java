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

import java.util.stream.Stream;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.test.context.TestExecutionListeners;

/**
 * {@code ApplicationEvents} encapsulates all {@linkplain ApplicationEvent
 * application events} that were fired during the execution of a single test method.
 *
 * <p>To use {@code ApplicationEvents} in your tests, do the following.
 * <ul>
 * <li>Ensure that your test class is annotated or meta-annotated with
 * {@link RecordApplicationEvents @RecordApplicationEvents}.</li>
 * <li>Ensure that the {@link ApplicationEventsTestExecutionListener} is
 * registered. Note, however, that it is registered by default and only needs
 * to be manually registered if you have custom configuration via
 * {@link TestExecutionListeners @TestExecutionListeners}
 * that does not include the default listeners.</li>
 * <li>Annotate a field of type {@code ApplicationEvents} with
 * {@link cn.taketoday.beans.factory.annotation.Autowired @Autowired} and
 * use that instance of {@code ApplicationEvents} in your test and lifecycle methods.</li>
 * <li>With JUnit Jupiter, you may optionally declare a parameter of type
 * {@code ApplicationEvents} in a test or lifecycle method as an alternative to
 * an {@code @Autowired} field in the test class.</li>
 * </ul>
 *
 * @author Sam Brannen
 * @author Oliver Drotbohm
 * @see RecordApplicationEvents
 * @see ApplicationEventsTestExecutionListener
 * @see cn.taketoday.context.ApplicationEvent
 * @since 4.0
 */
public interface ApplicationEvents {

  /**
   * Stream all application events that were fired during test execution.
   *
   * @return a stream of all application events
   * @see #stream(Class)
   * @see #clear()
   */
  Stream<ApplicationEvent> stream();

  /**
   * Stream all application events or event payloads of the given type that
   * were fired during test execution.
   *
   * @param <T> the event type
   * @param type the type of events or payloads to stream; never {@code null}
   * @return a stream of all application events or event payloads of the
   * specified type
   * @see #stream()
   * @see #clear()
   */
  <T> Stream<T> stream(Class<T> type);

  /**
   * Clear all application events recorded by this {@code ApplicationEvents} instance.
   * <p>Subsequent calls to {@link #stream()} or {@link #stream(Class)} will
   * only include events recorded since this method was invoked.
   *
   * @see #stream()
   * @see #stream(Class)
   */
  void clear();

}
