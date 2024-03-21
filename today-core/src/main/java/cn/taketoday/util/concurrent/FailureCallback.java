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

package cn.taketoday.util.concurrent;

/**
 * Failure callback for a {@link Future}.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface FailureCallback {

  /**
   * Called when the {@link Future} completes with failure.
   * <p>Note that Exceptions raised by this method are ignored.
   *
   * @param ex the failure
   */
  void onFailure(Throwable ex) throws Throwable;

  /**
   * on failure callback
   *
   * @param future target future
   * @param failureCallback failure callback
   * @param <V> value type
   * @throws NullPointerException failureCallback is null
   */
  static <V> void onFailure(Future<V> future, FailureCallback failureCallback) throws Throwable {
    Throwable cause = future.getCause();
    if (cause != null) {
      failureCallback.onFailure(cause);
    }
  }

}
