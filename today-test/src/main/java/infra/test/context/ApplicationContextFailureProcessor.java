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

package infra.test.context;

import infra.context.ApplicationContext;
import infra.lang.TodayStrategies;

/**
 * Strategy for components that process failures related to application contexts
 * within the <em>Infra TestContext Framework</em>.
 *
 * <p>Implementations must be registered via the
 * {@link TodayStrategies} mechanism.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ContextLoadException
 * @since 4.0 2023/6/15 21:13
 */
public interface ApplicationContextFailureProcessor {

  /**
   * Invoked when a failure was encountered while attempting to load an
   * {@link ApplicationContext}.
   * <p>Implementations of this method must not throw any exceptions. Consequently,
   * any exception thrown by an implementation of this method will be ignored, though
   * potentially logged.
   *
   * @param context the application context that did not load successfully
   * @param exception the exception thrown while loading the application context
   */
  void processLoadFailure(ApplicationContext context, Throwable exception);

}
