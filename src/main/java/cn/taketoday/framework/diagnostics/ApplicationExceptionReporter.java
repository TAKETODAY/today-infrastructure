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

package cn.taketoday.framework.diagnostics;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.framework.Application;

/**
 * Callback interface used to support custom reporting of {@link Application}
 * startup errors. {@link ApplicationExceptionReporter reporters} are loaded via the
 * {@link cn.taketoday.lang.TodayStrategies} and must declare a public constructor with a single
 * {@link ConfigurableApplicationContext} parameter.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ApplicationContextAware
 * @since 4.0 2022/2/19 22:36
 */
@FunctionalInterface
public interface ApplicationExceptionReporter {

  /**
   * Report a startup failure to the user.
   *
   * @param failure the source failure
   * @return {@code true} if the failure was reported or {@code false} if default
   * reporting should occur.
   */
  boolean reportException(Throwable failure);

}
