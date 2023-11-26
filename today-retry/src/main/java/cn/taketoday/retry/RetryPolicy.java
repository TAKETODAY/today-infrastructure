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

package cn.taketoday.retry;

import java.io.Serializable;

/**
 * A {@link RetryPolicy} is responsible for allocating and managing resources needed by
 * {@link RetryOperations}. The {@link RetryPolicy} allows retry operations to be aware of
 * their context. Context can be internal to the retry framework, e.g. to support nested
 * retries. Context can also be external, and the {@link RetryPolicy} provides a uniform
 * API for a range of different platforms for the external context.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface RetryPolicy extends Serializable {

  /**
   * The value returned by {@link RetryPolicy#getMaxAttempts()} when the policy doesn't
   * provide a maximum number of attempts before failure
   */
  int NO_MAXIMUM_ATTEMPTS_SET = -1;

  /**
   * @param context the current retry status
   * @return true if the operation can proceed
   */
  boolean canRetry(RetryContext context);

  /**
   * Acquire resources needed for the retry operation. The callback is passed in so that
   * marker interfaces can be used and a manager can collaborate with the callback to
   * set up some state in the status token.
   *
   * @param parent the parent context if we are in a nested retry.
   * @return a {@link RetryContext} object specific to this policy.
   */
  RetryContext open(RetryContext parent);

  /**
   * @param context a retry status created by the {@link #open(RetryContext)} method of
   * this policy.
   */
  void close(RetryContext context);

  /**
   * Called once per retry attempt, after the callback fails.
   *
   * @param context the current status object.
   * @param throwable the exception to throw
   */
  void registerThrowable(RetryContext context, Throwable throwable);

  /**
   * Called to understand if the policy has a fixed number of maximum attempts before
   * failure
   *
   * @return -1 if the policy doesn't provide a fixed number of maximum attempts before
   * failure, the number of maximum attempts before failure otherwise
   */
  default int getMaxAttempts() {
    return NO_MAXIMUM_ATTEMPTS_SET;
  }
}
