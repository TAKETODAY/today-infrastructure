/*
 * Copyright 2017 - 2025 the original author or authors.
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
