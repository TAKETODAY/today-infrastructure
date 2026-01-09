/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.logging;

import org.jspecify.annotations.Nullable;

import infra.lang.TodayStrategies;

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
