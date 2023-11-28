/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.logging;

import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;

/**
 * Factory class used by {@link LoggingSystem#get(ClassLoader)} to find an actual
 * implementation.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface LoggingSystemFactory {

  /**
   * Return a logging system implementation or {@code null} if no logging system is
   * available.
   *
   * @param classLoader the class loader to use
   * @return a logging system
   */
  @Nullable
  LoggingSystem getLoggingSystem(ClassLoader classLoader);

  /**
   * Return a {@link LoggingSystemFactory} backed by {@code today.strategies}.
   *
   * @return a {@link LoggingSystemFactory} instance
   */
  static LoggingSystemFactory fromStrategies() {
    return new DelegatingLoggingSystemFactory(
            classLoader -> TodayStrategies.find(LoggingSystemFactory.class, classLoader));
  }

}
