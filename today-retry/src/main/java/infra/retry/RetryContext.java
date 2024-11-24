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

package infra.retry;

import infra.core.AttributeAccessor;

/**
 * Low-level access to ongoing retry operation. Normally not needed by clients, but can be
 * used to alter the course of the retry, e.g. force an early termination.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface RetryContext extends AttributeAccessor {

  /**
   * Retry context attribute name for reporting key. Can be used for reporting purposes,
   * for instance in a retry listener, to accumulate data about the performance of a
   * retry.
   */
  String NAME = "context.name";

  /**
   * Retry context attribute name for state key. Can be used to identify a stateful
   * retry from its context.
   */
  String STATE_KEY = "context.state";

  /**
   * Retry context attribute that is non-null (and true) if the context has been closed.
   */
  String CLOSED = "context.closed";

  /**
   * Retry context attribute that is non-null (and true) if the recovery path was taken.
   */
  String RECOVERED = "context.recovered";

  /**
   * Retry context attribute that is non-null (and true) if the retry was exhausted.
   */
  String EXHAUSTED = "context.exhausted";

  /**
   * Retry context attribute that is non-null (and true) if the exception is not
   * recoverable.
   */
  String NO_RECOVERY = "context.no-recovery";

  /**
   * Retry context attribute that represent the maximum number of attempts for policies
   * that provide a maximum number of attempts before failure. For other policies the
   * value returned is {@link RetryPolicy#NO_MAXIMUM_ATTEMPTS_SET}
   */
  String MAX_ATTEMPTS = "context.max-attempts";

  /**
   * Signal to the framework that no more attempts should be made to try or retry the
   * current {@link RetryCallback}.
   */
  void setExhaustedOnly();

  /**
   * Public accessor for the exhausted flag {@link #setExhaustedOnly()}.
   *
   * @return true if the flag has been set.
   */
  boolean isExhaustedOnly();

  /**
   * Accessor for the parent context if retry blocks are nested.
   *
   * @return the parent or null if there is none.
   */
  RetryContext getParent();

  /**
   * Counts the number of retry attempts. Before the first attempt this counter is zero,
   * and before the first and subsequent attempts it should increment accordingly.
   *
   * @return the number of retries.
   */
  int getRetryCount();

  /**
   * Accessor for the exception object that caused the current retry.
   *
   * @return the last exception that caused a retry, or possibly null. It will be null
   * if this is the first attempt, but also if the enclosing policy decides not to
   * provide it (e.g. because of concerns about memory usage).
   */
  Throwable getLastThrowable();

}
