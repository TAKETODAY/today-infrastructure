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

package infra.util;

import org.jspecify.annotations.Nullable;

/**
 * A strategy for handling errors. This is especially useful for handling
 * errors that occur during asynchronous execution of tasks that have been
 * submitted to a TaskScheduler. In such cases, it may not be possible to
 * throw the error to the original caller.
 *
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/8/21 01:42
 */
@FunctionalInterface
public interface ErrorHandler {

  /**
   * Handle the given error, possibly rethrowing it as a fatal exception.
   */
  void handleError(Throwable t);

  /**
   * @return An {@link ErrorHandler} implementation that logs the Throwable at error
   * level.
   * @since 5.0
   */
  static ErrorHandler forLogging() {
    return forLogging(null);
  }

  /**
   * @return An {@link ErrorHandler} implementation that logs the Throwable at error
   * level.
   * @since 5.0
   */
  static ErrorHandler forLogging(@Nullable String message) {
    return forLogging(message, (String) null);
  }

  /**
   * @return An {@link ErrorHandler} implementation that logs the Throwable at error
   * level.
   * @since 5.0
   */
  static ErrorHandler forLogging(@Nullable String message, @Nullable String loggerName) {
    return new LoggingErrorHandler(message, loggerName);
  }

  /**
   * @return An {@link ErrorHandler} implementation that logs the Throwable at error
   * level.
   * @throws NullPointerException is loggerClass is {@code null}
   * @since 5.0
   */
  static ErrorHandler forLogging(@Nullable String message, Class<?> loggerClass) {
    return new LoggingErrorHandler(message, loggerClass.getName());
  }

  /**
   * @return An {@link ErrorHandler} implementation that logs the Throwable at error
   * level and then propagates it.
   * @since 5.0
   */
  static ErrorHandler forPropagating() {
    return forPropagating(null);
  }

  /**
   * @return An {@link ErrorHandler} implementation that logs the Throwable at error
   * level and then propagates it.
   * @since 5.0
   */
  static ErrorHandler forPropagating(@Nullable String message) {
    return forPropagating(message, (String) null);
  }

  /**
   * @return An {@link ErrorHandler} implementation that logs the Throwable at error
   * level and then propagates it.
   * @since 5.0
   */
  static ErrorHandler forPropagating(@Nullable String message, @Nullable String loggerName) {
    return new PropagatingErrorHandler(message, loggerName);
  }

  /**
   * @return An {@link ErrorHandler} implementation that logs the Throwable at error
   * level and then propagates it.
   * @throws NullPointerException is loggerClass is {@code null}
   * @since 5.0
   */
  static ErrorHandler forPropagating(@Nullable String message, Class<?> loggerClass) {
    return new PropagatingErrorHandler(message, loggerClass.getName());
  }

}
