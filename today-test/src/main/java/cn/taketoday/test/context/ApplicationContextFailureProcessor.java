/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.context;

import cn.taketoday.context.ApplicationContext;

/**
 * Strategy for components that process failures related to application contexts
 * within the <em>Infra TestContext Framework</em>.
 *
 * <p>Implementations must be registered via the
 * {@link cn.taketoday.lang.TodayStrategies} mechanism.
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
